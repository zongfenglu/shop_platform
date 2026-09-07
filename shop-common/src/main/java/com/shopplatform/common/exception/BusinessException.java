package com.shopplatform.common.exception;

import com.shopplatform.common.result.ErrorCode;

/**
 * 业务异常基类。所有可预期的业务失败（非系统故障）都应抛出此异常，
 * 由全局异常处理器统一转换为 {@link com.shopplatform.common.result.Result} 返回。
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMsg());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String customMsg) {
        super(customMsg);
        this.code = errorCode.getCode();
    }

    public int getCode() {
        return code;
    }
}
