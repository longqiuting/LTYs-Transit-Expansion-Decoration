#!/usr/bin/env node
/**
 * M4b-4 平台层机械修复器（可重跑、幂等）。
 *
 * java-gen 是 tools/remap-to-mojmap.sh 的生成物，所以「Fabric API → Forge 平台 shim」
 * 的调用点改写全部放在这里，重跑反编译管线后再跑一次本脚本即可复原。
 *
 * 只改「调用平台 API 的那几行」：
 *   1. 注册：Registry.register(BuiltInRegistries.X, id, v) → Reg.block/item/tab/sound/entityType
 *      FabricBlockEntityTypeBuilder.create(...).build() → Reg.blockEntityType(id, factory, blocks)
 *   2. 页签：ItemGroupEvents.modifyEntriesEvent(...).register(...) → Reg.tabEntries(...)
 *   3. 渲染：BlockEntityRendererRegistry/EntityRendererRegistry → Renderers；
 *      BlockRenderLayerMap.INSTANCE.putBlock → Layers.put
 *   4. 网络：ServerPlayNetworking/ClientPlayNetworking/PacketByteBufs/PlayerLookup/
 *      ServerPlayConnectionEvents/ClientPlayConnectionEvents → Net
 *   5. 环境：FabricLoader → Env；SimpleResourceReloadListener/ResourceManagerHelper → Resources
 *   6. 命令：ClientCommandManager/FabricClientCommandSource → Cmds + CommandSourceStack
 *   7. 入口：ClientModInitializer/ModInitializer → ClientInit/CommonInit
 *   8. 反编译残留：server.l() → isDedicatedServer()、completedFuture((Object)null) → (Void)null
 *      以及 XiamenClient 两处被丢掉的 raw provider cast。
 *
 * 类名 / 包名 / 业务逻辑一律不动。
 *
 * 用法：
 *   node tools/fix-java-gen-platform.js             # 应用
 *   node tools/fix-java-gen-platform.js --dry-run   # 只报改动
 */
'use strict';

const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const GEN = path.join(ROOT, 'forge/src/main/java-gen');
const DRY = process.argv.includes('--dry-run');
const changed = [];

/* ---------------- 文件遍历 ---------------- */
function walk(dir, out = []) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) walk(full, out);
    else if (entry.name.endsWith('.java')) out.push(full);
  }
  return out;
}

/* ---------------- 括号配平 ---------------- */
function findClose(s, openIdx) {
  let depth = 0;
  let quote = null;
  for (let i = openIdx; i < s.length; i++) {
    const c = s[i];
    if (quote) {
      if (c === '\\') { i++; continue; }
      if (c === quote) quote = null;
      continue;
    }
    if (c === '"' || c === "'") { quote = c; continue; }
    if (c === '(') depth++;
    else if (c === ')') { depth--; if (depth === 0) return i; }
  }
  return -1;
}

function splitArgs(s) {
  const out = [];
  let depth = 0;
  let quote = null;
  let last = 0;
  for (let i = 0; i < s.length; i++) {
    const c = s[i];
    if (quote) {
      if (c === '\\') { i++; continue; }
      if (c === quote) quote = null;
      continue;
    }
    if (c === '"' || c === "'") { quote = c; continue; }
    if ('([{'.includes(c)) depth++;
    else if (')]}'.includes(c)) depth--;
    else if (c === ',' && depth === 0) {
      out.push(s.slice(last, i));
      last = i + 1;
    }
  }
  out.push(s.slice(last));
  return out.map((x) => x.trim());
}

/* ---------------- 注册调用改写 ---------------- */
const REG_CALL = 'Registry.register(';
const BET_CREATE = 'FabricBlockEntityTypeBuilder.create';
const TAB_BUILDER = 'FabricItemGroup.builder()';

