package com.evops.machine;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.evops.common.BaseEntity;

/**
 * 农机底账。不物理删除，停用走 status=DISABLED。
 */
@TableName("t_machine")
public class Machine extends BaseEntity {
    private String machineNo;
    private String machineName;
    private String machineType;
    /** ACTIVE / DISABLED */
    private String status;
    @Version
    private Integer version;

    public String getMachineNo() { return machineNo; }
    public void setMachineNo(String machineNo) { this.machineNo = machineNo; }
    public String getMachineName() { return machineName; }
    public void setMachineName(String machineName) { this.machineName = machineName; }
    public String getMachineType() { return machineType; }
    public void setMachineType(String machineType) { this.machineType = machineType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
