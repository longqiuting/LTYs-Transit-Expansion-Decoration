#!/usr/bin/env node
/**
 * M4b-3 错误分类器：读 .remap-work/m4b3-javac.log（tools/m4b3-javac.sh 产出），
 * 按「文件 → 主因类别」聚合，输出 markdown 表 + JSON。
 *
 * 分类优先级（一个文件只归一类，取最根因）：
 *   1) 可机械修但还没修的：DUP 重复局部变量 / OVERRIDE 覆盖可见性 / TYPE 泛型推断
 *   2) MTR 侧签名差异（mtr.mappings / EntityBlockMapper / SRG 名）
 *   3) Fabric 平台层，按涉及的 API 面细分：
 *      NET 网络 / REG 注册 / RENDER 渲染 / CMD 客户端命令 / ENTRY 入口与 loader
 *   4) OTHER 兜底
 *
 * 用法：node tools/m4b3-classify.js [--md <out.md>]
 */
'use strict';
const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const LOG = path.join(ROOT, '.remap-work/m4b3-javac.log');
const GEN = path.join(ROOT, 'forge/src/main/java-gen');

const argv = process.argv.slice(2);
const mdIdx = argv.indexOf('--md');
const MD_OUT = mdIdx >= 0 ? argv[mdIdx + 1] : null;

function walk(dir) {
  const out = [];
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) out.push(...walk(p));
    else out.push(p);
  }
  return out;
}

const raw = fs.readFileSync(LOG, 'utf8').replace(/\r/g, '');
const files = new Map();
const reErr = /^(.*?\.java):(\d+): 错误: (.*)$/;
for (const line of raw.split('\n')) {
  const m = line.match(reErr);
  if (!m) continue;
  const f = path.normalize(m[1]).replace(/\\/g, '/');
  if (!files.has(f)) files.set(f, []);
  files.get(f).push({ line: +m[2], msg: m[3].trim() });
}

const all = walk(GEN).filter(f => f.endsWith('.java')).map(f => f.replace(/\\/g, '/'));

const FACETS = [
  { cat: 'NET', name: 'Fabric 网络 API', re: /fabric\.api\.(client\.)?networking\.v1\.|PacketByteBuf|PlayerLookup/,
    root: 'ServerPlayNetworking/ClientPlayNetworking/PacketByteBufs 在 Forge 无对应类；Forge 侧是 SimpleChannel(FriendlyByteBuf)，读写体可照搬但注册与分发是另一套 API' },
  { cat: 'REG', name: 'Fabric 注册 API', re: /ItemGroupEvents|FabricItemGroup|FabricBlockEntityTypeBuilder|FabricEntityTypeBuilder/,
    root: 'FabricBlockEntityTypeBuilder / ItemGroupEvents 是 Fabric API 独有；Forge 侧要用 BlockEntityType.Builder.of + DeferredRegister，创造模式标签改 BuildCreativeModeTabContentsEvent' },
  { cat: 'CMD', name: 'Fabric 客户端命令 v2', re: /client\.command\.v2|FabricClientCommandSource|ClientCommandManager|ClientCommandRegistrationCallback/,
    root: 'FabricClientCommandSource 是 Fabric 独有的命令源；Forge 侧用 RegisterClientCommandsEvent + CommandSourceStack（参数类型 / 反馈方法都不同）' },
  { cat: 'RENDER', name: 'Fabric 渲染注册', re: /BlockEntityRendererRegistry|EntityRendererRegistry|BlockRenderLayerMap|WorldRenderEvents/,
    root: 'BlockEntityRendererRegistry / BlockRenderLayerMap 在 Forge 是 EntityRenderersEvent.RegisterRenderers 与 ItemBlockRenderTypes.setRenderLayer（client setup 阶段）' },
  { cat: 'ENTRY', name: 'Fabric 入口 / loader', re: /net\.fabricmc\.api\b|net\.fabricmc\.loader|ModMenuApi|ConfigScreenFactory|ResourceManagerHelper|SimpleResourceReloadListener/,
    root: 'ClientModInitializer/ModInitializer/FabricLoader/ModMenu 是 Fabric 生命周期；Forge 侧是 @Mod 构造器 + FMLClientSetupEvent，ModMenu 无等价物（配置改 ModConfigSpec 或自带 GUI）' },
];

const MTR_RE = /mtr\/mappings|mtr\.mappings|EntityBlockMapper|先于|SRG/;

function classify(file, errs) {
  const src = fs.readFileSync(file, 'utf8');
  const msgs = errs.map(e => e.msg).join('\n');

  // Fabric 平台层优先：它缺类会级联出「类型不兼容 / 找不到符号」，先归平台层才不误判
  if (/net\.fabricmc|com\.terraformersmc/.test(src)) {
    for (const f of FACETS) if (f.re.test(src)) return f.cat;
    return 'FABRIC_OTHER';
  }
  if (MTR_RE.test(src) && /mtr\./.test(src)) return 'MTR';
  if (/已在方法.*中定义了变量/.test(msgs)) return 'DUP';
  if (/无法覆盖|无法实现|不是抽象的/.test(msgs)) return 'OVERRIDE';
  if (/不兼容的类型|无法转换为|类型错误|推论变量|方法引用/.test(msgs)) return 'TYPE';
  return 'OTHER';
}

