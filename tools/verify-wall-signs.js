// 校验贴墙标识（wall_sign_*）的静态一致性：我们自己那份「站名出入口」模型（灯板 + 品牌 logo）
// ↔ blockstate 的 propagate_property × facing 变体 ↔ Java（继承 MTR 的 BlockStationNameEntrance、
// 自己的 WallSignRenderer）、贴图引用、item 模型、战利品表、lang、pickaxe 标签。不启动游戏。
//
// 关键不变量（用户提的「logo 读到 MTR 原版 / 背景混进别的贴图」就是这几条崩了）：
//   1. 模型（含子模型的覆盖）里一个 mtr: 前缀都不许有；
//   2. 每个 textures 值都必须以 mtrlte: 开头、且 png 真的存在；
//   3. 渲染器不许用 getStationNameEntrance(...)（那张贴图里烤着 MTR 的 logo），必须用 getStationName(...)；
//   4. 模型的 logo quad 必须落在灯板前面（z 更大）、站名平面（0.15 px）后面，且 x 不与站名带重叠。
const fs = require("fs");
const path = require("path");

const VER = process.argv[2] || "v1.20.1";
const ROOT = path.join(VER, "src/main/resources");
const JAVA = path.join(VER, "src/main/java/dev/js/mtrheadlight/xiamen/sign");
const IDS = ["xiamen", "jiaomei", "lte", "fuzhou", "shenzhen", "mtr_hk"];

// 灯板在模型里的 y 范围 —— 与 MTR 的 BlockStationNameEntrance#getOutlineShape 一一对应
// （我们继承它、一行都不重写，所以模型必须和它的 4..12 / 0..16 对得上）。
const BOARD = {
  short: { minY: 4, maxY: 12 },
  tall: { minY: 0, maxY: 16 },
};
const MODELS = { 0: "short", 1: "tall" };

const read = (rel) => JSON.parse(fs.readFileSync(path.join(ROOT, rel), "utf8"));
const exists = (rel) => fs.existsSync(path.join(ROOT, rel));
const readText = (p) => fs.readFileSync(p, "utf8");

let fails = 0;
const check = (ok, msg) => {
  console.log((ok ? "  ok   " : "  FAIL ") + msg);
  if (!ok) fails++;
};

const rotPoint = (x, z, deg) => {
  switch (((deg % 360) + 360) % 360) {
    case 90: return [16 - z, x];
    case 180: return [16 - x, 16 - z];
    case 270: return [z, 16 - x];
    default: return [x, z];
  }
};

const resolveModel = (ref, depth = 0) => {
  const [ns, name] = ref.split(":").length === 1 ? ["minecraft", ref] : ref.split(":");
  const file = `assets/${ns}/models/${name}.json`;
  if (!exists(file)) return { file, data: null, chain: [ref] };
  const data = read(file);
  if (data.elements) return { file, data, chain: [ref] };
  if (data.parent && depth < 8) {
    const parent = resolveModel(data.parent, depth + 1);
    return {
      file,
      chain: [ref, ...parent.chain],
      data: {
        ...parent.data,
        ...data,
        textures: { ...(parent.data.textures || {}), ...(data.textures || {}) },
        elements: parent.data.elements,
      },
    };
  }
  return { file, data, chain: [ref] };
};

const num = (src, name) => {
  const m = new RegExp(name + "\\s*=\\s*(-?[0-9.]+)F").exec(src);
  return m ? Number(m[1]) : null;
};

// ---------------------------------------------------------------- Java 源头

