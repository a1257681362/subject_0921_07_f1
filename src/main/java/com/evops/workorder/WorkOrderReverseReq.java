package com.evops.workorder;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class WorkOrderReverseReq {
    @NotBlank(message = "请求号不能为空")
    @Size(max = 64, message = "请求号过长")
    private String requestNo;

    @Size(max = 255, message = "冲销原因过长")
    private String reason;

    public String getRequestNo() { return requestNo; }
    public void setRequestNo(String requestNo) { this.requestNo = requestNo; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