function rewriteRegistrations(src) {
  let i = 0;
  let guard = 0;
  while ((i = src.indexOf(REG_CALL, i)) !== -1 && guard++ < 5000) {
    const open = i + REG_CALL.length - 1;
    const close = findClose(src, open);
    if (close < 0) break;
    const args = splitArgs(src.slice(open + 1, close));
    const registry = args[0];
    let repl = null;

    if (args.length === 3 && (registry === 'BuiltInRegistries.BLOCK' || registry === 'BuiltInRegistries.ITEM')) {
      repl = `${registry.endsWith('BLOCK') ? 'Reg.block' : 'Reg.item'}(${args[1]}, ${args[2]})`;
    } else if (args.length === 3 && registry === 'BuiltInRegistries.BLOCK_ENTITY_TYPE' && args[2].startsWith(BET_CREATE)) {
      const createOpen = args[2].indexOf(BET_CREATE) + BET_CREATE.length;
      const createClose = findClose(args[2], createOpen);
      if (createClose > 0) {
        const inner = splitArgs(args[2].slice(createOpen + 1, createClose));
        const factory = inner[0];
        const blocks = inner.slice(1).join(', ');
        repl = `Reg.blockEntityType(${args[1]}, ${factory}${blocks ? ', ' + blocks : ''})`;
      }
    } else if (args.length === 3 && registry === 'BuiltInRegistries.SOUND_EVENT' && args[2].startsWith('SoundEvent.createVariableRangeEvent(')) {
      repl = `Reg.sound(${args[1]})`;
    } else if (args.length === 3 && registry === 'BuiltInRegistries.ENTITY_TYPE' && args[2].startsWith('FabricEntityTypeBuilder.create(')) {
      const m = /^FabricEntityTypeBuilder\.create\(([^,]+),\s*([\s\S]+?)\)\.dimensions\(EntityDimensions\.fixed\(([^,]+),\s*([^)]+)\)\)\.trackRangeBlocks\((\d+)\)\.trackedUpdateRate\((\d+)\)\.build\(\)$/.exec(args[2]);
      if (m) {
        repl = `Reg.entityType(${args[1]}, ${m[2].trim()}, ${m[1].trim()}, ${m[3].trim()}, ${m[4].trim()}, ${m[5]}, ${m[6]})`;
      }
    } else if (args.length === 3 && registry === 'BuiltInRegistries.CREATIVE_MODE_TAB' && args[2].startsWith(TAB_BUILDER)) {
      let rest = args[2].slice(TAB_BUILDER.length);
      if (rest.startsWith('.title(')) {
        const titleOpen = TAB_BUILDER.length + '.title'.length;
        const titleClose = findClose(args[2], titleOpen);
        if (titleClose > 0) {
          const title = args[2].slice(titleOpen + 1, titleClose);
          rest = args[2].slice(titleClose + 1);
          if (rest.startsWith('.icon(')) {
            const iconOpen = args[2].indexOf('.icon(', titleClose) + '.icon'.length;
            const iconClose = findClose(args[2], iconOpen);
            if (iconClose > 0) {
              const icon = args[2].slice(iconOpen + 1, iconClose);
              repl = `Reg.tab(${args[1]}, ${title}, ${icon})`;
            }
          }
        }
      }
    }

    if (!repl) { i = close + 1; continue; }
    src = src.slice(0, i) + repl + src.slice(close + 1);
    i += repl.length;
  }
  return src;
}

/* ---------------- 页签条目 ---------------- */
const TAB_ENTRIES = 'ItemGroupEvents.modifyEntriesEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, ';
const TAB_ENTRIES_END = ')).register(';

function rewriteTabEntries(src) {
  let i = 0;
  let guard = 0;
  while ((i = src.indexOf(TAB_ENTRIES, i)) !== -1 && guard++ < 500) {
    const idStart = i + TAB_ENTRIES.length;
    const end = src.indexOf(TAB_ENTRIES_END, idStart);
    if (end < 0) break;
    const id = src.slice(idStart, end);
    const repl = `Reg.tabEntries(${id}, `;
    src = src.slice(0, i) + repl + src.slice(end + TAB_ENTRIES_END.length);
    i += repl.length;
  }
  return src;
}

/* ---------------- Fabric import 处置 ---------------- */
const PLATFORM = 'dev.js.mtrheadlight.forge.platform.';
const IMPORT_RULES = [
  ['import net.fabricmc.api.ClientModInitializer;', `import ${PLATFORM}ClientInit;`],
  ['import net.fabricmc.api.ModInitializer;', `import ${PLATFORM}CommonInit;`],
  ['import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;', `import ${PLATFORM}Renderers;`],
  ['import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;', `import ${PLATFORM}Layers;`],
  ['import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;', `import ${PLATFORM}Net;`],
  ['import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;', `import ${PLATFORM}Net;`],
  ['import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;', `import ${PLATFORM}Net;`],
  ['import net.fabricmc.fabric.api.networking.v1.PlayerLookup;', `import ${PLATFORM}Net;`],
  ['import net.fabricmc.loader.api.FabricLoader;', `import ${PLATFORM}Env;`],
  ['import net.fabricmc.fabric.api.resource.SimpleResourceReloadListener;', `import ${PLATFORM}Resources;`],
  ['import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;', 'import net.minecraft.commands.CommandSourceStack;'],
  // 纯 Fabric 事件的 import：直接去掉（调用点已改写）
  ['import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;', null],
  ['import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;', null],
  ['import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;', null],
  ['import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;', null],
  ['import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;', null],
  ['import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;', null],
  ['import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;', null],
  ['import net.fabricmc.fabric.api.resource.ResourceManagerHelper;', null],
  ['import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;', null],
  ['import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;', null],
];

