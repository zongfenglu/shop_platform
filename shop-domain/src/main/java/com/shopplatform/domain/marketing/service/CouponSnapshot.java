package com.shopplatform.domain.marketing.service;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

/**
 * 用户券快照（user_coupon.snapshot 解析后的视图）。领取时把券的关键信息冻结下来，
 * 券后续被运营改面额/门槛/范围不影响已发放的券——与充值方案快照同理。
 * <p>
 * 仅用于价格引擎 {@code CouponHandler} 与前端展示读取，不参与持久化。
 */
public record CouponSnapshot(
        String name,
        /** reduce / discount */
        String type,
        @JsonProperty("reducePrice") BigDecimal reducePrice,
        @JsonProperty("discountRatio") BigDecimal discountRatio,
        @JsonProperty("minPrice") BigDecimal minPrice,
        /** all / category / goods */
        @JsonProperty("applyRange") String applyRange,
        /** apply_range=category 时为分类 id，=goods 时为商品 id；all 时为空 */
        @JsonProperty("applyRangeConfig") List<Long> applyRangeConfig
) {
}
