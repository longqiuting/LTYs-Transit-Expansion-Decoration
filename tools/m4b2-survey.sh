#!/usr/bin/env bash
# tools/m4b2-survey.sh —— 统计 java-gen 基线里需要人工改造的类别与数量（M4b-2 报告用）
set -uo pipefail
mp() { cygpath -m "$1" 2>/dev/null || printf '%s' "$1"; }
R="$(mp "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)")"
GEN="$R/forge/src/main/java-gen"
JAVAP="C:/Program Files/Java/jdk-17/bin/javap"

c() { grep -rl "$1" "$GEN" --include='*.java' 2>/dev/null | wc -l | tr -d ' '; }
n() { grep -rho "$1" "$GEN" --include='*.java' 2>/dev/null | wc -l | tr -d ' '; }

echo "## 规模"
echo "java 文件            : $(find "$GEN" -name '*.java' | wc -l | tr -d ' ')"
echo "java 行数            : $(cat $(find "$GEN" -name '*.java') | wc -l | tr -d ' ')"
echo
echo "## Fabric 平台层（需整体替换成 Forge）"
echo "import net.fabricmc.* 文件        : $(c 'import net.fabricmc')"
echo "  FabricBlockEntityTypeBuilder    : $(n 'FabricBlockEntityTypeBuilder') 次 / $(c 'FabricBlockEntityTypeBuilder') 文件"
echo "  FabricItemGroup / ItemGroupEvents: $(n 'FabricItemGroup\|ItemGroupEvents') 次 / $(c 'FabricItemGroup\|ItemGroupEvents') 文件"
echo "  Registry.register               : $(n 'Registry\.register') 次 / $(c 'Registry\.register') 文件"
echo "  ServerPlayNetworking/ClientPlayNetworking: $(n 'ServerPlayNetworking\|ClientPlayNetworking') 次 / $(c 'ServerPlayNetworking\|ClientPlayNetworking') 文件"
echo "  PacketByteBuf                   : $(n 'PacketByteBuf') 次 / $(c 'PacketByteBuf') 文件"
echo "  ClientTickEvents/ServerTickEvents: $(n 'ClientTickEvents\|ServerTickEvents') 次 / $(c 'ClientTickEvents\|ServerTickEvents') 文件"
echo "  ClientLifecycleEvents/ModInitializer: $(n 'ModInitializer\|ClientModInitializer') 次 / $(c 'ModInitializer\|ClientModInitializer') 文件"
echo "  BlockEntityRendererFactories    : $(n 'BlockEntityRendererFactories') 次 / $(c 'BlockEntityRendererFactories') 文件"
echo "  EntityRendererRegistry          : $(n 'EntityRendererRegistry') 次 / $(c 'EntityRendererRegistry') 文件"
echo "  KeyBindingHelper / KeyBinding   : $(n 'KeyBindingHelper') 次 / $(c 'KeyBindingHelper') 文件"
echo "  ResourceManagerHelper / 内置资源包: $(n 'ResourceManagerHelper\|ModResourcePack') 次 / $(c 'ResourceManagerHelper\|ModResourcePack') 文件"
echo "  com.terraformersmc (ModMenu)    : $(n 'com\.terraformersmc') 次 / $(c 'com\.terraformersmc') 文件"
echo
echo "## Mixin"
echo "@Mixin 类            : $(c '@Mixin')"
echo "  remap = false      : $(n 'remap = false') 次"
echo "  @Inject/@Redirect/@ModifyArg/@ModifyVariable/@Overwrite: $(n '@Inject\|@Redirect\|@ModifyArg\|@ModifyVariable\|@Overwrite\|@InjectMixin') 次"
echo
echo "## MTR 抽象层"
echo "import mtr.mappings.* : $(c 'import mtr\.mappings') 文件"
grep -rho 'mtr\.mappings\.[A-Za-z0-9_]*' "$GEN" --include='*.java' 2>/dev/null | sort | uniq -c | sort -rn | sed 's/^/    /'
echo
echo "## 反编译产物里的已知重编失败类别（来源: tools/verify-decompiled.sh --all）"
echo "（见 docs/forge-m4b2-mojmap-baseline.md 表格）"
