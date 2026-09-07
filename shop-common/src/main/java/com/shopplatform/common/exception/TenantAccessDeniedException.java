package com.shopplatform.common.exception;

import com.shopplatform.common.result.ErrorCode;

/**
 * 租户越权异常：当前请求尝试访问不属于自己租户的数据。
 * 全局异常处理器应将其映射为 403/404，绝不能静默放行或返回 200。
 * 见文档三 §2.4 越权防御。
 */
public class TenantAccessDeniedException extends BusinessException {

    public TenantAccessDeniedException() {
        super(ErrorCode.FORBIDDEN, "无权限访问该资源");
    }

    public TenantAccessDeniedException(String resourceDesc) {
        super(ErrorCode.FORBIDDEN, "无权限访问：" + resourceDesc);
    }
}
