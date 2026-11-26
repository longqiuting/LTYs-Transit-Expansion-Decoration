# 超高清贴图不爆显存：方速做法拆解 + 落到 LTED 的建议

调研对象：`[方速MTR扩展] [MTR3]fangsu-mc1.20.1-FABRIC-1.4.0-build20261120101347.jar`（67 MB，259 张 PNG，1792 个类）。
手段：解包 + `javap -p` / `javap -p -c` / `javap -p -constants` 读常量池与方法体，配合 `assets/` 里的 JSON 交叉验证。

---

## 0. 一句话结论

**方速没做什么聪明事。它只是把大贴图放在方块图集外面。**

方速的超大贴图（4000×4000、2560×1494、1024×1024）绝大多数是**每材质一张独立 png、用 `RenderType.getEntityCutout(全路径带 .png)` 直绑**，不走 `textures/block` 的方块图集；同时靠三件配套事把它撑住：方块实体按距离整块剔除、几何进 VBO 常驻、动态贴图有完整释放生命周期。
它**没有**做 mipmap 分级、没有按距离换贴图、没有图集分页、没有按站台只加载一张、也没有探测 `GL_MAX_TEXTURE_SIZE`。

反过来，方速**自己也踩了一次图集坑**：`textures/block/ticket_machine_new.png` 是 4000×4000，并且被 `models/block/ticket_machine_lower.json` 引用 —— 这张是真的进了方块图集的。也就是说「大图不进图集」是它的习惯，不是它的机制。

---

## 1. 方速的具体手段与证据

### 1.1 渲染层：每个材质一张独立贴图，不进方块图集

这是全部答案的核心。方速自研了一套 GPU 批渲染器，包名 `com.fangsu.render.sowcer`，模型的基本单位是：

```
com.fangsu.render.sowcer.model.Model   → ArrayList<Mesh>
com.fangsu.render.sowcer.model.Mesh    → VertBuf + IndexBuf + MaterialProp (implements Closeable)
```

`MaterialProp` 就是「一份材质」的完整描述，关键字段：

| 字段 | 类型 | 作用 |
|---|---|---|
| `texture` | `Identifier` (`class_2960`) | 这张材质自己的 png，**完整路径带 `.png`** |
| `normalTexture` | `Identifier` | 法线贴图（可选） |
| `normalScale` | `float` | 法线强度 |
| `translucent` | `boolean` | 走半透明 RenderType |
| `cutoutHack` | `boolean` | alpha 裁切 |
| `writeDepthBuf` | `boolean` | 是否写深度 |
| `sheetElementsU` / `sheetElementsV` | `int` | 序列帧的格数（**不是**图集分页） |
| `WHITE_TEXTURE_LOCATION` | `static Identifier` | 无贴图时的白图兜底 |

绑定方式在 `MaterialProp.getBlazeRenderType()` 里，`javap -c` 的常量池给出确凿证据：

```
NormalMapShader.getRenderType(MaterialProp) -> RenderType | null
  命中则直接返回缓存
未命中则按 shaderName 分派，常量池里三个字符串：
  "rendertype_entity_cutout"
  "rendertype_entity_translucent_cull"
  （第三个是默认分支）
最终调用 RenderType.entityCutout(texture) / entityTranslucentCull(texture)
```

`RenderType.entityCutout(Identifier)` 在 1.20.1 里构造的是 `TextureStateShard(texture, blur=false, mipmap=false)` —— 也就是说：

1. **贴图 id 就是文件路径**，直接交给 `TextureManager` 当独立 GL 纹理上传，**和方块图集完全无关**；
2. **不生成 mipmap**（`mipmap=false`），所以 4000×4000 恰好是 4000×4000×4 B = **61 MiB**，没有图集那层 ×1.33 的 mipmap 开销；
3. 贴图由 `TextureManager` **懒加载** —— 第一次被 bind 时才上传。世界里没有这台机器，这 61 MiB 就一个字节都不占。

真正的技能点在第 1 条。LTED 的 `RawModelRenderer.cutoutLayer()` 和 `PsdLcdRenderer` 的 `RenderLayer.getEntitySolid()` 走的是同一条路 —— 这部分我们对齐了，问题不在机制，在**哪些图被放进了图集**。

### 1.2 大贴图到底放在哪：`assets/fangsu/<分类>/xxx.png`