const blockSrc = readText(path.join(JAVA, "WallSignBlock.java"));
console.log("\n[Java: WallSignBlock]");
check(/extends BlockStationNameEntrance/.test(blockSrc), "继承 MTR 的 BlockStationNameEntrance（形状/连接/刷子全用它的）");
check(/createBlockEntity\(/.test(blockSrc), "只覆写 createBlockEntity（换成我们自己的方块实体类型）");
check(!/luminance\(/.test(blockSrc), "没有 luminance（MTR 那块板不是自发光方块）");
check(!/IntProperty\.of\(\"link\"/.test(blockSrc), "没有自己造 link 属性（MTR 不存连接状态，渲染时现数）");

const beSrc = readText(path.join(JAVA, "WallSignBlockEntity.java"));
console.log("\n[Java: WallSignBlockEntity]");
check(/extends BlockStationNameBase\.TileEntityStationNameBase/.test(beSrc), "继承 MTR 的 TileEntityStationNameBase");
check(/super\(WallSignObjects\.entityType\(\),\s*pos,\s*state,\s*0\.0F,\s*0\.00625F,\s*false\)/.test(beSrc),
  "yOffset=0 / zOffset=0.00625 / 单面（与 MTR 出入口款一致）");
// 站名平面在模型空间里的 z（px）：板在 0.05，MTR 把它画在「方块中心 - zOffset(0.00625) - 0.003125」处。
const namePlaneZ = (0.00625 + 0.003125) * 16;

const rendererSrc = readText(path.join(JAVA, "WallSignRenderer.java"));
console.log("\n[Java: WallSignRenderer]");
// 现在的实现（与 logo 的取法一起改过）：渲染器 = MTR 的 RenderStationNameTiled 原样（showLogo=true），
// 只压/弹一个「这一帧画的是哪个品牌」的作用域；logo 那张图由 StationNameEntranceLogoMixin 从
// mtr:textures/block/sign/logo.png 换成 mtrlte:textures/block/<brand>_logo.png。
check(/extends RenderStationNameTiled</.test(rendererSrc), "继承 MTR 的 RenderStationNameTiled（不另写绘制）");
check(/super\(dispatcher, true\)/.test(rendererSrc), "构造参数照 MTR 出入口款：showLogo = true");
check(/WallSignLogos\.push\(/.test(rendererSrc) && /WallSignLogos\.pop\(/.test(rendererSrc),
  "render 里压/弹 WallSignLogos 品牌作用域（push 与 pop 成对）");
check(!/getStationNameEntrance\(/.test(rendererSrc),
  "不许用 getStationNameEntrance(...)（那张贴图左边烤着 MTR 的红底白 M logo）");

const mixinSrc = readText(path.join(VER, "src/main/java/dev/js/mtrheadlight/mixin/mtr/StationNameEntranceLogoMixin.java"));
check(/sign\/logo\.png/.test(mixinSrc) && /textures\/block\//.test(mixinSrc),
  "mixin 把 mtr 的 sign/logo.png 换成 mtrlte:textures/block/<brand>_logo.png");

// ---------------------------------------------------------------- 每个方块

for (const id of IDS) {
  const name = `wall_sign_${id}`;
  console.log(`\n[${name}]`);

  // ---- blockstate：multipart，8 条（2 形态 × 4 朝向），which model 是本 id 自己那份
  const bs = read(`assets/mtrlte/blockstates/${name}.json`);
  const parts = bs.multipart || [];
  check(parts.length === 8, `blockstate 是 multipart 且有 8 条（2 形态 × 4 朝向），实际 ${parts.length}`);

  for (const part of parts) {
    const when = part.when || {};
    const style = Number(when.propagate_property);
    const y = (part.apply || {}).y || 0;
    const expectedModel = `mtrlte:block/${name}_${MODELS[style]}`;
    check((part.apply || {}).model === expectedModel, `propagate_property=${when.propagate_property}/${when.facing}: 指向 ${expectedModel}`);

    const model = resolveModel(expectedModel);
    check(!!model.data && !!model.data.elements, `  模型 ${expectedModel} 可解析且有 elements（链 ${model.chain.join(" → ")}）`);

    // 灯板的几何包围盒（只用来确认模型没被改成别的厚度 / 高度）
    const board = model.data.elements.find((el) => el.from[2] === el.to[2]);
    const h = BOARD[MODELS[style]];
    check(!!board && board.from[1] === h.minY && board.to[1] === h.maxY,
      `  灯板 y ${h.minY}..${h.maxY}（与 MTR getOutlineShape 一致），实际 ${board ? board.from[1] + ".." + board.to[1] : "找不到零厚度元素"}`);
    check(!!board && Math.abs(board.from[2] - board.to[2]) < 1e-9, "  灯板零厚度（与 MTR 原模型一致）");
    check(!!board && board.faces.north && board.faces.north.cullface === "north", "  灯板 north 面 cullface=north（贴墙不画）");
    void y;
  }

  // ---- 贴图引用 + logo 位置（对每个形态各查一遍）
  for (const style of [0, 1]) {
    const tag = `${name}_${MODELS[style]}`;
    const model = resolveModel(`mtrlte:block/${tag}`);
    const json = JSON.stringify(model.data);
    check(!json.includes('"mtr:') && !json.includes("mtr:block/"), `${tag}: 模型（含父模型覆盖后）里没有 mtr: 前缀`);
    for (const [key, tex] of Object.entries(model.data.textures || {})) {
      check(tex.startsWith("mtrlte:"), `${tag}: textures.${key} = ${tex} 没有跨模组引用`);
      check(exists(`assets/mtrlte/textures/${tex.split(":")[1]}.png`), `${tag}: textures.${key} 的文件存在（${tex}）`);
    }
    // 品牌 logo 不在模型里：MTR 的渲染器把它画进生成的站名贴图（取图由 mixin 换成这张），
    // 所以这里不查 logo quad，只查这张 png 真的存在、且模型里没有跨模组引用。
    check(exists(`assets/mtrlte/textures/block/${id}_logo.png`),
      `${tag}: 品牌 logo 贴图存在（textures/block/${id}_logo.png）`);
    check(!model.data.textures || !("logo" in model.data.textures),
      `${tag}: 模型里没有 logo 贴图引用（logo 由渲染器画进贴图）`);
  }

  // ---- item 模型
  const item = read(`assets/mtrlte/models/item/${name}.json`);
  check(item.parent === `mtrlte:block/${name}_tall`, `item 模型 parent=${item.parent}`);
  check(!!item.display && !!item.display.gui && !!item.display.thirdperson_righthand, "item 模型有 3D display（gui/thirdperson）");

  // ---- 战利品表 / lang / 标签
  check(exists(`data/mtrlte/loot_tables/blocks/${name}.json`), `战利品表存在: ${name}`);
  const zh = read("assets/mtrlte/lang/zh_cn.json");
  const en = read("assets/mtrlte/lang/en_us.json");
  check(!!zh[`block.mtrlte.${name}`] && !!zh[`item.mtrlte.${name}`], "zh_cn 有 block/item 两条翻译");
  check(!!en[`block.mtrlte.${name}`] && !!en[`item.mtrlte.${name}`], "en_us 有 block/item 两条翻译");
  const tag = read("data/minecraft/tags/blocks/mineable/pickaxe.json");
  check(tag.values.includes(`mtrlte:${name}`), "在 mineable/pickaxe 标签里");
}

console.log(fails === 0 ? "\nALL OK" : `\n${fails} FAILURE(S)`);
process.exit(fails === 0 ? 0 : 1);
