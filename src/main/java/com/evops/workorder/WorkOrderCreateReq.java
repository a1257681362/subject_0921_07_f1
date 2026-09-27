package com.evops.workorder;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

public class WorkOrderCreateReq {
    @NotBlank(message = "请求号不能为空")
    @Size(max = 64, message = "请求号过长")
    private String requestNo;

    @NotBlank(message = "作业单号不能为空")
    @Size(max = 48, message = "作业单号过长")
    private String orderNo;

    @NotBlank(message = "农机编号不能为空")
    @Size(max = 32, message = "农机编号过长")
    private String machineNo;

    @NotBlank(message = "作业类型不能为空")
    @Size(max = 32, message = "作业类型过长")
    private String workType;

    /** 人工上报面积，仅作参考，可空 */
    @DecimalMin(value = "0", message = "上报面积不能为负")
    private BigDecimal reportedArea;

    public String getRequestNo() { return requestNo; }
    public void setRequestNo(String requestNo) { this.requestNo = requestNo; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getMachineNo() { return machineNo; }
    public void setMachineNo(String machineNo) { this.machineNo = machineNo; }
    public String getWorkType() { return workType; }
    public void setWorkType(String workType) { this.workType = workType; }
    public BigDecimal getReportedArea() { return reportedArea; }
    public void setReportedArea(BigDecimal reportedArea) { this.reportedArea = reportedArea; }
}
