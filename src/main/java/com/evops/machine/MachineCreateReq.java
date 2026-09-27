package com.evops.machine;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class MachineCreateReq {
    @NotBlank(message = "请求号不能为空")
    @Size(max = 64, message = "请求号过长")
    private String requestNo;

    @NotBlank(message = "农机编号不能为空")
    @Size(max = 32, message = "农机编号过长")
    private String machineNo;

    @NotBlank(message = "农机名称不能为空")
    @Size(max = 64, message = "农机名称过长")
    private String machineName;

    @NotBlank(message = "农机类型不能为空")
    @Size(max = 32, message = "农机类型过长")
    private String machineType;

    public String getRequestNo() { return requestNo; }
    public void setRequestNo(String requestNo) { this.requestNo = requestNo; }
    public String getMachineNo() { return machineNo; }
    public void setMachineNo(String machineNo) { this.machineNo = machineNo; }
    public String getMachineName() { return machineName; }
    public void setMachineName(String machineName) { this.machineName = machineName; }
    public String getMachineType() { return machineType; }
    public void setMachineType(String machineType) { this.machineType = machineType; }
}
