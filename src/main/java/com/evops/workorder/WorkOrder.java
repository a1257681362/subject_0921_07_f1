package com.evops.workorder;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.evops.common.BaseEntity;

import java.math.BigDecimal;

/**
 * 作业单。不物理删除；冲销另开 docType=REVERSAL 的反向单据（面积取负抵消原单）。
 */
@TableName("t_work_order")
public class WorkOrder extends BaseEntity {
    private String orderNo;
    /** NORMAL / REVERSAL */
    private String docType;
    /** 反向单据指向的原单号 */
    private String refOrderNo;
    private String machineNo;
    private String workType;
    /** PENDING / IN_PROGRESS / COMPLETED / REVERSED */
    private String status;
    /** 人工上报面积，仅作参考 */
    private BigDecimal reportedArea;
    /** 轨迹核算面积，结算以此为准 */
    private BigDecimal trackArea;
    private String reverseReason;
    @Version
    private Integer version;

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getDocType() { return docType; }
    public void setDocType(String docType) { this.docType = docType; }
    public String getRefOrderNo() { return refOrderNo; }
    public void setRefOrderNo(String refOrderNo) { this.refOrderNo = refOrderNo; }
    public String getMachineNo() { return machineNo; }
    public void setMachineNo(String machineNo) { this.machineNo = machineNo; }
    public String getWorkType() { return workType; }
    public void setWorkType(String workType) { this.workType = workType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getReportedArea() { return reportedArea; }
    public void setReportedArea(BigDecimal reportedArea) { this.reportedArea = reportedArea; }
    public BigDecimal getTrackArea() { return trackArea; }
    public void setTrackArea(BigDecimal trackArea) { this.trackArea = trackArea; }
    public String getReverseReason() { return reverseReason; }
    public void setReverseReason(String reverseReason) { this.reverseReason = reverseReason; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
