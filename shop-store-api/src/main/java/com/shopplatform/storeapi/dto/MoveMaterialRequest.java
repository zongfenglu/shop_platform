package com.shopplatform.storeapi.dto;

/** groupId 为空表示移到未分组。 */
public record MoveMaterialRequest(Long groupId) {
}