方速的大图基本放在 `textures/` 外面，这样任何 model JSON 都不可能引用到它，物理上排除了被缝合进图集的可能。从类常量池里挖到的实际路径：

| 常量 | 尺寸 | 归属 |
|---|---|---|
| `fangsu:ticketbarrier/welcome.png` | 2560×1494 | `BlockEntityAdvBoard` 直绑 |
| `fangsu:sign/routea.png`、`routeb.png`、`texts.png`、`trainicon.png` | 各 ≤512 | 站牌绘制 |
| `fangsu:pids/black.png` | 小 | PIDS 底板（6 处引用） |
| `fangsu:sign/def_face1.png` | 小 | 默认牌面 |
| `fangsu:screendoor/no_entry_1.png` | 1024×1024 | 屏蔽门 |
| `minecraft:textures/misc/white.png` | — | 白图 |

方速 jar 里 `assets/fangsu/ticketbarrier/welcome.png` 落地 3.4 MB / 2560×1494 = 14.6 MiB 显存，只有一块广告板在视野里才上传。

**反例**：`assets/fangsu/textures/block/ticket_machine_new.png` 4000×4000（6.8 MB），被 `models/block/ticket_machine_lower.json` / `_upper.json` / `models/item/ticket_machine.json` 三处引用 → 真的进了方块图集。单这一张就足以把 blocks 图集的一边顶到 ≥4096，且它被 mipmap。

顺带说：方速这个模型的 UV 只用到 `[0.072,0.132]..[8.644,15.248]` 这种小区域，属于素材没裁干净。**它是反面教材，不是设计意图。**

### 1.3 距离剔除：整个方块实体按距离不画

`com.fangsu.blockEntities.client.BaseBlockEntityRender<T extends BaseObjBlockEntity> implements BlockEntityRenderer<T>`

`javap -p -constants` / `static {}` 拿到两个常量：

```java
private static final int  MAX_RENDER_DISTANCE    = Integer.getInteger("fangsu.blockEntityRenderDistance", 128);
private static final double MAX_RENDER_DISTANCE_SQ = (double) MAX_RENDER_DISTANCE * MAX_RENDER_DISTANCE;
```

`shouldRender(T, Vec3d)` 的实现：`MAX_RENDER_DISTANCE <= 0` 时直接 `return true`（等于关闭限制），否则算 `be.getPos()` 中心到相机的平方距离和 `MAX_RENDER_DISTANCE_SQ` 比。
`shouldRenderOffScreen(T)` 恒 `return false`（`iconst_0; ireturn`）—— 视锥外不画。

128 格剔除 + 懒加载 = **远处那 61 MiB 的 4K 贴图不会被 bind，也就不会被上传**。这是方速在「一个站台摆几十台闸机」时还能跑的原因，和贴图压缩本身无关。

### 1.4 几何常驻显存，不逐帧重传

- `Mesh` = `VertBuf`（顶点）+ `IndexBuf`（索引）+ `MaterialProp`，`implements Closeable`。
- `Model implements Closeable`，持 `ArrayList<Mesh>`，`close()` 释放 GL buffer。
- `OffHeapAllocator` 用 `org.lwjgl.system.MemoryUtil.MemoryAllocator` 分配**堆外**直接内存做上传暂存（`allocate/resize/free`），避开 GC 堆抖动。
- `GlStateTracker` 有 `capture()/restore()/assertProtected()`，带 `LEAK_STRIKE_LIMIT` 计数和 `leakWarned` 一次性告警 —— 防止自定义渲染把 GL 状态搞脏。

### 1.5 逐帧批处理：同一材质一帧只 bind 一次

`BatchManager`：

```java
HashMap<BatchTuple, Queue<RenderCall>> batches;          // BatchTuple = (MaterialProp, ShaderProp)
void enqueue(VertArrays / VertArray, EnqueueProp, ShaderProp);
void drawAll(ShaderManager, DrawContext);                // 按 BatchTuple 分桶后逐个 draw
```

`BatchTuple.equals/hashCode` 用 `MaterialProp` 的结构相等 → 同贴图、同 shader、同混合状态的所有实例合成一次 draw call，**一张 4K 贴图一帧只 bind 一次**。
另有一个 `com.fangsu.render.MtrTranslucentPass`，用 `Map<RenderLayer, Batch> BATCHES` + `ArrayDeque<Batch> AVAILABLE`（对象池）单独收半透明面，`flush(VertexConsumerProvider)` 时按距离排序后统一画。

