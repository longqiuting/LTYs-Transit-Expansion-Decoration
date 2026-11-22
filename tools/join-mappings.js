#!/usr/bin/env node
/*
 * join-mappings.js —— 把 intermediary(tiny v2) 与 mojmap(ProGuard tv2) 合成一份
 * 三命名空间 tiny v2:  official / intermediary / named
 *
 * 用法:
 *   node tools/join-mappings.js <intermediary mappings.tiny> <client_mappings.txt> <out.tiny>
 *
 * 为什么是三个命名空间:  intermediary 文件里的 descriptor 是 obf(official) 描述符。
 * mojmap 的 ProGuard 文件写的是 *named* 类型，所以成员描述的匹配必须先把 named 类型
 * 反查回 obf（proguard 的类行本身给了 named -> obf 的全量映射）。
 * 匹配成功后把 named 名字追加成第三列，descriptor 一列不用改，tiny-remapper 支持 N 命名空间。
 */
const fs = require('fs');

const [, , inTiny, inProguard, outTiny] = process.argv;
if (!inTiny || !inProguard || !outTiny) {
  console.error('usage: join-mappings.js <mappings.tiny> <client_mappings.txt> <out.tiny>');
  process.exit(2);
}

const PRIM = { void: 'V', int: 'I', boolean: 'Z', byte: 'B', char: 'C', short: 'S', long: 'J', float: 'F', double: 'D' };

function javaTypeToDesc(javaType, namedToObf) {
  let dims = 0;
  let t = javaType.trim();
  while (t.endsWith('[]')) { dims++; t = t.slice(0, -2); }
  // 去掉泛型（proguard 偶尔带 <T> / <K,V>）
  let depth = 0, out = '';
  for (const ch of t) {
    if (ch === '<') depth++;
    else if (ch === '>') depth--;
    else if (depth === 0) out += ch;
  }
  t = out.trim();
  let base;
  if (PRIM[t]) base = PRIM[t];
  else {
    const obf = namedToObf.get(t) || t;
    base = 'L' + obf.replace(/\./g, '/') + ';';
  }
  return '['.repeat(dims) + base;
}

const pgLines = fs.readFileSync(inProguard, 'utf8').split(/\r?\n/);

// ---- pass 1: 类行  named -> obf / obf -> named ----
const namedToObf = new Map();
const clsNamedByObf = new Map();
const classLineRe = /^(.*?) -> (\S+):$/;
let pgClasses = 0;
for (const line of pgLines) {
  if (!line || line.startsWith(' ') || line.startsWith('#')) continue;
  const m = classLineRe.exec(line);
  if (!m) continue;
  const named = m[1].trim(), obf = m[2].trim();
  // tiny 的类名必须是内部名（斜杠），ProGuard 给的是点分名；'$' 保持不动
  const namedInternal = named.replace(/\./g, '/');
  namedToObf.set(named, obf);
  namedToObf.set(namedInternal, obf);
  clsNamedByObf.set(obf, namedInternal);
  pgClasses++;
}

// ---- pass 2: 成员行 ----
const memberNamed = new Map(); // `${obfClass}\t${obfName}\t${desc}` -> namedName
let curObf = null, pgMembers = 0, dupKeys = 0, badLines = 0;
const memberRe = /^\s+(?:\d+:\d+:)?(.+?) -> (\S+)$/;

for (const line of pgLines) {
  if (!line || line.startsWith('#')) continue;
  if (!line.startsWith(' ')) {
    const m = classLineRe.exec(line);
    curObf = m ? m[2].trim() : null;
    continue;
  }
  if (!curObf) continue;
  const m = memberRe.exec(line);
  if (!m) { badLines++; continue; }
  const left = m[1].trim();
  const obfName = m[2];
  let desc, namedName;
  const pIdx = left.indexOf('(');
  if (pIdx >= 0) {
    const retAndName = left.slice(0, pIdx).trim();
    const args = left.slice(pIdx + 1, left.lastIndexOf(')'));
    const sp = retAndName.lastIndexOf(' ');
    const ret = sp < 0 ? retAndName : retAndName.slice(0, sp);
    namedName = sp < 0 ? retAndName : retAndName.slice(sp + 1);
    const argDescs = args.trim() ? args.split(',').map(a => javaTypeToDesc(a, namedToObf)) : [];
    desc = '(' + argDescs.join('') + ')' + javaTypeToDesc(ret, namedToObf);
  } else {
    const sp = left.lastIndexOf(' ');
    namedName = sp < 0 ? left : left.slice(sp + 1);
    desc = javaTypeToDesc(sp < 0 ? 'java.lang.Object' : left.slice(0, sp), namedToObf);
  }
  const key = `${curObf}\t${obfName}\t${desc}`;
  if (memberNamed.has(key)) { dupKeys++; continue; }
  memberNamed.set(key, namedName);
  pgMembers++;
}

// ---- 读 intermediary tiny v2，追加 named 列 ----
const tinyLines = fs.readFileSync(inTiny, 'utf8').split(/\r?\n/);
const out = [];
let curObfClass = null;
let nClass = 0, nClassHit = 0, nMethod = 0, nMethodHit = 0, nField = 0, nFieldHit = 0;
const missClass = [], missMember = [];

for (const raw of tinyLines) {
  if (raw === '') { out.push(raw); continue; }
  if (raw.startsWith('tiny\t')) {
    const parts = raw.split('\t');
    if (parts.length !== 5) throw new Error('unexpected tiny header: ' + raw);
    out.push(raw + '\tnamed');
    continue;
  }
  if (raw.startsWith('c\t')) {
    const parts = raw.split('\t');
    const obf = parts[1];
    curObfClass = obf;
    nClass++;
    const named = clsNamedByObf.get(obf);
    if (named) nClassHit++; else missClass.push(obf);
    out.push(raw + '\t' + (named || obf));
    continue;
  }
  if (raw.startsWith('\t')) {
    const parts = raw.split('\t');
    const kind = parts[1];
    if (kind === 'm' || kind === 'f') {
      const desc = parts[2], obfName = parts[3];
      const named = memberNamed.get(`${curObfClass}\t${obfName}\t${desc}`);
      if (kind === 'm') { nMethod++; if (named) nMethodHit++; else missMember.push('m ' + curObfClass + '.' + obfName + desc); }
      else { nField++; if (named) nFieldHit++; else missMember.push('f ' + curObfClass + '.' + obfName + ' ' + desc); }
      out.push(raw + '\t' + (named || obfName));
      continue;
    }
    out.push(raw);
    continue;
  }
  out.push(raw);
}

fs.writeFileSync(outTiny, out.join('\n'), 'utf8');

const stats = {
  proguard: { classes: pgClasses, members: pgMembers, duplicateMemberKeys: dupKeys, unparsedMemberLines: badLines },
  intermediary: {
    classes: nClass, classesMatched: nClassHit,
    methods: nMethod, methodsMatched: nMethodHit,
    fields: nField, fieldsMatched: nFieldHit,
  },
  unmatchedClassSamples: missClass.slice(0, 20),
  unmatchedMemberSamples: missMember.slice(0, 20),
};
console.log(JSON.stringify(stats, null, 2));
fs.writeFileSync(outTiny + '.stats.json', JSON.stringify(stats, null, 2));
