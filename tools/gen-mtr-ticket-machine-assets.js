// 港铁新售票机（ticket_machine_mtr）的静态资产：把用户给的 Blockbench 模型 + 两张状态贴图
// 落成原版方块模型 / blockstate / 物品模型 / 战利品表。
//
// 抄的是哪一份：xiamen 的售票机（ticket_machine_xiamen_*）——
//   · 一份「下段 / 上段」各一个模型（正面朝 +Z 导出，blockstate 按 facing 转 y）；
//   · 节能关机态另挂一份模型，几何一模一样、只换贴图（厦门那份是 _closed，这里叫 _off，
//     因为素材就叫 new_ticket_machine_off.png）；子模型用 parent 覆盖贴图，不重复写 elements；
//   · 物品图标是「整机」：把上段整体 +16 抬到第二格，和下段拼进同一个模型；
//   · 战利品表是空池（掉落由 XiamenMachineBlock#onBreak 发出，恒为 1 个）。
//
// UV：素材模型里的 uv 已经是「16 单位 = 整张贴图」的原版约定（最大值 11 / 15.5 对应贴图里
// 实际有内容的左上角区域，与 xiamen 那台 amtr_ticket 完全同一套），所以 uv 原样保留、
// 贴图也原样拷 4096² 的大图 —— 不缩、不重排。
//
// 用法：
//   node tools/gen-mtr-ticket-machine-assets.js
//   SRC=/path/to/素材/港铁新售票机 node tools/gen-mtr-ticket-machine-assets.js
const fs = require("fs");
const path = require("path");

const VER = process.argv[2] || "v1.20.1";
const ROOT = path.join(VER, "src/main/resources");
const SRC = process.env.SRC || "C:/Users/AD/Desktop/素材/港铁新售票机";

const ID = "ticket_machine_mtr";
const TEX_ON = `mtrlte:block/${ID}`;
const TEX_OFF = `mtrlte:block/${ID}_off`;

const read = (name) => JSON.parse(fs.readFileSync(path.join(SRC, name), "utf8").replace(/^\uFEFF/, ""));

const writeText = (rel, text) => {
  const p = path.join(ROOT, rel);
  fs.mkdirSync(path.dirname(p), { recursive: true });
  fs.writeFileSync(p, text);
  console.log("write", rel);
};

const copyBin = (srcFile, rel) => {
  const p = path.join(ROOT, rel);
  fs.mkdirSync(path.dirname(p), { recursive: true });
  fs.copyFileSync(path.join(SRC, srcFile), p);
  console.log("copy ", rel, `<${srcFile}>`, fs.statSync(p).size, "bytes");
};

const num = (n) => (Number.isInteger(n) ? String(n) : String(Math.round(n * 1e5) / 1e5));
const vec = (a) => "[" + a.map(num).join(", ") + "]";

/** 一个 Blockbench 元素 → 原版方块模型元素（0° 旋转直接丢掉，原版不需要）。 */
const element = (el, dy) => {
  const lines = [];
  lines.push("    {");
  lines.push(`      "from": ${vec([el.from[0], el.from[1] + dy, el.from[2]])},`);
  lines.push(`      "to": ${vec([el.to[0], el.to[1] + dy, el.to[2]])},`);
  const r = el.rotation;
  if (r && r.angle !== 0) {
    lines.push("      \"rotation\": {");
    lines.push(`        "origin": ${vec([r.origin[0], r.origin[1] + dy, r.origin[2]])},`);
    lines.push(`        "axis": "${r.axis}",`);
    lines.push(`        "angle": ${num(r.angle)},`);
    lines.push("        \"rescale\": false");
    lines.push("      },");
  }
  lines.push("      \"faces\": {");
  const faces = Object.entries(el.faces || {});
  faces.forEach(([name, face], i) => {
    const uv = face.uv ? `"uv": ${vec(face.uv)}, ` : "";
    const extra = face.cullface ? `, "cullface": "${face.cullface}"` : "";
    const tail = i === faces.length - 1 ? "" : ",";
    lines.push(`        "${name}": { ${uv}"texture": "#tex"${extra} }${tail}`);
  });
  lines.push("      }");
  lines.push("    }");
  return lines.join("\n");
};

/** 一整份模型（下段 / 上段 / 整机）。 */
const model = (comment, tex, elements, dy = 0) => `{
  "__comment": ${JSON.stringify(comment)},
  "parent": "block/block",
  "textures": { "particle": "${tex}", "tex": "${tex}" },
  "elements": [
${elements.map((el) => element(el, dy)).join(",\n")}
  ]
}
`;

// ---------------------------------------------------------------- 素材

const bottom = read("new_ticket_machine_bottom.json");
const top = read("new_ticket_machine_top.json");

// ---------------------------------------------------------------- 1) 贴图（原图直拷）

copyBin("new_ticket_machine.png", `assets/mtrlte/textures/block/${ID}.png`);
copyBin("new_ticket_machine_off.png", `assets/mtrlte/textures/block/${ID}_off.png`);

// ---------------------------------------------------------------- 2) 世界里用的四份模型

