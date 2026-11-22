#!/usr/bin/env node
/*
 * fix-decompiled-source.js —— 修 fernflower 无类路径时留下的类型名渲染问题
 *
 * 反编译时 forgeflower 没有 classpath，遇到「泛型签名里的类型」会退化成二进制名：
 *   1) 包名首段被当成外部类:  net$minecraft.world.level.block.Block   -> net.minecraft.world.level.block.Block
 *   2) 内部类没被解析成点号:  StateDefinition$Builder                 -> StateDefinition.Builder
 *       （javac 不接受源码里的二进制名，java.util.Map$Entry 同样报找不到符号）
 *
 * 关键约束：字符串字面量里必须原样保留（如 "mtr.data.IPIDS$TileEntityPIDS"、
 * "price ${}"），所以按行切成「代码段 / 字面量段」分别处理。
 * lambda$xxx$0 / lte$routeMap 这类小写开头的方法名不受影响。
 *
 * 用法: node tools/fix-decompiled-source.js <目录或文件>...
 * 输出: 每个被改动文件的 "path<TAB>改写次数" 到 stdout
 */
const fs = require('fs');
const path = require('path');

const PKG_RE = /([a-z][a-z0-9_]*)\$([a-z][a-z0-9_]*\.)/g;   // net$minecraft. -> net.minecraft.
const NESTED_RE = /([A-Z][A-Za-z0-9_]*)\$([A-Z][A-Za-z0-9_]*)/g; // Foo$Bar -> Foo.Bar

function fixCode(code) {
  let n = 0, prev;
  do {
    prev = code;
    code = code.replace(PKG_RE, (m, a, b) => { n++; return a + '.' + b; });
  } while (code !== prev);
  do {
    prev = code;
    code = code.replace(NESTED_RE, (m, a, b) => { n++; return a + '.' + b; });
  } while (code !== prev);
  return { code, n };
}

// 把一行拆成 [{literal:bool, text}]，正确处理 \" 与 \\ 转义
function splitLiterals(line) {
  const out = [];
  let i = 0, buf = '', inLit = false;
  while (i < line.length) {
    const ch = line[i];
    if (!inLit) {
      if (ch === '"') { if (buf) out.push({ literal: false, text: buf }); buf = '"'; inLit = true; i++; continue; }
      if (ch === "'") { // 字符字面量，直接吃到配对引号
        if (buf) out.push({ literal: false, text: buf }); buf = "'"; i++;
        while (i < line.length) { buf += line[i]; if (line[i] === '\\') { buf += line[i + 1] || ''; i += 2; continue; } if (line[i] === "'") { i++; break; } i++; }
        out.push({ literal: true, text: buf }); buf = ''; continue;
      }
      buf += ch; i++;
    } else {
      buf += ch;
      if (ch === '\\') { buf += line[i + 1] || ''; i += 2; continue; }
      if (ch === '"') { inLit = false; out.push({ literal: true, text: buf }); buf = ''; }
      i++;
    }
  }
  if (buf) out.push({ literal: inLit, text: buf });
  return out;
}

function fixFile(file) {
  const src = fs.readFileSync(file, 'utf8');
  const lines = src.split('\n');
  let total = 0;
  const outLines = lines.map(line => {
    // 快速跳过：没有 '$' 的行不用拆
    if (line.indexOf('$') < 0) return line;
    const segs = splitLiterals(line);
    let changed = false;
    const fixed = segs.map(s => {
      if (s.literal) return s.text;
      const r = fixCode(s.text);
      if (r.n) { changed = true; total += r.n; }
      return r.code;
    }).join('');
    return changed ? fixed : line;
  });
  if (total) {
    fs.writeFileSync(file, outLines.join('\n'), 'utf8');
    console.log(`${file}\t${total}`);
  }
  return total;
}

function walk(p) {
  const st = fs.statSync(p);
  if (st.isDirectory()) {
    for (const e of fs.readdirSync(p)) walk(path.join(p, e));
  } else if (p.endsWith('.java')) {
    fixFile(p);
  }
}

const targets = process.argv.slice(2);
if (!targets.length) { console.error('usage: fix-decompiled-source.js <dir|file>...'); process.exit(2); }
for (const t of targets) walk(t);