`DrawContext` 里带 `drawCallCount / batchCount / singleFaceCount / instancedFaceCount / blazeFaceCount` 的帧内计数器，说明他们是**拿仪表面板盯 draw call 的**，不是拍脑袋优化。

### 1.6 动态贴图有完整的注册/释放生命周期

`com.fangsu.scripting.GraphicsTexture implements AutoCloseable`：

| 成员 | 作用 |
|---|---|
| `DynamicTexture dynamicTexture` | 实际的 GL 纹理 |
| `Identifier identifier` | 在 TextureManager 里的注册名 |
| `AtomicBoolean isClosed / isRegistered / isTextureValid / isReleased` | 四个独立状态位，防止双释放 |
| `static ConcurrentHashMap<Identifier, GraphicsTexture> ACTIVE_TEXTURES` | 全局登记表，可按 id 反查 |
| `registerTexture()` / `upload()` / `bind()` | 注册、上传、绑定分开 |
| `closeLater()` | `isTextureValid=false` 后 `client.execute(...)` **投递到渲染线程**再释放 |
| `releaseResources()` | `TextureManager.registerTexture(id, null)` 拿回旧对象 → **身份比对** `old == this.dynamicTexture` 才 `destroyTexture(id)` |

最后那条身份比对很关键：资源包重载后 id 可能已经指向别人的纹理，直接 `destroyTexture` 会把别人的图删掉（就是「黑紫方块」的经典成因）。`PsdLcdRenderer` 注释里提到的「不会像上一版那样黑紫方块」也是同一个坑。

### 1.7 网络 / GIF 贴图有硬上限（这是唯一带数字的显存闸门）

`com.fangsu.scripting.GifHelper`，`javap -p -constants`：

```java
private static final int MAX_WEB_IMAGES    = 24;
private static final int MAX_WEB_PIXELS    = 4194304;   // 4M 像素 = 16 MiB RGBA8
private static final int MAX_WEB_DIMENSION = 4096;
```

`checkWebSize(int w, int h, int frames)` 的判定（`javap -c` 可见 `sipush 4096` / `ldc2_w 4194304L`）：

```
w <= 0 || h <= 0                     -> throw IOException("Decoded image is too large")
w > 4096 || h > 4096                 -> throw
(long) w * h * frames > 4194304      -> throw
```

配套 API：`bindGif / unbindGif / getGifTextureLocation / hasGif / pause / resume / disposeAll / shutdown`，还有 `bindGif(BlockPos, …)` / `unbindGif(BlockPos)` 的**按方块坐标绑定**版本（`blockPosToId`）。即「谁在显示 → 谁负责释放」，方块没了调 `unbindGif` 就把 GL 纹理还回去。

注：`MAX_WEB_IMAGES` 只在 `GifHelper` 内部声明，`javap -c` 里**没有找到它的读取点**，所以这条更像是留下了口子没接上（或者靠 `webExecutor` 的有界线程池间接限流）。另外两条 `checkWebSize` 里的 4096 / 4M 是**真在生效**的。

### 1.8 方速明确**没有**做的事（逐条否认，都是 grep 过全 jar 的）

| 手段 | 方速有没有 | 依据 |
|---|---|---|
| 按距离 / 视角切 mip 级别 | **没有** | 全 jar 无 `generateMipmaps` / `MipmapGenerator` / `GL_LINEAR_MIPMAP` / `mipLevel` / `LevelOfDetail` 任何一处 |
| 探测 `GL_MAX_TEXTURE_SIZE` | **没有** | 全 jar 无 `GL_MAX_TEXTURE_SIZE` / `maxSupportedTextureSize` |
| 图集分页 / 只加载当前站台那一张 | **没有** | 大图根本不进图集，无从分页；也没有任何「按站台筛选贴图」的逻辑 |
| 方块图集排除机制（mixin SpriteLoader/Stitcher） | **没有** | `fangsu.mixins.json` 里没有 atlas 相关 mixin；靠「不引用」实现排除 |
| 运行时降采样 | **没有** | 只有 `TextureDownscale` 那类离线工具才有；jar 内无运行时 resize 路径 |
| 运行时纹理尺寸上限（大图本身） | **没有** | 4096/4M 的闸门只装在 `GifHelper`（网络/GIF）上，**自家打包的 PNG 完全不设限** |

