package com.shopplatform.storeapi.dto;

public record MemberListQuery(
        String keyword,
        Integer status,
        Integer pageNum,
        Integer pageSize
) {
    public long pageNumOrDefault() {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    public long pageSizeOrDefault() {
        if (pageSize == null) return 20;
        return Math.min(Math.max(pageSize, 1), 100);
    }
}
