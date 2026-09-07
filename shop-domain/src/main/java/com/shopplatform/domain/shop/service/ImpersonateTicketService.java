package com.shopplatform.domain.shop.service;

/**
 * 平台免密进入商户后台的一次性票据。见文档二 §1.2：5 分钟有效、单次使用。
 * 票据本身不带 JWT，兑换成功后由 store-api 签发 platformImpersonation=true 的会话。
 */
public interface ImpersonateTicketService {

    int TTL_SECONDS = 300;

    record Payload(Long shopId, Long storeUserId) {
    }

    String issue(Long shopId, Long storeUserId);

    /** 原子取出并删除。过期、已用过或伪造均返回 null。 */
    Payload consume(String ticket);
}
