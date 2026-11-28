// 终点站指示灯的方块实体：只用来挂 BER，本身不存任何数据、也不 tick。
package dev.js.mtrheadlight.region.hk;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

/**
 * 终点站指示灯的方块实体。
 *
 * <p><b>故意是空壳</b>：颜色完全由客户端从「这个站台正在进站的车」算出来（见
 * {@link DestinationColors}），不存任何状态 —— 所以它没有 NBT、没有 tick、也没有网络同步，
 * 一整个站台的灯带加起来也只是几百个空对象。
 */
public class HkDestinationLightBlockEntity extends BlockEntity {

    public HkDestinationLightBlockEntity(final BlockPos pos, final BlockState state) {
        super(HkDestinationLights.entityType(), pos, state);
    }
}
