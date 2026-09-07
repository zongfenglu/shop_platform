package com.shopplatform.adminapi.dto;

import java.time.LocalDateTime;

/**
 * 建店成功后的响应。对应原型 admin/shop-new.html 第5步"完成"页展示的字段。
 */
public record CreateShopResponse(
        Long shopId,
        String code,
        String name,
        String storeAdminUsername,
        LocalDateTime expireTime
) {
}