所以「方速 4K 不爆显存」= **不移进图集 + 不生成 mipmap + 懒上传 + 128 格剔除**。四条叠起来，一块 4K 贴图只在「站在它 128 格内」时占 61 MiB。

---

## 2. LTED 现状（实测，非估算）

`TextureAudit` 口径：显存 = Σ(w×h×4)，RGBA8 未压缩、未 mipmap。

### 2.1 默认包 `v1.20.1/src/main/resources/assets`

```
354 张 PNG | 6.2 MB 落地 | 109 MiB 显存 | 最大边 512 | >512: 0 张
分布: <=128 = 183 | <=256 = 56 | <=512 = 115
```

> 所谓「152 张 >256px」在 512 降分辨率之后已经变成 **115 张**，全部 ≤512。

### 2.2 内置高清包 `resourcepacks/lted_hd`（= `hd/` 备份，344 张，18.3 MB）

```
344 张 PNG | 18.3 MB 落地 | 1388 MiB 显存 | 最大边 5000 | >512: 66 张 | >1024: 36 张
分布: <=128=175 | <=256=56 | <=512=47 | <=1024=30 | >1024=36
```

**开高清包 = 显存从 109 MiB 涨到 1388 MiB（+1.25 GiB）。**

### 2.3 按目录拆开看（决定生死的不是总量，是「在不在图集里」）

| 目录 | 默认包 | 高清包 | 是否进方块图集 | 渲染方式 |
|---|---|---|---|---|
| `textures/block` | 142 张 / **42 MiB** / ≤512 | 140 张 / **438 MiB** / 最大 4096 | ✅ 图集（142 张里 **95 张**被 model 引用，其余 47 张不缝合） | 烘焙方块模型 |
| `textures/item` | 118 张 / 4 MiB / ≤128 | 118 张 / 4 MiB / ≤128 | ✅ 图集 | 同上 |
| `textures/eyecandies` | 59 张 / **51 MiB** / ≤512 | 59 张 / **916 MiB** / 最大 5000 | ❌ 直绑 | `RawModelRenderer.cutoutLayer` → `getEntityCutout` |
| `textures/fangsu_psd` | 15 张 / 3 MiB | 15 张 / 9 MiB / 最大 1024 | ❌ 直绑 | `getEntityCutout` |
| `advertisements` | 4 张 / 2 MiB | 4 张 / 2 MiB | ❌ 直绑 | `PsdLcdRenderer` → `getEntitySolid` |
| `textures/gui` | 7 张 / 2 MiB | 7 张 / 18 MiB / 最大 2144 | ❌ | GUI |

两点值得注意：

1. **`textures/eyecandies` 高清包 916 MiB 是最贵的一块，但它是安全的** —— 走直绑，懒上传，世界里没那种装饰就不占显存。「5000×5000 的 `texturexlt.png` = 95 MiB」只在它进视野时付。这半边我们已经做对了。
2. **`textures/block` 高清包 438 MiB 是危险的那块** —— 它是**图集**，全部常驻、全部 mipmap，而且大图会把整张图集的边长顶上去。

### 2.4 高清包里 >512 且**在图集里**的 23 张（问题清单）

`hd/mtrlte/textures/block/` 逐张核过 model 引用：

| 尺寸 | 张数 | 文件 | 图集 |
|---|---|---|---|
| 4096×4096 | 4 | `amtr_ticket.png`、`amtr_ticket_closed.png`、`charger.png`、`balance_machine.png` | **是** |
| 2048×2048 | 4 | `ticket_barrier.png`、`station_sign_xiamen.png`、`station_sign_jiaomei.png`、`brt_barrier.png` | **是** |
| 1744×1744 | 1 | `floor_hangzhou.png` | **是** |
| 1024×1024 | 2 | `railing_stair_glass.png`、`liquid_scanner.png` | **是** |
| 872×872 | 1 | `floor_hangzhou_floor.png` | **是** |
| 600×600 | 1 | `xiamen_logo.png` | **是** |
| 576×288 | 10 | `psd_door_top_{left,right}_1[_l2..l6].png` | **是** |
| 4096×4096 | 1 | `drinks_machine.png` | 否（未引用，白占 6 MB 落地体积） |
| 544×432 / 544×144 | 4 | `apg_door_{bottom,top}_{left,right}.png` | 否 |

