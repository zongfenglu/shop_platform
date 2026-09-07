package com.shopplatform.clientapi.controller;

import com.shopplatform.domain.ssl.SslCertificateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Let's Encrypt HTTP-01。必须在 80 端口裸 HTTP 可达，且不走租户过滤器。
 */
@RestController
public class AcmeChallengeController {

    private final SslCertificateService sslCertificateService;

    public AcmeChallengeController(SslCertificateService sslCertificateService) {
        this.sslCertificateService = sslCertificateService;
    }

    @GetMapping(value = "/.well-known/acme-challenge/{token}", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> challenge(@PathVariable String token) {
        return sslCertificateService.http01Authorization(token)
                .map(body -> ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(body))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(""));
    }
}