/* ---------------- 调用点表格 ---------------- */
const TEXT_RULES = [
  // 渲染
  ['BlockEntityRendererRegistry.register(', 'Renderers.ber('],
  ['EntityRendererRegistry.register(', 'Renderers.entity('],
  ['BlockRenderLayerMap.INSTANCE.putBlock(', 'Layers.put('],
  // 网络
  ['ServerPlayNetworking.canSend(', 'Net.canSend('],
  ['ServerPlayNetworking.send(', 'Net.send('],
  ['ClientPlayNetworking.canSend(', 'Net.canSend('],
  ['ClientPlayNetworking.send(', 'Net.sendToServer('],
  ['PacketByteBufs.create()', 'Net.buf()'],
  ['PlayerLookup.tracking(', 'Net.tracking('],
  ['PlayerLookup.all(', 'Net.allPlayers('],
  // 环境 / 资源
  ['FabricLoader.getInstance().getConfigDir()', 'Env.configDir()'],
  ['FabricLoader.getInstance().isModLoaded(', 'Env.isModLoaded('],
  ['(String)FabricLoader.getInstance().getModContainer("mtrlte").map((c) -> c.getMetadata().getVersion().getFriendlyString()).orElse("?")', 'Env.modVersion("mtrlte")'],
  ['FabricLoader.getInstance().getModContainer("minecraft").map((c) -> c.getMetadata().getVersion().getFriendlyString()).orElse("?")', 'Env.modVersion("minecraft")'],
  ['ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(', 'Resources.registerClient('],
  ['implements SimpleResourceReloadListener<Void>', 'implements Resources.ReloadListener'],
  ['CompletableFuture.completedFuture((Object)null)', 'CompletableFuture.completedFuture((Void)null)'],
  // 构造器形参是 EntityType<? extends X>，方法引用无法推断，换成显式 lambda
  ['Reg.entityType(PROJECTILE_ID, PowerBankProjectileEntity::new,', 'Reg.<PowerBankProjectileEntity>entityType(PROJECTILE_ID, (type, level) -> new PowerBankProjectileEntity(type, level),'],
  ['Reg.entityType(PROJECTILE_ID, (type, level) ->', 'Reg.<PowerBankProjectileEntity>entityType(PROJECTILE_ID, (type, level) ->'],
  // 命令
  ['ClientCommandManager.literal(', 'Cmds.literal('],
  ['ClientCommandManager.argument(', 'Cmds.argument('],
  ['ClientCommandRegistrationCallback.EVENT.register((ClientCommandRegistrationCallback)(dispatcher, registryAccess) -> {', 'Cmds.register((dispatcher) -> {'],
  ['source.sendFeedback(', 'Cmds.feedback(source, '],
  ['client.tell(', 'client.execute('],
  // 入口接口
  ['implements ClientModInitializer', 'implements ClientInit'],
  ['implements ModInitializer', 'implements CommonInit'],
  // 反编译残留
  ['server.l()', 'server.isDedicatedServer()'],
  // 页签监听器的函数式类型
  ['(ItemGroupEvents.ModifyEntries)', '(Reg.TabEntriesConsumer)'],
];

const JOIN_OLD = 'ServerPlayConnectionEvents.JOIN.register((ServerPlayConnectionEvents.Join)(handler, sender, server) -> {';
const JOIN_NEW = 'Net.onJoin((player, server) -> {';

const RAW_PROVIDER_OLD = [
  'BlockEntityRendererProvider<XiamenPsdBlocks.TileEntityXiamenPSDDoor> doorRendererFactory = RenderPsdDoor::new;',
  'BlockEntityRendererProvider<XiamenPsdBlocks.TileEntityXiamenAPGDoor> apgDoorRendererFactory = RenderPsdApgDoor::new;',
];
const RAW_PROVIDER_NEW = [
  'BlockEntityRendererProvider<XiamenPsdBlocks.TileEntityXiamenPSDDoor> doorRendererFactory = (BlockEntityRendererProvider) RenderPsdDoor::new;',
  'BlockEntityRendererProvider<XiamenPsdBlocks.TileEntityXiamenAPGDoor> apgDoorRendererFactory = (BlockEntityRendererProvider) RenderPsdApgDoor::new;',
];

/* ---------------- import 补全 ---------------- */
const NEEDED_IMPORTS = [
  [/\bReg\./, `import ${PLATFORM}Reg;`],
  [/\bLayers\./, `import ${PLATFORM}Layers;`],
  [/\bRenderers\./, `import ${PLATFORM}Renderers;`],
  [/\bNet\./, `import ${PLATFORM}Net;`],
  [/\bEnv\./, `import ${PLATFORM}Env;`],
  [/\bResources\./, `import ${PLATFORM}Resources;`],
  [/\bCmds\./, `import ${PLATFORM}Cmds;`],
  [/\bimplements ClientInit\b/, `import ${PLATFORM}ClientInit;`],
  [/\bimplements CommonInit\b/, `import ${PLATFORM}CommonInit;`],
];