这 **23 张图集内大图本身**合计 **350.2 MiB**（含 mipmap 摊到 **~467 MiB**）。注意这是「按 sprite 面积相加」的口径；实际代价是下面这个「整张图集被撑大」的口径，两者别混。

更要命的是**图集尺寸**：方块图集是**一张** `textures/atlas/blocks.png`，所有 sprite 缝进同一张。只要有一张 4096×4096，图集一边就是 ≥4096；四张 4096² 加四张 2048² 之后，缝出来的图集不可能小于 **4096×8192（128 MiB）**，实际多半要到 **4096×16384（256 MiB）**，再乘 mipmap ≈ **341 MiB**。

也就是说：**光为了让这 4 张票机/闸机贴图是 4K，全世界所有方块都要背一张 341 MiB 的图集。** 这才是「开高清包卡成 PPT」的真正原因，而不是 1388 MiB 这个总数。

### 2.5 LTED 已经做对的部分（不要推倒重来）

- `RawModelRenderer.CUTOUT_LAYERS`：`Map<Identifier, RenderLayer>` + `computeIfAbsent`。注释里已经踩过并写清了「`RenderLayer.getEntityCutout` 每次都 new，`Immediate` 按身份建缓冲，逐帧 new 会无限塞键」的坑 —— 和方速 `NormalMapShader.RENDER_TYPES` 缓存的 `MaterialKey(shader, texture, normal, strength)` 是同一个设计。
- `PsdLcdRenderer`：`RenderLayer.getEntitySolid(SOLID_TEXTURE)` 直绑，注释明确写了「别让它走图集」。
- `RawObjParser.materials()`：按 OBJ 的 `map_Kd` 拆材质，每份 `RawMesh` 带自己的 `texture()`，`MATERIAL_CACHE` 缓存。这已经等价于方速的 `Model → Mesh(MaterialProp)` 结构。
- `client/RenderDistance`：`blocks() = clamp(viewDistance*16 + 32, 64, 512)`，`inRange(be, cameraPos)` 做了平方距离比较。已有 **15 个文件 / 28 处**调用（全项目 17 个 BER 类）。
- `tools/verify-hd-pack.sh` + `tools/TextureAudit.java` + `tools/TextureDownscale.java`：已经有「默认包断言 ≤512 / 高清包逐字节比对原图」的验收链路。

**缺的只有一样：没有人对「高清包里的图集路径」单独设上限。** `verify-hd-pack.sh` 第 1 步只断言 `assets/**`（默认包），高清包 `resourcepacks/lted_hd/**` 只数了张数、抽查了 md5，**没有做尺寸断言**。所以 4096² 就这么进来了。

---

## 3. 对比表

| 维度 | 方速 | LTED 现状 | 差距 |
|---|---|---|---|
| 大贴图载体 | 独立 png，`assets/fangsu/<分类>/` | `textures/block`（部分）+ `textures/eyecandies`（部分） | ⚠️ 图集里混进了 23 张大图 |
| 绑定 API | `RenderType.entityCutout / entityTranslucentCull(贴图)` | `RawModelRenderer.cutoutLayer` = `getEntityCutout`；`PsdLcdRenderer` = `getEntitySolid` | ✅ 对齐 |
| RenderLayer 缓存 | `NormalMapShader.RENDER_TYPES` + `MaterialKey` 记录 | `CUTOUT_LAYERS` + `computeIfAbsent` | ✅ 对齐 |
| 贴图懒加载 | 靠 `TextureManager` 首次 bind 才上传 | 同样靠 `TextureManager` | ✅ 对齐（显式图集例外） |
| mipmap | 不开（`mipmap=false`） | 图集自动开（+33%）；直绑不开 | ⚠️ 图集那 438 MiB 要乘 1.33 |
| 方块实体距离剔除 | `MAX_RENDER_DISTANCE = 128`（可 `-D` 覆盖），`shouldRenderOffScreen=false` | `RenderDistance.inRange`，64–512 格，跟区块视距走 | ✅ 对齐（LTED 更宽，但可控） |
| 几何常驻 | `Mesh` = VBO+IBO，`Closeable` 释放 | 每帧写 `VertexConsumer`，由 `Immediate` 缓冲 | ⚠️ LTED 没做 VBO 常驻 |
| 批处理 | `BatchManager` 按 `(MaterialProp, ShaderProp)` 分桶；`MtrTranslucentPass` 收半透明 | 靠 `Immediate` 按 RenderLayer 合并 | ⚠️ 同 RenderLayer 可合，跨贴图不行 |
| 动态贴图生命周期 | `GraphicsTexture`：4 个 AtomicBoolean + 身份比对 + `closeLater` 走渲染线程 | `PsdLcdRenderer` 有一套自己的动态纹理管理 | 基本对齐 |
| 全局像素预算 | `GifHelper`：4096 / 4M px / 24 张（仅网络&GIF） | **无任何上限** | ❌ 完全缺失 |
| 打包资源尺寸闸门 | 无（自家 PNG 不设限，所以有 4000² 进图集） | 默认包有（`verify-hd-pack.sh` 断言 ≤512），高清包无 | ⚠️ 补上高清包断言即可 |
| 运行时 LOD / 距离换图 | 无 | 无 | — 机会点 |

