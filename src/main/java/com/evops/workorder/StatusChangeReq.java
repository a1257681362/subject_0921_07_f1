package com.evops.workorder;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** 不带业务字段的状态变更请求（如开工），仅需请求号。 */
public class StatusChangeReq {
    @NotBlank(message = "请求号不能为空")
    @Size(max = 64, message = "请求号过长")
    private String requestNo;

    public String getRequestNo() { return requestNo; }
    public void setRequestNo(String requestNo) { this.requestNo = requestNo; }
}