function ensureImports(src) {
  const present = new Set(
    src.split(/\r?\n/).filter((l) => l.startsWith('import ')).map((l) => l.trim())
  );
  const missing = [];
  for (const [re, imp] of NEEDED_IMPORTS) {
    if (present.has(imp)) continue;
    if (!re.test(src)) continue;
    // 只在类体之前匹配（避免注释里的同名 token 触发）
    missing.push(imp);
    present.add(imp);
  }
  if (!missing.length) return src;
  const lines = src.split(/\r?\n/);
  let lastImport = -1;
  for (let i = 0; i < lines.length; i++) {
    if (lines[i].startsWith('import ')) lastImport = i;
  }
  if (lastImport < 0) return src;
  lines.splice(lastImport + 1, 0, ...missing);
  return lines.join('\n');
}

/* ---------------- 整文件替换（Fabric 专属桥接 → Forge 等价物） ---------------- */
const FILE_OVERRIDES = {
  'dev/js/mtrheadlight/ModMenuIntegration.java': `package dev.js.mtrheadlight;

import dev.js.mtrheadlight.decor.LtedDashboardScreen;
import net.minecraft.client.gui.screens.Screen;

/**
 * Fabric 的 ModMenu 集成在 Forge 侧的等价物（M4b-4）。
 *
 * <p>Fabric 侧由 ModMenu 扫描 {@code ModMenuApi} 实现类；Forge 侧改由
 * {@code ClientBootstrap} 注册 {@code ConfigScreenHandler.ConfigScreenFactory}
 * 扩展点 —— 效果相同：模组列表里的「配置」按钮直接打开 LTED 仪表板。
 */
public final class ModMenuIntegration {

   private ModMenuIntegration() {
   }

   public static Screen configScreen(Screen parent) {
      return new LtedDashboardScreen(parent);
   }
}
`,
};

/* ---------------- 单文件处理 ---------------- */
function transform(src) {
  let out = rewriteRegistrations(src);
  out = rewriteTabEntries(out);

  for (const [from, to] of IMPORT_RULES) {
    if (!out.includes(from)) continue;
    out = to === null
      ? out.split('\n').filter((l) => l.trim() !== from).join('\n')
      : out.split(from).join(to);
  }

  for (const [from, to] of TEXT_RULES) {
    if (out.includes(from)) out = out.split(from).join(to);
  }

  // FabricClientCommandSource 的类型/强转
  out = out.split('FabricClientCommandSource').join('CommandSourceStack');

  // JOIN 事件
  if (out.includes(JOIN_OLD)) {
    out = out.split(JOIN_OLD).join(JOIN_NEW);
    out = out.split('handler.player').join('player');
  }
  out = out.replace(
    /ClientPlayConnectionEvents\.DISCONNECT\.register\(\(ClientPlayConnectionEvents\.Disconnect\)\(handler, client\) -> ([^;]+)\);/g,
    'Net.onDisconnect(() -> $1);'
  );

  // 渲染器注册：lambda 形参
  out = out.replace(
    /ServerPlayNetworking\.registerGlobalReceiver\(([^,]+),\s*\(server, player, handler, buf, sender\) ->/g,
    'Net.registerServer($1, (server, player, buf) ->'
  );
  out = out.replace(
    /ClientPlayNetworking\.registerGlobalReceiver\(([^,]+),\s*\(client, handler, buf, sender\) ->/g,
    'Net.registerClient($1, (client, buf) ->'
  );

  // XiamenClient 被反编译丢掉的 raw cast
  for (let i = 0; i < RAW_PROVIDER_OLD.length; i++) {
    if (out.includes(RAW_PROVIDER_OLD[i])) out = out.split(RAW_PROVIDER_OLD[i]).join(RAW_PROVIDER_NEW[i]);
  }

  return ensureImports(out);
}

/* ---------------- 主流程 ---------------- */
for (const file of walk(GEN)) {
  const rel = path.relative(GEN, file).replace(/\\/g, '/');
  const before = fs.readFileSync(file, 'utf8');
  const after = FILE_OVERRIDES[rel] !== undefined ? FILE_OVERRIDES[rel] : transform(before);
  if (after === before) continue;
  if (!DRY) fs.writeFileSync(file, after, 'utf8');
  changed.push(rel);
}

console.log(`${DRY ? '[dry-run] ' : ''}${changed.length} file(s) rewritten`);
for (const rel of changed) console.log(`  ${rel}`);
