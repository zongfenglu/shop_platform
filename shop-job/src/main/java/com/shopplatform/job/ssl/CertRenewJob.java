package com.shopplatform.job.ssl;

import com.shopplatform.domain.ssl.SslCertificateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Let's Encrypt 到期续期。未启用 ACME 时只把已过期记录标 expired，不假装续签成功。
 */
@Component
public class CertRenewJob {

    private static final Logger log = LoggerFactory.getLogger(CertRenewJob.class);

    private final SslCertificateService sslCertificateService;

    public CertRenewJob(SslCertificateService sslCertificateService) {
        this.sslCertificateService = sslCertificateService;
    }

    @Scheduled(cron = "${shop.job.cert-renew-cron:0 0 3 * * ?}")
    public void run() {
        int renewed = sslCertificateService.renewDue();
        log.info("证书续期结束 renewed={}", renewed);
    }
}
