package com.shopplatform.storeapi.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record SaveFullReduceRuleRequest(
        @NotBlank String name,
        /** money / count */
        @NotBlank String type,
        /** money: [{"threshold":100.00,"reduce":10.00}]；count: [{"threshold":2,"discount":0.90}] */
        List<Tier> rules,
        Integer freeExpress,
        /** on / off */
        String status,
        Integer sort
) {
    public record Tier(java.math.BigDecimal threshold, java.math.BigDecimal reduce, java.math.BigDecimal discount) {
    }
}
