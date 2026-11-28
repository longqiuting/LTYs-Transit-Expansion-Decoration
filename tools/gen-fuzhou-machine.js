// 福州地铁售票机：把 Blockbench 素材转成 rawmodels/（运行时网格），并拷贴图。
//
// 用法: node tools/gen-fuzhou-machine.js
//   C:\Users\AD\Desktop\素材\福州地铁售票机\fuzhou_ticket_machine_{bottom,top,bottom_color,top_color}.json
//   → v1.20.1/src/main/resources/assets/mtrlte/rawmodels/fuzhou_machine_*.json
//
// 说明：
//   * 素材 UV 已经是 0..16 空间（texture_size 只是元数据），直接沿用；
//   * 贴图统一指向 textures/block/fuzhou_ticket_machine（关机态另有 _off）；
//   * top 里的 screen 分组单独拆成一个网格，LED 走字屏用。
const fs = require('fs');
const path = require('path');

const SRC = 'C:/Users/AD/Desktop/素材/福州地铁售票机';
const ROOT = path.resolve(__dirname, '..');
const RAW = path.join(ROOT, 'v1.20.1/src/main/resources/assets/mtrlte/rawmodels');
const TEX = path.join(ROOT, 'v1.20.1/src/main/resources/assets/mtrlte/textures/block');

const TEX_ON = 'textures/block/fuzhou_ticket_machine';
const TEX_OFF = 'textures/block/fuzhou_ticket_machine_off';

function convert(file, outName, texture) {
  const m = JSON.parse(fs.readFileSync(path.join(SRC, file), 'utf8'));
  const out = {
    textures: { 0: texture, particle: texture },
    texture_size: [16, 16],
    elements: m.elements.map((e) => {
      const faces = {};
      for (const [k, v] of Object.entries(e.faces || {})) {
        const f = { uv: v.uv, texture: '#0' };
        if (v.cullface) f.cullface = v.cullface;
        if (v.rotation) f.rotation = v.rotation;
        if (v.tintindex !== undefined) f.tintindex = v.tintindex;
        faces[k] = f;
      }
      const el = { from: e.from, to: e.to, faces };
      if (e.rotation) el.rotation = e.rotation;
      if (e.shade !== undefined) el.shade = e.shade;
      return el;
    }),
  };
  fs.writeFileSync(path.join(RAW, outName + '.json'), JSON.stringify(out, null, 2) + '\n');
  const faces = out.elements.reduce((n, e) => n + Object.keys(e.faces).length, 0);
  console.log(`${outName}: elements=${out.elements.length} faces=${faces}`);
}

// top：拆出 screen 分组
function convertTopWithScreen() {
  const m = JSON.parse(fs.readFileSync(path.join(SRC, 'fuzhou_ticket_machine_top.json'), 'utf8'));
  const screenIdx = new Set();
  const walk = (groups) => {
    for (const g of groups || []) {
      if (g.name === 'screen') {
        for (const c of g.children || []) if (typeof c === 'number') screenIdx.add(c);
      }
      walk(g.children);
    }
  };
  walk(m.groups);

  const body = [];
  const screen = [];
  m.elements.forEach((e, i) => (screenIdx.has(i) ? screen : body).push(e));

  const mk = (elements, outName) => {
    const out = {
      textures: { 0: TEX_ON, particle: TEX_ON },
      texture_size: [16, 16],
      elements: elements.map((e) => {
        const faces = {};
        for (const [k, v] of Object.entries(e.faces || {})) {
          faces[k] = { uv: v.uv, texture: '#0' };
        }
        const el = { from: e.from, to: e.to, faces };
        if (e.rotation) el.rotation = e.rotation;
        return el;
      }),
    };
    fs.writeFileSync(path.join(RAW, outName + '.json'), JSON.stringify(out, null, 2) + '\n');
    console.log(`${outName}: elements=${out.elements.length}`);
  };
  mk(body, 'fuzhou_machine_top');
  mk(screen, 'fuzhou_machine_screen');
}

convert('fuzhou_ticket_machine_bottom.json', 'fuzhou_machine_bottom', TEX_ON);
convertTopWithScreen();
convert('fuzhou_ticket_machine_bottom_color.json', 'fuzhou_machine_bottom_color', TEX_ON);
convert('fuzhou_ticket_machine_top_color.json', 'fuzhou_machine_top_color', TEX_ON);

// 贴图：原图直拷，不缩放
for (const [src, dst] of [
  ['fuzhou_ticket_machine.png', 'fuzhou_ticket_machine.png'],
  ['fuzhou_ticket_machine_off.png', 'fuzhou_ticket_machine_off.png'],
]) {
  fs.copyFileSync(path.join(SRC, src), path.join(TEX, dst));
  const b = fs.readFileSync(path.join(TEX, dst));
  console.log(`texture ${dst}: ${b.readUInt32BE(16)}x${b.readUInt32BE(20)} ${(b.length / 1024) | 0}KB`);
}
console.log('TEX_OFF 常量（关机态给渲染器用）:', TEX_OFF);
