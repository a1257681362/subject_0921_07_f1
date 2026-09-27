package com.evops.workorder;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

public class WorkOrderCompleteReq {
    @NotBlank(message = "请求号不能为空")
    @Size(max = 64, message = "请求号过长")
    private String requestNo;

    /** 轨迹核算面积，完工面积以此为准 */
    @NotNull(message = "轨迹核算面积不能为空")
    @DecimalMin(value = "0", inclusive = false, message = "轨迹核算面积必须大于0")
    private BigDecimal trackArea;

    public String getRequestNo() { return requestNo; }
    public void setRequestNo(String requestNo) { this.requestNo = requestNo; }
    public BigDecimal getTrackArea() { return trackArea; }
    public void setTrackArea(BigDecimal trackArea) { this.trackArea = trackArea; }
}
