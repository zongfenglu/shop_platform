package com.shopplatform.adminapi.dto;

public record OpsCacheFlushResult(String scope, Long shopId, long deleted) {
}
