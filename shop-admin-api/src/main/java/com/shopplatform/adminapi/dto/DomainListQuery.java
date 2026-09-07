package com.shopplatform.adminapi.dto;

/**
 * 平台域名列表查询条件。
 */
public record DomainListQuery(
        String keyword,
        String type,
        String verifyStatus,
        Integer pageNum,
        Integer pageSize
) {
    public int pageNumOrDefault() {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    public int pageSizeOrDefault() {
        return pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100);
    }
}
