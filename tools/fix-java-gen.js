#!/usr/bin/env node
/**
 * M4b-3 机械修复器（可重跑、幂等）。
 *
 * 只做「反编译/重映射产物的机械瑕疵」修复，不改业务逻辑、不改方法行为：
 *   1. 表格修复：丢 cast / 类型推断 / 重映射残留（MinecraftServer.D() → overworld()）/
 *      mojmap 命名撞车（box → ltedBox、BlockEntityMapper.createBlockEntity → newBlockEntity）/
 *      static isDamaged 与 IForgeItem 冲突 / decompiler 的 label-block 包 super(...) 构造器。
 *   2. 结构性修复：switch 的 case 体加块作用域（反编译把不同 case 的局部变量摊平到同一作用域）；
 *   3. 结构性修复：重复局部变量 —— 取 javac 报错行，找到最内层 `{}`，只在该块内改名 + 改引用。
 *
 * java-gen 是生成物（tools/remap-to-mojmap.sh 会覆盖），所以修复全部放在这里，
 * 每次重跑管线后重跑本脚本即可复原同样的修复结果。
 *
 * 用法：
 *   node tools/fix-java-gen.js             # 应用全部修复（幂等）
 *   node tools/fix-java-gen.js --dry-run   # 只报将要改什么
 */
'use strict';
const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const GEN = path.join(ROOT, 'forge/src/main/java-gen');
const LOG = path.join(ROOT, '.remap-work/m4b3-javac.log');
const DRY = process.argv.includes('--dry-run');

const changed = [];
function read(rel) {
  return fs.readFileSync(path.join(GEN, rel), 'utf8');
}
function write(rel, text, why) {
  if (DRY) { changed.push(`${rel}  (${why})`); return; }
  fs.writeFileSync(path.join(GEN, rel), text, 'utf8');
  changed.push(`${rel}  (${why})`);
}

