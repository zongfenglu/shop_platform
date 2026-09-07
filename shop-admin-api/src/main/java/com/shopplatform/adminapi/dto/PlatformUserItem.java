package com.shopplatform.adminapi.dto;

import java.time.LocalDateTime;

/** 平台账号条目。 */
public record PlatformUserItem(
        Long id,
        String username,
        String realName,
        String mobile,
        String roleName,
        Long roleId,
        Integer status,
        LocalDateTime lastLoginTime,
        LocalDateTime createTime
) {
}
