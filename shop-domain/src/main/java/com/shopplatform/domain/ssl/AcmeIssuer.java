package com.shopplatform.domain.ssl;

import java.time.LocalDateTime;

/**
 * 向 ACME CA 申请一张域名证书。实现里必须走真实 HTTP-01，不允许本地伪造签发成功。
 */
public interface AcmeIssuer {

    IssuedCert issue(String domain, Http01Publisher publisher);

    record IssuedCert(String certPem, String keyPem, LocalDateTime notAfter, String issuer) {
    }

    interface Http01Publisher {
        void publish(String token, String authorization);

        void clear(String token);
    }
}
