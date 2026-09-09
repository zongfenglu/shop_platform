package com.shopplatform.domain.ssl;

import com.shopplatform.domain.shop.entity.ShopDomain;

import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;

public interface SslCertificateService {

    /** 审核通过后尝试签发：未启用或本地域名只记原因，不把 cert_status 标成 valid。 */
    void issueAfterApprove(ShopDomain domain);

    /** 超管手动重试。未启用或失败抛业务异常。 */
    ShopDomain issueNow(Long domainId);

    ShopDomain upload(Long domainId, MultipartFile certificate, MultipartFile privateKey);

    /** 到期 30 天内续期；并把已过期记录标 expired。 */
    int renewDue();

    Optional<String> http01Authorization(String token);
}