---

## 4. 落到 LTED 的三条建议（按性价比排序）

### 建议 1：高清包里 23 张「图集内 >512」的图，按危险度逐批拆出图集 ★ 最高性价比

**改哪里**

1. `tools/verify-hd-pack.sh` 加第 6 步断言（改动最小、立刻生效）：
   ```bash
   # 6) 高清包图集路径：必须 ≤512（图集是全场景常驻 + mipmap，不允许大图）
   "$JAVA" -Dfile.encoding=UTF-8 tools/TextureAudit.java \
       "$WORK/resourcepacks/lted_hd/assets/mtrlte/textures/block" 512
   "$JAVA" -Dfile.encoding=UTF-8 tools/TextureAudit.java \
       "$WORK/resourcepacks/lted_hd/assets/mtrlte/textures/item" 512
   ```
   `TextureAudit` 已经支持 cap 参数和退出码，不用改工具。
2. 按危险度删/降 `hd/mtrlte/textures/block/` 下的图（`hd/` 是构建输入，`resourcepacks/` 是产物，两边都要改或重新生成）：
   - **第一批（零风险，收益 320 MiB）**：4 张 4096² + 4 张 2048² → 512。这 8 张就是「一张图集逼出 341 MiB」的元凶。512 对一台票机/闸机/站牌已经足够（1 格 = 16px，512 = 32 px/格）。
   - **第二批（低风险）**：`floor_hangzhou 1744²`、`railing_stair_glass / liquid_scanner 1024²`、`floor_hangzhou_floor 872²`、`xiamen_logo 600²` → 512。再省 ~30 MiB + 把图集边长从 2048 档回落到 1024 档。
   - **第三批（要动渲染）**：10 张 `psd_door_top_*` 576×288。它们是 PSD 门的上半段，若要保留 576 就得走 `getEntityCutout` 直绑；收益只有 6.3 MiB，**不建议现在做**。
   - `drinks_machine.png`（4096²，无 model 引用）直接删 —— 白占 6 MB 包体积。

**预期显存收益**

| | 现在 | 第一批后 | 三批都做后 |
|---|---|---|---|
| 图集内大图 | 350 MiB（mip 后 467） | 30 MiB（mip 后 40） | 24 MiB（mip 后 32） |
| blocks 图集边长 | ≥4096×8192 | ≤2048 | ≤1024 |
| blocks 图集实占 | ~341–400 MiB | ~70–90 MiB | ~42 MiB（≈ 默认包水平） |
| **高清包总显存** | **1388 MiB** | **~1030 MiB** | **~950 MiB** |

只做第一批就能把「全世界背一张 341 MiB 图集」这件事消掉，这是开高清包卡顿的主因。

**风险**

- 4K→512 是**画质回退**，票机正面那圈文字/图标在近距离会糊。缓解：`ticket_machine_xiamen_*` 的 UV bbox 实测只用 0..11.875 × 0..11.86（约 74%×74%），先把 4096² **裁到 3040² 再降到 1024** 比直接 512 好看不少；但 `_closed` 两个模型没有显式 UV，裁图前必须先跑一遍 `tools/verify-rawmodels.js` 那类校验。
- 断言会让现有构建**立刻失败**，需要和素材同步改，别只加断言不同步图。
- 不动任何 Java 代码，纯资源 + 脚本。**这一条不碰业务代码，最安全。**

---

### 建议 2：给「直绑贴图」装上方速式的像素预算闸门 ★ 次高，跨版本可复用

