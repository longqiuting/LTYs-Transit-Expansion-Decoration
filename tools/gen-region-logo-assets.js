// 生成三个地区标志块（fuzhou_logo / shenzhen_logo / mtr_hk_logo）的静态资产，并拷入页签图标贴图。
//
// 抄的是哪一份：logo.LogoObjects（LTE / 角美两个标志块）。模型结构与
// assets/mtrlte/models/block/jiaomei_logo.json 逐字节同构 —— 满方块金属底板（六面 cullface）
// + 外扩 0.05 的四侧 logo 覆盖层（shade:false）。只有贴图名不同。
//
// 贴图：「原图直拷」—— 用户给的三张图不缩、不重绘，一个字节不改地落成两份：
//   · textures/block/<id>_logo.png   标志块四面的 logo + 贴墙标的品牌 logo（同 xiamen/jiaomei 的做法）
//   · textures/item/tab_<id>.png     创造页签图标（TabIcons 的纯图标物品用）
//
// 用法：
//   node tools/gen-region-logo-assets.js             # 默认从 Downloads 取那三张图
//   SRC=/path/to/dir node tools/gen-region-logo-assets.js
const fs = require("fs");
const path = require("path");

const VER = process.argv[2] || "v1.20.1";
const ROOT = path.join(VER, "src/main/resources");
const SRC = process.env.SRC || "C:/Users/AD/Downloads";

// id → 源图 + 中文名（页签图标 / 标志块的 lang 由 lang 文件负责，这里只写资源）。
const BRANDS = [
  { id: "fuzhou", src: "福州地铁.png" },
  { id: "shenzhen", src: "深圳地铁.png" },
  { id: "mtr_hk", src: "港铁.png" },
];

const writeText = (rel, text) => {
  const p = path.join(ROOT, rel);
  fs.mkdirSync(path.dirname(p), { recursive: true });
  fs.writeFileSync(p, text);
  console.log("write", rel);
};

const copyBin = (srcFile, rel) => {
  const p = path.join(ROOT, rel);
  fs.mkdirSync(path.dirname(p), { recursive: true });
  fs.copyFileSync(srcFile, p);
  console.log("copy ", rel, `<${path.basename(srcFile)}>`, fs.statSync(p).size, "bytes");
};

// 与 jiaomei_logo.png.mcmeta 一致：不做 mipmap 模糊、边缘 clamp，否则 logo 边会糊出半透明毛边。
const MCMETA = `{\n  "texture": {\n    "blur": false,\n    "clamp": true\n  }\n}\n`;

const blockstate = (id) => `{\n  "variants": {\n    "": { "model": "mtrlte:block/${id}_logo" }\n  }\n}\n`;

// 满方块 + 四面 logo 覆盖层：与 models/block/jiaomei_logo.json 同一套元素，只换贴图名。
const blockModel = (id) => `{
  "parent": "block/block",
  "textures": {
    "particle": "mtr:block/metal",
    "metal": "mtr:block/metal",
    "logo": "mtrlte:block/${id}_logo"
  },
  "elements": [
    {
      "from": [0, 0, 0],
      "to": [16, 16, 16],
      "faces": {
        "down": { "texture": "#metal", "cullface": "down" },
        "up": { "texture": "#metal", "cullface": "up" },
        "east": { "texture": "#metal", "cullface": "east" },
        "west": { "texture": "#metal", "cullface": "west" },
        "north": { "texture": "#metal", "cullface": "north" },
        "south": { "texture": "#metal", "cullface": "south" }
      }
    },
    {
      "from": [-0.05, 0, -0.05],
      "to": [16.05, 16, 16.05],
      "shade": false,
      "faces": {
        "east": { "uv": [0, 0, 16, 16], "texture": "#logo", "cullface": "east" },
        "west": { "uv": [0, 0, 16, 16], "texture": "#logo", "cullface": "west" },
        "north": { "uv": [0, 0, 16, 16], "texture": "#logo", "cullface": "north" },
        "south": { "uv": [0, 0, 16, 16], "texture": "#logo", "cullface": "south" }
      }
    }
  ]
}
`;

// 物品图标：与 jiaomei_logo 的 display 完全同一套数值（方块类物品的标准三视角）。
const itemModel = (id) => `{
    "parent": "mtrlte:block/${id}_logo",
    "display": {
        "gui": {
            "rotation": [30, 225, 0],
            "translation": [0, 0, 0],
            "scale": [0.625, 0.625, 0.625]
        },
        "ground": {
            "rotation": [0, 0, 0],
            "translation": [0, 3, 0],
            "scale": [0.25, 0.25, 0.25]
        },
        "fixed": {
            "rotation": [0, 0, 0],
            "translation": [0, 0, 0],
            "scale": [0.5, 0.5, 0.5]
        },
        "thirdperson_righthand": {
            "rotation": [75, 45, 0],
            "translation": [0, 2.5, 0],
            "scale": [0.375, 0.375, 0.375]
        },
        "firstperson_righthand": {
            "rotation": [0, 45, 0],
            "translation": [0, 0, 0],
            "scale": [0.4, 0.4, 0.4]
        },
        "firstperson_lefthand": {
            "rotation": [0, 225, 0],
            "translation": [0, 0, 0],
            "scale": [0.4, 0.4, 0.4]
        }
    }
}
`;

const lootTable = (id) => `{
  "type": "minecraft:block",
  "pools": [
    {
      "rolls": 1,
      "bonus_rolls": 0,
      "entries": [
        {
          "type": "minecraft:item",
          "name": "mtrlte:${id}_logo"
        }
      ],
      "conditions": [
        {
          "condition": "minecraft:survives_explosion"
        }
      ]
    }
  ]
}
`;

// 页签图标：TabIcons 的纯图标物品（item/generated 单层）。
const tabItemModel = (id) => `{
  "parent": "item/generated",
  "textures": {
    "layer0": "mtrlte:item/tab_${id}"
  }
}
`;

for (const brand of BRANDS) {
  const src = path.join(SRC, brand.src);
  if (!fs.existsSync(src)) {
    console.error("!! 找不到源图", src);
    process.exit(1);
  }

  // 贴图：原图直拷（两份同字节）
  copyBin(src, `assets/mtrlte/textures/block/${brand.id}_logo.png`);
  writeText(`assets/mtrlte/textures/block/${brand.id}_logo.png.mcmeta`, MCMETA);
  copyBin(src, `assets/mtrlte/textures/item/tab_${brand.id}.png`);
  writeText(`assets/mtrlte/models/item/tab_${brand.id}.json`, tabItemModel(brand.id));

  // 方块
  writeText(`assets/mtrlte/blockstates/${brand.id}_logo.json`, blockstate(brand.id));
  writeText(`assets/mtrlte/models/block/${brand.id}_logo.json`, blockModel(brand.id));
  writeText(`assets/mtrlte/models/item/${brand.id}_logo.json`, itemModel(brand.id));
  writeText(`data/mtrlte/loot_tables/blocks/${brand.id}_logo.json`, lootTable(brand.id));
}
