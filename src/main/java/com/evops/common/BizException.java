package com.evops.common;

/**
 * 业务异常：携带可对外展示的错误信息，由全局异常处理统一包装。
 */
public class BizException extends RuntimeException {
    public BizException(String message) {
        super(message);
    }
}
