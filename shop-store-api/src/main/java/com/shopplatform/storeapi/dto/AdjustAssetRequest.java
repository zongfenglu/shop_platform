package com.shopplatform.storeapi.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 后台调整会员余额/积分。amount 带正负号：正数增加，负数扣减。
 * 扣减后不可透支（由 MemberService 的原子更新兜底）。
 */
public record AdjustAssetRequest(
        @NotNull Long userId,
        @NotNull BigDecimal amount,
        String remark
) {
}