方速唯一带数字的闸门全在 `GifHelper`。LTED 完全没有对应物，而 `eyecandies` 高清包有 **916 MiB / 单张 5000×5000 = 95 MiB**。

**改哪里**

1. 新建 `v1.20.1/src/main/java/dev/js/mtrheadlight/client/TextureBudget.java`（新文件，不动现有类）：

   | 常量 | 值 | 对应方速 |
   |---|---|---|
   | `MAX_DIMENSION` | `2048` | `GifHelper.MAX_WEB_DIMENSION = 4096` |
   | `MAX_PIXELS` | `4194304`（4M px = 16 MiB） | `GifHelper.MAX_WEB_PIXELS = 4194304` |
   | `MAX_TEXTURES` | `24` | `GifHelper.MAX_WEB_IMAGES = 24` |

   方法对照 `checkWebSize(w, h, frames)`：`boolean check(int w, int h)`，超限时 **降采样到预算内再上传**（不是像方速那样抛异常 —— 我们的图是自家打包的，抛异常等于崩游戏）。
2. 挂载点挑两处：
   - `RawModelRenderer.cutoutLayer()`：在 `computeIfAbsent` 之前查一次预算，对超限贴图回退到一张预降采样的同名 id（`textures/eyecandies/<name>_2k.png`）。
   - `PsdLcdRenderer` 的动态纹理构造：在 `BufferedImage` 尺寸确定后查一次，超了就用 `Graphics2D.drawImage` 缩。
3. `tools/verify-hd-pack.sh` 第 7 步加同样的断言（对 `textures/eyecandies`）：

   ```bash
   # 7) 直绑路径：单张最大边 ≤2048（方速 GifHelper 档位），总显存 ≤1 GiB
   "$JAVA" -Dfile.encoding=UTF-8 tools/TextureAudit.java \
       "$WORK/resourcepacks/lted_hd/assets/mtrlte/textures/eyecandies" 2048
   ```

**预期显存收益**

- `texturexlt.png` 5000² → 2048²：**95 MiB → 16 MiB（省 79 MiB/张）**。
- 高清包 `eyecandies` 916 MiB 里，24 张 >1024 的按 2048 封顶，粗估 **916 → ~420 MiB（省 ~500 MiB）**。
- 合计建议 1 + 建议 2：**1388 MiB → ~450–500 MiB**，比现在的默认包只贵 4 倍、比现状便宜 65%。

**风险**

- 「降采样到预算内」要在**上传前**做，不能在 GPU 上做；`RawModelRenderer` 是纯 CPU 路径，加 `BufferedImage` 缩放会引入 `ImageIO` 依赖到渲染热路径 —— **必须在资源重载时（`ClientResourceReload` 回调）预计算，不能在 `render()` 里算**，否则每帧几十次 `ImageIO.read` 会卡死。
- 更省事的替代：**只用静态断言**（第 3 步那行 `TextureAudit`），不写 Java。收益一样（图本来就该在打包时缩好），风险归零。**建议先只做断言，Java 侧等有动态纹理需求时再上。**
- 预算常量会和未来想要更高清的素材冲突，需要留 `-Dmtrlte.textureBudget.maxDimension=` 覆盖口（照方速 `Integer.getInteger("fangsu.blockEntityRenderDistance", 128)` 的写法）。

---

### 建议 3：给直绑 + 高模做「距离分级」——方速没做，是我们能弯道超车的地方 ★ 长期

方速的距离剔除是**二值**的：128 格内全画、128 格外全不画。中间没有过渡，走到 128 格边界会「啪」一下出现/消失。它也没做 mip 分级，所以远处的高模会闪烁（aliasing）。

LTED 的 `RenderDistance.blocks()` 已经能拿到当前视距（64–512），`inRange` 也在 15 个文件里用了。缺的是**分级**。

**改哪里**

1. `client/RenderDistance` 加一个 `Level` 枚举和 `levelOf(BlockEntity, Vec3d)`：

   | 档位 | 距离 | 画什么 |
   |---|---|---|
   | `NEAR` | `d < 0.35 * blocks()` | 全模 + 高清贴图 |
   | `MID` | `d < 0.7 * blocks()` | 全模 + 默认（512）贴图 |
   | `FAR` | `d < blocks()` | 只画主材质层（`materials()` 里 `map_Kd` 的第一个），跳过灯带/黑框等次材质 |
   | `NONE` | 更远 | 不画（现在就是这行为） |

   判据全部是现成的：`RenderDistance.blocks()`、`RawObjParser.materials()` 返回的 `LinkedHashMap`（`materials()` 注释已经写明按「首次出现的顺序」），`RawMesh.texture()`。
