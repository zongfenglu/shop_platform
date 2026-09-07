package com.shopplatform.domain.ops;

public interface CacheOpsService {

    Snapshot snapshot();

    long flush(String scope, Long shopId);

    record Snapshot(long tenantKeyCount, String usedMemory, String maxMemory, String hitRate) {
    }
}