const names = {
  NET: 'Fabric 网络 API', REG: 'Fabric 注册 API', CMD: 'Fabric 客户端命令 v2',
  RENDER: 'Fabric 渲染注册', ENTRY: 'Fabric 入口 / loader', FABRIC_OTHER: '其他 Fabric API',
  MTR: 'MTR 侧签名差异', DUP: '重复局部变量', TYPE: '泛型/类型推断', OVERRIDE: '覆盖/可见性', OTHER: '其他',
};
const rootCauses = {
  NET: 'Forge 没有 net.fabricmc.fabric.api.networking.v1（ServerPlayNetworking / ClientPlayNetworking / PacketByteBufs / PlayerLookup）；要换 SimpleChannel + PacketDistributor，包体读写（FriendlyByteBuf）可照搬。',
  REG: 'FabricBlockEntityTypeBuilder / ItemGroupEvents 是 Fabric 独有 API；Forge 侧是 BlockEntityType.Builder.of + DeferredRegister 与 BuildCreativeModeTabContentsEvent。',
  CMD: 'FabricClientCommandSource 是 Fabric 独有命令源；Forge 侧 RegisterClientCommandsEvent + CommandSourceStack，参数类型与反馈调用都不同。',
  RENDER: 'BlockEntityRendererRegistry / BlockRenderLayerMap 在 Forge 换成 EntityRenderersEvent.RegisterRenderers 与 ItemBlockRenderTypes.setRenderLayer。',
  ENTRY: 'ClientModInitializer / ModInitializer / FabricLoader / ModMenuApi 属 Fabric 生命周期；Forge 侧 @Mod 构造器 + FMLClientSetupEvent，ModMenu 无等价物。',
  MTR: 'MTR Forge 版的方法名是 SRG（m_142194_ 等），与 mojmap 编译期不匹配；部分签名（TileEntityPIDS 构造器、BlockEntityRendererMapper）两侧不一致。',
  DUP: 'fernflower 把不同分支的局部变量摊平到同一作用域。',
  TYPE: '擦除导致三元式 / 方法引用 / 泛型参数丢失。',
  OVERRIDE: 'yarn 名与 mojmap 同名不同义（box、isDamaged、createBlockEntity）。',
  OTHER: '其他。',
  FABRIC_OTHER: '其他 Fabric API 类型。',
};

const rows = [];
for (const f of all) {
  const key = fs.existsSync(f) ? f : null;
  if (!files.has(f)) { rows.push({ file: f, ok: true }); continue; }
  const errs = files.get(f);
  const cat = classify(f, errs);
  rows.push({
    file: path.relative(ROOT, f).replace(/\\/g, '/'),
    ok: false, cat, nErr: errs.length,
    first: errs[0].msg, firstLine: errs[0].line,
    samples: [...new Set(errs.map(e => e.msg))].slice(0, 3),
  });
}

const ok = rows.filter(r => r.ok).length;
const bad = rows.filter(r => !r.ok);
const byCat = new Map();
for (const r of bad) { if (!byCat.has(r.cat)) byCat.set(r.cat, []); byCat.get(r.cat).push(r); }
const order = ['NET', 'REG', 'CMD', 'RENDER', 'ENTRY', 'FABRIC_OTHER', 'MTR', 'DUP', 'TYPE', 'OVERRIDE', 'OTHER'];
const totalErr = [...files.values()].reduce((a, b) => a + b.length, 0);

const L = [];
L.push(`## 全量 javac（200 个 java-gen + 5 个手写 forge 类）：通过 ${ok}，失败 ${bad.length}，错误 ${totalErr}`);
L.push('');
L.push('| 类别 | 文件数 | 错误数 | 代表文件 | 根因 |');
L.push('|---|---|---|---|---|');
for (const cat of order) {
  const list = byCat.get(cat);
  if (!list) continue;
  const rep = list.slice().sort((a, b) => b.nErr - a.nErr)[0];
  L.push(`| ${cat} ${names[cat]} | ${list.length} | ${list.reduce((a, r) => a + r.nErr, 0)} | ${path.basename(rep.file)} | ${rootCauses[cat]} |`);
}
L.push('');
for (const cat of order) {
  const list = byCat.get(cat);
  if (!list) continue;
  L.push(`### ${cat} ${names[cat]} (${list.length})`);
  for (const r of list) L.push(`- \`${r.file}\` — ${r.nErr} err — L${r.firstLine}: ${r.first}`);
  L.push('');
}

const out = L.join('\n');
console.log(out);
if (MD_OUT) {
  fs.writeFileSync(path.resolve(ROOT, MD_OUT), out + '\n', 'utf8');
  console.log(`\nmarkdown → ${MD_OUT}`);
}
fs.writeFileSync(path.join(ROOT, '.remap-work/m4b3-classify.json'),
  JSON.stringify({ total: all.length, ok, bad: bad.length, totalErr, rows }, null, 1));
