package com.evops.machine;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class MachineUpdateReq {
    @NotBlank(message = "请求号不能为空")
    @Size(max = 64, message = "请求号过长")
    private String requestNo;

    @Size(max = 64, message = "农机名称过长")
    private String machineName;

    @Size(max = 32, message = "农机类型过长")
    private String machineType;

    /** 仅允许 ACTIVE / DISABLED，置空表示不变更。 */
    @Size(max = 16, message = "状态值过长")
    private String status;

    public String getRequestNo() { return requestNo; }
    public void setRequestNo(String requestNo) { this.requestNo = requestNo; }
    public String getMachineName() { return machineName; }
    public void setMachineName(String machineName) { this.machineName = machineName; }
    public String getMachineType() { return machineType; }
    public void setMachineType(String machineType) { this.machineType = machineType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