2. 贴图切换不需要两套 id：`RawMesh` 已经有 `texture()` 字段，渲染时按 `Level` 选 `mesh.texture()` 或 `<name>_512` 版本即可。`CUTOUT_LAYERS` 的 key 是 `Identifier`，天然支持两套。
3. 验收：`tools/check-pids-draw-overload.sh` 那套思路可以复用，加一个「同一场景 LOD 切换后 draw call 不增加」的断言。

**预期显存收益**

- **显存基本不降**（同一张直绑贴图只在首次 bind 时上传一次，LOD 换图只是换 bind 对象；两套 id 会让显存**上涨**约 25%）。
- 真正收益在 **GPU 带宽和 draw call**：远处只画主材质层，能砍掉方速那套 `MtrTranslucentPass` 在处理的东西（次材质、半透明面）。
- 这一条要和 `verify-hd-pack.sh` 的 `strip-textures.sh` 配合：`_512` 版本可以用 `TextureDownscale` 批量生成，包体积涨约 6 MB。

**风险**

- **显存不降反升** —— 如果目标是「不爆显存」，这条优先级最低，别指望它。
- LOD 边界切换会看到材质跳变，需要淡入或者把档位做得更细；调试成本高。
- 涉及 `RawModelRenderer` / `RawObjParser` 的改动，属于**业务代码**，本次不动。

---

## 5. 排序理由（为什么不是别的顺序）

| 建议 | 省多少显存 | 改动面 | 风险 | 性价比 |
|---|---|---|---|---|
| 1. 高清包图集内 23 张大图降到 512 | **~350–400 MiB**（去掉 341 MiB 的巨型图集） | 资源 + 一个 shell 断言，**0 行 Java** | 画质回退，可裁图缓解 | ★★★★★ |
| 2. 直绑贴图装 2048 / 4M 预算（先只做断言） | **~500 MiB** | 一个 shell 断言；Java 侧可选 | 只做断言风险≈0 | ★★★★☆ |
| 3. 距离分级 LOD | ≈0（甚至 +25%） | `RenderDistance` + `RawModelRenderer` + `RawObjParser` | 材质跳变，调试成本 | ★★☆☆☆（是性能项，不是显存项） |
| （不做）照抄 Fangsu 的 sowcer：VBO 常驻 + BatchManager | ≈0（显存是 CPU/带宽项） | 整套渲染器重写 | 极高 | ☆☆☆☆☆ |

**如果只做一件事：做建议 1 的第一批（8 张图 → 512）。** 一句话理由：那 8 张图让 `textures/atlas/blocks.png` 从 1024 档涨到 4096×8192 档，全世界所有方块（包括原版石头）都在替它们付显存，而这 8 张图各自只服务一台机器。

---

## 6. 复现方式

```bash
# 默认包（应全部 <=512）
java tools/TextureAudit.java v1.20.1/src/main/resources/assets

# 高清包（现状：最大 5000，36 张 >1024）
java tools/TextureAudit.java v1.20.1/src/main/resources/hd

# 高清包里"在图集内"且 >512 的清单
cd v1.20.1/src/main/resources/hd/mtrlte/textures/block
for f in *.png; do
  d=$(file "$f" | sed -n 's/.*PNG image data, \([0-9]*\) x \([0-9]*\).*/\1 \2/p'); [ -z "$d" ] && continue
  w=${d% *}; h=${d#* }; me=$(( w>h ? w : h ))
  [ $me -le 512 ] && continue
  n="${f%.png}"
  grep -rql "block/$n" ../../../../assets/mtrlte/models ../../../../assets/mtrlte/blockstates \
    && echo "ATLAS  ${w}x${h}  $f"
done | sort -rn

# 方速侧证据
javap -p -constants -cp <fangsu解包目录> com.fangsu.scripting.GifHelper
javap -p -c -cp <fangsu解包目录> com.fangsu.render.sowcer.batch.MaterialProp | grep -A40 getBlazeRenderType
javap -p -c -cp <fangsu解包目录> com.fangsu.blockEntities.client.BaseBlockEntityRender | grep -A12 'static {}'
```