writeText(`assets/mtrlte/models/block/${ID}_bottom.json`, model(
  `港铁售票机「下段」：素材 new_ticket_machine_bottom.json 原样（${bottom.elements.length} 个元素），正面朝 +Z。`
  + "UV 与几何全部照素材，只把 Blockbench 的 texture 键 #0 换成原版的 #tex；"
  + "blockstate 按 facing 转 y（south=0 / west=90 / north=180 / east=270）。",
  TEX_ON, bottom.elements));

writeText(`assets/mtrlte/models/block/${ID}_top.json`, model(
  `港铁售票机「上段」：素材 new_ticket_machine_top.json 原样（${top.elements.length} 个元素），正面朝 +Z。`
  + "上段在游戏里是独立的一格，所以局部 y 就是 0..16（拼成物品图标时才 +16）。",
  TEX_ON, top.elements));

// 节能关机态：几何完全相同，只换贴图 —— 用 parent 覆盖 #tex，不重复写 elements。
for (const half of ["bottom", "top"]) {
  writeText(`assets/mtrlte/models/block/${ID}_${half}_off.json`, `{
  "__comment": ${JSON.stringify(
    `港铁售票机「${half === "bottom" ? "下段" : "上段"}」的节能关机态：几何照抄 ${ID}_${half}，只把贴图换成 `
    + `${TEX_OFF}（素材 new_ticket_machine_off.png）。厦门那台同类子模型叫 _closed，这里跟素材叫 _off。`)},
  "parent": "mtrlte:block/${ID}_${half}",
  "textures": {
    "particle": "${TEX_OFF}",
    "tex": "${TEX_OFF}"
  }
}
`);
}

// ---------------------------------------------------------------- 3) 物品图标（整机）

// 上段整体 +16（y），旋转的 origin 一起 +16 —— 与 xiamen 的 _item 模型同一套合并方式。
const itemElements = [
  ...bottom.elements.map((el) => element(el, 0)),
  ...top.elements.map((el) => element(el, 16)),
];
writeText(`assets/mtrlte/models/block/${ID}_item.json`, `{
  "__comment": ${JSON.stringify(
    "港铁售票机的物品图标：下段原样 + 上段整体 +16（含旋转 origin），拼成一台完整的机器。"
    + "世界里两段各用各的模型（block/" + ID + "_{bottom,top}），只有物品栏用这一份。")},
  "parent": "block/block",
  "textures": { "particle": "${TEX_ON}", "tex": "${TEX_ON}" },
  "elements": [
${itemElements.join(",\n")}
  ]
}
`);

// 与 xiamen 的 models/item/ticket_machine_xiamen.json 同一套 display（方块类物品的三视角）。
writeText(`assets/mtrlte/models/item/${ID}.json`, `{
  "parent": "mtrlte:block/${ID}_item",
  "display": {
    "gui": { "rotation": [30, 225, 0], "translation": [0, -2.6, 0], "scale": [0.33, 0.33, 0.33] },
    "ground": { "rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.125, 0.125, 0.125] },
    "fixed": { "rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.25, 0.25, 0.25] },
    "thirdperson_righthand": { "rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.1875, 0.1875, 0.1875] },
    "firstperson_righthand": { "rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.2, 0.2, 0.2] },
    "firstperson_lefthand": { "rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.2, 0.2, 0.2] }
  }
}
`);

// ---------------------------------------------------------------- 4) blockstate（4 朝向 × 2 段 × 3 个 ui）

// facing → blockstate 的 y，与 XiamenMachineBlock.blockRotation 严格一致。
const FACINGS = [
  ["north", 180],
  ["east", 270],
  ["south", 0],
  ["west", 90],
];
const UIS = ["fangsu", "vanilla", "eco"];

const rows = [];
for (const [facing, y] of FACINGS) {
  for (const half of ["lower", "upper"]) {
    for (const ui of UIS) {
      const off = ui === "eco" ? "_off" : "";
      const ref = `mtrlte:block/${ID}_${half === "lower" ? "bottom" : "top"}${off}`;
      const apply = y === 0 ? `{ "model": "${ref}" }` : `{ "model": "${ref}", "y": ${y} }`;
      rows.push(`    "facing=${facing},half=${half},ui=${ui}": ${apply}`);
    }
  }
}
writeText(`assets/mtrlte/blockstates/${ID}.json`, `{
  "__comment": ${JSON.stringify(
    "港铁售票机：4 朝向 × 2 段 × 3 个 ui（fangsu / vanilla 用开机模型，eco = 节能关机用 _off 模型），"
    + "与 blockstates/ticket_machine_xiamen.json 同一套排法。y 的取值与 XiamenMachineBlock.blockRotation 一致。")},
  "variants": {
${rows.join(",\n")}
  }
}
`);

// ---------------------------------------------------------------- 5) 战利品表（空池）
//
// 空池：上下两段只在 XiamenMachineBlock#onBreak 里掉 1 个（拆哪一半都只掉一个、而且一定掉在
// 下段那一格）。与 ticket_machine_xiamen 同一套做法 —— 所以文件里只有 type + 空的 pools，
// 不写注释字段（战利品表走 codec 解析，仓库里其它战利品表也都不带 __comment）。

writeText(`data/mtrlte/loot_tables/blocks/${ID}.json`, `{
  "type": "minecraft:block",
  "pools": []
}
`);
