package com.shopplatform.domain.shop.dns;

import java.util.Optional;

/** 查询域名的 CNAME 记录。本地可用 mock；生产走 DNS。 */
public interface CnameLookup {

    Optional<String> lookupCname(String domain);
}
