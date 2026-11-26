#!/usr/bin/env node
/**
 * 厦门地铁柱子：把 Blockbench 素材转成原版方块模型。
 *
 *   素材（默认 C:/Users/AD/Desktop/素材/厦门地铁柱子）
 *     amtr_pole.json          → assets/mtrlte/models/block/amtr_pole_middle.json
 *     amtr_pole_bottom.json   → assets/mtrlte/models/block/amtr_pole_bottom.json
 *     white_concrete.png      → assets/mtrlte/textures/block/white_concrete.png（原图，不缩放）
 *     gray_concrete.png       → assets/mtrlte/textures/block/gray_concrete.png（原图，不缩放）
 *
 * 只做四件事，几何一个数字都不改：
 *   1. textures 的两个变量改成 mtrlte:block/white_concrete（#0）/ mtrlte:block/gray_concrete（#1）；
 *   2. 去掉 Blockbench 专有的 credit / groups（原版模型不认，留着也只是噪音）；
 *   3. 柱底：elements[9..16]（Blockbench 里的 color 分组）的每个面加 "tintindex": 0
 *      —— 这就是"这一圈按方块颜色 provider 染色"的唯一开关；
 *   4. 顶上挂 "parent": "minecraft:block/block"（拿到标准 display 变换 + particle 兜底）。
 *
 * 幂等：重复跑结果一样。用法：
 *   node tools/gen-amtr-pole-models.js [素材目录]
 */
'use strict';

const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const SRC = process.argv[2] || 'C:/Users/AD/Desktop/素材/厦门地铁柱子';
const ASSETS = path.join(ROOT, 'v1.20.1/src/main/resources/assets/mtrlte');

/** 素材里的贴图变量 → 本模组的贴图 id。 */
const TEXTURES = {
  '0': 'mtrlte:block/white_concrete',
  '1': 'mtrlte:block/gray_concrete',
  particle: 'mtrlte:block/white_concrete',
};

/** 柱底里"可染色那圈"的 element 下标（= Blockbench 的 color 分组：9..16）。 */
const TINT_RANGE = { file: 'amtr_pole_bottom.json', from: 9, to: 16 };

function convert(file, outName) {
  const raw = JSON.parse(fs.readFileSync(path.join(SRC, file), 'utf8'));
  const model = {
    parent: 'minecraft:block/block',
    textures: { ...TEXTURES },
    elements: raw.elements,
  };

  if (file === TINT_RANGE.file && process.env.NOTINT !== '1') {
    for (let i = TINT_RANGE.from; i <= TINT_RANGE.to; i++) {
      const element = model.elements[i];
      if (!element || !element.faces) {
        throw new Error(`${file}: 缺少 element ${i}（素材结构变了？）`);
      }
      for (const face of Object.values(element.faces)) {
        face.tintindex = 0;
      }
    }
  }

  // 零厚度 element（素材里那 8 片 0 厚的贴面）在原版方块模型里会退化成零面积四边形，
  // 实测整个模型的方块渲染直接不出几何（物品能画、方块画不出来）。这里把 0 厚度轴撑开
  // 0.02 像素（1/800 格，肉眼无差别），几何体就正常了。数值改的是 to，from 保持不动。
  for (const element of model.elements) {
    for (let axis = 0; axis < 3; axis++) {
      const delta = element.to[axis] - element.from[axis];
      if (Math.abs(delta) < 1e-6) {
        element.to[axis] = element.from[axis] + 0.02;
      }
    }
  }

  const out = path.join(ASSETS, 'models/block', outName);
  fs.mkdirSync(path.dirname(out), { recursive: true });

  // 柱底：几何交给 BER（PoleRawRenderer）用 rawmodel 逐面画，方块模型留空，
  // 免得同一份几何被烘焙模型和方块实体各画一遍。
  if (file === TINT_RANGE.file && process.env.RAW === '1') {
    const groups = [
      { name: 'pole_core', tex: 'textures/block/gray_concrete', from: 0, to: 0 },
      { name: 'pole_fins', tex: 'textures/block/white_concrete', from: 1, to: 8 },
      { name: 'pole_band', tex: 'textures/block/white_concrete', from: 9, to: 16 },
    ];
    for (const group of groups) {
      const elements = model.elements.slice(group.from, group.to + 1).map((element) => {
        const copy = JSON.parse(JSON.stringify(element));
        for (const face of Object.values(copy.faces || {})) {
          delete face.tintindex;
        }
        return copy;
      });
      const raw = {
        textures: { 0: group.tex, particle: group.tex },
        texture_size: [16, 16],
        elements,
      };
      const outRaw = path.join(ASSETS, 'rawmodels', group.name + '.json');
      fs.mkdirSync(path.dirname(outRaw), { recursive: true });
      fs.writeFileSync(outRaw, format(raw), 'utf8');
      console.log(`rawmodel ${group.name}.json  elements=${elements.length}  ${group.tex}`);
    }
    model.elements = [];
  }

  fs.writeFileSync(out, format(model), 'utf8');

  // 方块模型留空之后，物品栏需要一个自己的完整模型（否则物品是空的）。
  const itemModel = {
    parent: 'minecraft:block/block',
    textures: { ...TEXTURES },
    elements: raw.elements,
  };
  const itemOut = path.join(ASSETS, 'models/item', outName);
  fs.mkdirSync(path.dirname(itemOut), { recursive: true });
  fs.writeFileSync(itemOut, format(itemModel), 'utf8');
  const tinted = model.elements.reduce((sum, element) =>
    sum + Object.values(element.faces || {}).filter(face => face.tintindex !== undefined).length, 0);
  console.log(`model  ${outName}  elements=${model.elements.length} tintedFaces=${tinted}`);
}

/**
 * 把 {@code JSON.stringify(x, null, '\t')} 那种"每个数字占一行"的数组压回一行，
 * 与素材 / 仓库里其它模型的排版一致（纯空白差异，不影响解析）。
 */
function format(value) {
  const json = JSON.stringify(value, null, '\t');
  // 先压数组，再压"只含基本值"的小对象（faces 里的 uv/texture/tintindex 那一层）。
  return json
    .replace(/\[\s+([^[\]{}]*?)\s+\]/g, (_, inner) => '[' + inner.split(/,\s+/).join(', ') + ']')
    .replace(/\{\s+([^{}]*?)\s+\}/g, (_, inner) => '{ ' + inner.split(/,\s+/).join(', ') + ' }')
    + '\n';
}

function copyTexture(name) {
  const from = path.join(SRC, name);
  if (!fs.existsSync(from)) {
    console.log(`texture ${name}  SKIPPED (源文件不在)`);
    return;
  }
  const to = path.join(ASSETS, 'textures/block', name);
  fs.mkdirSync(path.dirname(to), { recursive: true });
  fs.copyFileSync(from, to);
  const size = fs.statSync(to).size;
  console.log(`texture ${name}  ${size} bytes（原图直拷，未缩放）`);
}

convert('amtr_pole_bottom.json', 'amtr_pole_bottom.json');
convert('amtr_pole.json', 'amtr_pole_middle.json');
copyTexture('white_concrete.png');
copyTexture('gray_concrete.png');
