// 能被「售票机配置面板」编辑的方块实体都实现它。
package dev.js.mtrheadlight.machine;

/** 售票机配置的读写接口：面板打开时读一次，保存时写回。 */
public interface MachineConfigurable {

    /** 当前配置（含这台款式支持的项）。 */
    MachineConfig machineConfig();

    /** 面板点了「完成」：把配置写进来（服务端调用）。 */
    void applyMachineConfig(MachineConfig config);
}