/* ---------------- 1. 表格修复 ---------------- */
const TABLE = [
  // --- 反编译丢泛型 / 类型推断（补显式 cast 或类型参数，语义不变） ---
  { rel: 'dev/js/mtrheadlight/bind/MtrTiles.java', from: 'new LinkedHashSet((Set)value)', to: 'new LinkedHashSet<Long>((Set)value)', why: 'Set<Object> → Set<Long>：补类型参数' },
  { rel: 'dev/js/mtrheadlight/clock/ClockBlock.java', from: '? null : ClockBlockEntity::tick;', to: '? null : (BlockEntityTicker<T>)(BlockEntityTicker<ClockBlockEntity>)ClockBlockEntity::tick;', why: 'getTicker 三元式丢泛型 cast' },
  { rel: 'dev/js/mtrheadlight/xiamen/eyecandy/EyecandyBlock.java', from: '? EyecandyBlockEntity::tick : null;', to: '? (BlockEntityTicker<T>)(BlockEntityTicker<EyecandyBlockEntity>)EyecandyBlockEntity::tick : null;', why: 'getTicker 三元式丢泛型 cast' },
  { rel: 'dev/js/mtrheadlight/mixin/mtr/RouteAsyncScopeMixin.java', from: 'Object var4;', to: 'U var4;', why: 'lambda 返回值被擦成 Object（应为类型变量 U）' },
  // M4b-5：mixin 选择器从 yarn 名改成 Forge 生产环境实际的名字
  // （Forge 1.20.1 jar 里 MC 类名是 official，成员是 SRG；构造函数名不变，类名必须换）。
  { rel: 'dev/js/mtrheadlight/mixin/mtr/StationNameEntranceLogoMixin.java', re: /Lnet\/minecraft\/util\/Identifier;<init>\(Ljava\/lang\/String;Ljava\/lang\/String;\)V/g, to: 'Lnet/minecraft/resources/ResourceLocation;<init>(Ljava/lang/String;Ljava/lang/String;)V', why: 'yarn Identifier → mojmap ResourceLocation（两处 @At target）' },
  { rel: 'dev/js/mtrheadlight/mixin/mtr/StationNameEntranceLogoMixin.java', re: /remap = true/g, to: 'remap = false', why: 'Forge 侧选择器已是最终名，不需要 refmap 重映射' },
  { rel: 'dev/js/mtrheadlight/clock/ClockBlockEntity.java', from: '.orElse((Object)null)', to: '.orElse((SoundEvent)null)', why: 'Optional<SoundEvent>.orElse 参数被擦成 Object' },
  { rel: 'dev/js/mtrheadlight/security/LiquidScannerConfigScreen.java', from: 'Comparator.comparing((id) -> nameOf(id)', to: 'Comparator.comparing((ResourceLocation id) -> nameOf(id)', why: 'lambda 形参缺类型（擦除）' },
  { rel: 'dev/js/mtrheadlight/xiamen/eyecandy/EyecandyBlocks.java', from: 'for(int[] run : (List)rows.get(z0))', to: 'for(int[] run : (List<int[]>)rows.get(z0))', why: '裸 List 无法迭代成 int[]' },

  // --- 重映射残留：MinecraftServer#overworld() 没被改名（obf 名 D()） ---
  { rel: 'dev/js/mtrheadlight/clock/ClockGlobalState.java', from: 'server.D()', to: 'server.overworld()', why: '重映射残留 server.D() → overworld()' },
  { rel: 'dev/js/mtrheadlight/xiamen/pids/PidsAdSetState.java', from: 'server.D()', to: 'server.overworld()', why: '重映射残留 server.D() → overworld()' },

  // --- mojmap 命名撞车：本地 static box(...) 与 Block.box(...) 同签名构成隐藏 ---
  { rel: 'dev/js/mtrheadlight/xiamen/charger/ChargerBlock.java', re: /(?<!\.)\bbox\(/g, to: 'ltedBox(', why: 'box 与 Block.box 撞名' },
  { rel: 'dev/js/mtrheadlight/xiamen/drinks/DrinksMachineBlock.java', re: /(?<!\.)\bbox\(/g, to: 'ltedBox(', why: 'box 与 Block.box 撞名' },
  { rel: 'dev/js/mtrheadlight/xiamen/machine/XiamenMachineBlock.java', re: /(?<!\.)\bbox\(/g, to: 'ltedBox(', why: 'box 与 Block.box 撞名' },

  // --- static isDamaged(ItemStack) 与 IForgeItem 的实例默认方法冲突（静态不能实现接口方法） ---
  { rel: 'dev/js/mtrheadlight/xiamen/charger/PowerBankItem.java', re: /\bisDamaged\b/g, to: 'isDamagedTag', why: 'static isDamaged 与 IForgeItem.isDamaged 冲突，改名保语义' },
  { rel: 'dev/js/mtrheadlight/xiamen/charger/ChargerBlock.java', from: 'PowerBankItem.isDamaged(', to: 'PowerBankItem.isDamagedTag(', why: '跟随 isDamaged 改名' },

  // --- yarn 名残留：这些类的 createBlockEntity 要保留（MTR 的抽象方法），另补 newBlockEntity 桥 ---
];

/* decompiler 把 super(...) 塞进 label 块，导致「super 必须是构造器第一条语句」。
   改成先 super、再用静态辅助方法算 kind —— 与原逻辑等价（原有 label 块只是三元式的变形）。 */
const PIDS_BE_OLD = /   public PidsXiamenBlockEntity\(BlockPos pos, BlockState state\) \{\r?\n[\s\S]*?\r?\n      this\.kind = var10001;\r?\n   \}/;
const PIDS_BE_NEW = [
  '   public PidsXiamenBlockEntity(BlockPos pos, BlockState state) {',
  '      super(PidsXiamenObjects.entityType(), pos, state);',
  '      this.kind = kindOf(state);',
  '   }',
  '',
  '   private static PidsXiamenBlock.Kind kindOf(BlockState state) {',
  '      if (state != null) {',
  '         Block block = state.getBlock();',
  '         if (block instanceof PidsXiamenBlock pidsBlock) {',
  '            return pidsBlock.kind();',
  '         }',
  '      }',
  '',
  '      return PidsXiamenBlock.Kind.METRO;',
  '   }',
].join('\n');

function applyTable() {
  for (const e of TABLE) {
    let s = read(e.rel);
    if (e.from) {
      if (!s.includes(e.from)) continue;
      write(e.rel, s.split(e.from).join(e.to), e.why);
    } else if (e.re) {
      const before = s;
      s = s.replace(e.re, e.to);
      if (s !== before) write(e.rel, s, e.why);
    }
  }
  // 构造器 label 块
  const rel = 'dev/js/mtrheadlight/xiamen/pids/PidsXiamenBlockEntity.java';
  let s = read(rel);
  if (PIDS_BE_OLD.test(s)) {
    write(rel, s.replace(PIDS_BE_OLD, PIDS_BE_NEW), 'super(...) 被 label 块包住 → 改为首语句 + 静态 kindOf');
  }
  // MTR Forge 的 EntityBlockMapper 用 SRG 名（m_142194_）做 newBlockEntity 的默认实现，
  // 在 mojmap 编译期不算「实现了 EntityBlock.newBlockEntity」⇒ 每个实现类都要显式补一个桥方法。
  for (const r of BRIDGE_FILES) addNewBlockEntityBridge(r);
}

const BRIDGE_FILES = [
  'dev/js/mtrheadlight/xiamen/fangsu/FangsuPsdDoorBlock.java',
  'dev/js/mtrheadlight/xiamen/pids/PidsXiamenBlock.java',
  'dev/js/mtrheadlight/xiamen/pids/PsdLcdBlock.java',
  'dev/js/mtrheadlight/xiamen/sign/WallSignBlock.java',
  'dev/js/mtrheadlight/xiamen/XiamenPsdBlocks.java',
];

function addNewBlockEntityBridge(rel) {
  const s = read(rel);
  if (/\bnewBlockEntity\s*\(BlockPos/.test(s)) return; // 幂等
  const re = /(\r?\n([ \t]*)public BlockEntityMapper createBlockEntity\(BlockPos pos, BlockState state\) \{\r?\n[\s\S]*?\r?\n\2\})/g;
  if (!re.test(s)) return;
  write(
    rel,
    s.replace(re, (all, whole, indent) =>
      whole +
      `\n\n${indent}public BlockEntityMapper newBlockEntity(BlockPos pos, BlockState state) {\n` +
      `${indent}   return this.createBlockEntity(pos, state);\n` +
      `${indent}}`),
    '补 newBlockEntity 桥（MTR Forge 的默认实现是 SRG 名，mojmap 编译期不成立）'
  );
}

/* ---------------- 源码遮罩（字符串/注释 → 空白），用于精确的括号匹配 ---------------- */
function mask(src) {
  const out = src.split('');
  let i = 0, n = src.length, state = 0; // 0 normal 1 line 2 block 3 str 4 char
  while (i < n) {
    const c = src[i], d = src[i + 1];
    if (state === 0) {
      if (c === '/' && d === '/') { state = 1; out[i] = ' '; out[i + 1] = ' '; i += 2; continue; }
      if (c === '/' && d === '*') { state = 2; out[i] = ' '; out[i + 1] = ' '; i += 2; continue; }
      if (c === '"') { state = 3; out[i] = ' '; i++; continue; }
      if (c === "'") { state = 4; out[i] = ' '; i++; continue; }
      i++; continue;
    }
    if (state === 1) { if (c === '\n') { state = 0; } else out[i] = ' '; i++; continue; }
    if (state === 2) { if (c === '*' && d === '/') { out[i] = ' '; out[i + 1] = ' '; i += 2; state = 0; continue; } if (c !== '\n') out[i] = ' '; i++; continue; }
    if (state === 3) { if (c === '\\') { out[i] = ' '; out[i + 1] = ' '; i += 2; continue; } if (c === '"') { out[i] = ' '; state = 0; } else if (c !== '\n') out[i] = ' '; i++; continue; }
    if (state === 4) { if (c === '\\') { out[i] = ' '; out[i + 1] = ' '; i += 2; continue; } if (c === "'") { out[i] = ' '; state = 0; } else if (c !== '\n') out[i] = ' '; i++; continue; }
  }
  return out.join('');
}

const lineStarts = (s) => { const a = [0]; for (let i = 0; i < s.length; i++) if (s[i] === '\n') a.push(i + 1); return a; };

/* ---------------- 2. switch case 体加块作用域 ---------------- */
function wrapSwitchCase(src, m, switchOpen) {
  let depth = 0, switchClose = -1;
  for (let i = switchOpen; i < m.length; i++) {
    if (m[i] === '{') depth++;
    else if (m[i] === '}') { depth--; if (depth === 0) { switchClose = i; break; } }
  }
  if (switchClose < 0) return src;
  // 顶层 case/default 标签（按行首判定，避免把 case 里的字符串/代码误判）
  const labels = [];
  let d = 0;
  for (let i = switchOpen + 1; i < switchClose; i++) {
    const ch = m[i];
    if (ch === '{') d++;
    else if (ch === '}') d--;
    else if (d === 0) {
      const ls = src.lastIndexOf('\n', i) + 1;
      if (!/^[ \t]*$/.test(m.slice(ls, i))) continue;
      if (!/^(case\b|default\b)/.test(m.slice(i, i + 8))) continue;
      let j = i;
      while (j < switchClose && m[j] !== ':' && m[j] !== '{') j++;
      if (j >= switchClose || m[j] !== ':') continue;
      labels.push({ lineStart: ls, colon: j });
      i = j;
    }
  }
  if (labels.length === 0) return src;
  // 幂等：所有 case 体已经是 "{"
  if (labels.every(l => /^[ \t\r\n]*\{/.test(src.slice(l.colon + 1, l.colon + 40)))) return src;

  const eol = src.includes('\r\n') ? '\r\n' : '\n';
  const closeLineStart = (() => {
    const ls = src.lastIndexOf('\n', switchClose) + 1;
    return /^[ \t]*$/.test(src.slice(ls, switchClose)) ? ls : switchClose; // } 独占一行才用它
  })();
  let out = '', cursor = switchOpen + 1;
  for (let k = 0; k < labels.length; k++) {
    const end = k + 1 < labels.length ? labels[k + 1].lineStart : closeLineStart;
    const indent = (m.slice(labels[k].lineStart).match(/^[ \t]*/) || [''])[0];
    const body = src.slice(labels[k].colon + 1, end).replace(/\s+$/, '');
    out += src.slice(cursor, labels[k].colon + 1) + ' {' + eol + body + eol + indent + '}' + eol;
    cursor = end;
  }
  out += src.slice(cursor, switchClose);
  return src.slice(0, switchOpen + 1) + out + src.slice(switchClose);
}
function labelIndent(body, pos) {
  const ls = body.lastIndexOf('\n', pos) + 1;
  const s = body.slice(ls, pos);
  const m = s.match(/^ */);
  return m ? m[0].length : 3;
}

/* ---------------- 3. 重复局部变量：最内层块内改名 ---------------- */
function fixDupVar(rel, errLine) {
  const src = read(rel);
  const m = mask(src);
  const starts = lineStarts(src);
  const lineStart = starts[errLine - 1];
  const lineEnd = errLine < starts.length ? starts[errLine] : src.length;
  const text = m.slice(lineStart, lineEnd);
  const dm = text.match(/\b([A-Za-z_$][\w$]*)\s*=/);
  if (!dm) return null;
  const name = dm[1];
  if (name.endsWith('Lted')) return null; // 已改过，别叠加
  const eq = text.indexOf('=');
  const declPos = lineStart + text.lastIndexOf(name, eq);
  // 必须是「类型 名 =」形式，否则说明行号已过期（改了别处导致行偏移）——宁可不改
  const pre = text.slice(0, declPos - lineStart);
  if (!/^\s*(?:final\s+)?[A-Za-z_$][\w$.<>\[\], ?]*\s+$/.test(pre)) return null;

  // 最内层封闭 '{'
  let depth = 0, open = -1;
  for (let i = declPos; i >= 0; i--) {
    if (m[i] === '}') depth++;
    else if (m[i] === '{') { if (depth === 0) { open = i; break; } depth--; }
  }
  if (open < 0) return null;

  // switch 体？→ 交给 switch 处理
  const preText = m.slice(0, open);
  const lastSwitch = preText.lastIndexOf('switch');
  const lastBoundary = Math.max(preText.lastIndexOf('{'), preText.lastIndexOf(';'), preText.lastIndexOf('}'));
  if (lastSwitch > lastBoundary) {
    write(rel, wrapSwitchCase(src, m, open), `switch case 局部变量 ${name} 摊平 → case 体加块作用域`);
    return 'switch';
  }

  // 块结束
  depth = 0; let close = -1;
  for (let i = open; i < m.length; i++) {
    if (m[i] === '{') depth++;
    else if (m[i] === '}') { depth--; if (depth === 0) { close = i; break; } }
  }
  if (close < 0) return null;

  let newName = name + 'Lted';
  while (new RegExp(`\\b${newName}\\b`).test(m)) newName += '2';

  // 只在 [声明位置, 块结束) 内改；跳过 ".name"（成员访问）和 "name("（方法调用）
  const seg = src.slice(declPos, close);
  const re = new RegExp(`(?<![\\w$.])${name}(?![\\w$(])`, 'g');
  const nseg = seg.replace(re, newName);
  if (nseg === seg) return null;
  write(rel, src.slice(0, declPos) + nseg + src.slice(close), `重复局部变量 ${name} → ${newName}（限最内层块）`);
  return 'rename';
}

/* ---------------- 主流程 ---------------- */
applyTable();

if (fs.existsSync(LOG)) {
  const raw = fs.readFileSync(LOG, 'utf8').replace(/\r/g, '');
  const hits = [];
  const re = /^(.*?\.java):(\d+): 错误: 已在方法.*中定义了变量 (\w+)$/;
  for (const line of raw.split('\n')) {
    const mm = line.match(re);
    if (!mm) continue;
    const abs = mm[1].replace(/\\/g, '/');
    const idx = abs.indexOf('/forge/src/main/java-gen/');
    if (idx < 0) continue;
    hits.push({ rel: abs.slice(idx + '/forge/src/main/java-gen/'.length), line: +mm[2] });
  }
  // 同一文件按行号从大到小处理；每个文件每轮只改一处——行号始终来自最新一次 javac
  hits.sort((a, b) => (a.rel === b.rel ? b.line - a.line : a.rel.localeCompare(b.rel)));
  const done = new Set();
  for (const h of hits) {
    if (done.has(h.rel)) continue;
    if (fixDupVar(h.rel, h.line)) done.add(h.rel);
  }
}

if (changed.length === 0) {
  console.log('无需修改（已幂等）');
} else {
  console.log(`${DRY ? '[dry-run] 将修改' : '已修改'} ${changed.length} 处：`);
  for (const c of changed) console.log('  - ' + c);
}
