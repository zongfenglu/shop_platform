package com.shopplatform.domain.ssl.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.ssl.AcmeIssuer;
import com.shopplatform.domain.ssl.AcmeProperties;
import com.shopplatform.domain.ssl.entity.AcmeAccount;
import com.shopplatform.domain.ssl.mapper.AcmeAccountMapper;
import com.shopplatform.framework.crypto.AesGcmEncryptor;
import org.shredzone.acme4j.Account;
import org.shredzone.acme4j.AccountBuilder;
import org.shredzone.acme4j.Authorization;
import org.shredzone.acme4j.Certificate;
import org.shredzone.acme4j.Order;
import org.shredzone.acme4j.Session;
import org.shredzone.acme4j.Status;
import org.shredzone.acme4j.challenge.Http01Challenge;
import org.shredzone.acme4j.util.KeyPairUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Let's Encrypt（或兼容 ACME）HTTP-01 签发。失败原样抛出，不写假证书。
 */
@Component
public class Acme4jIssuer extends ServiceImpl<AcmeAccountMapper, AcmeAccount> implements AcmeIssuer {

    private static final long ACCOUNT_ID = 1L;

    private final AcmeProperties properties;
    private final AesGcmEncryptor aesGcmEncryptor;

    public Acme4jIssuer(AcmeProperties properties, AesGcmEncryptor aesGcmEncryptor) {
        this.properties = properties;
        this.aesGcmEncryptor = aesGcmEncryptor;
    }

    @Override
    public IssuedCert issue(String domain, Http01Publisher publisher) {
        if (!StringUtils.hasText(domain)) {
            throw new BusinessException(ErrorCode.DOMAIN_INVALID);
        }
        List<String> tokens = new ArrayList<>();
        try {
            Session session = new Session(properties.directoryUrl());
            Account account = loadOrCreateAccount(session);
            Order order = account.newOrder().domain(domain).create();
            for (Authorization auth : order.getAuthorizations()) {
                if (auth.getStatus() == Status.VALID) {
                    continue;
                }
                Http01Challenge challenge = auth.findChallenge(Http01Challenge.class)
                        .orElseThrow(() -> new BusinessException(ErrorCode.ACME_ISSUE_FAILED, "CA 未提供 http-01 校验"));
                publisher.publish(challenge.getToken(), challenge.getAuthorization());
                tokens.add(challenge.getToken());
                challenge.trigger();
                waitAuthorization(auth);
            }
            KeyPair domainKey = KeyPairUtils.createKeyPair(2048);
            order.execute(domainKey);
            waitOrder(order);
            Certificate certificate = order.getCertificate();
            if (certificate == null) {
                throw new BusinessException(ErrorCode.ACME_ISSUE_FAILED, "CA 未返回证书");
            }
            StringWriter certOut = new StringWriter();
            certificate.writeCertificate(certOut);
            StringWriter keyOut = new StringWriter();
            KeyPairUtils.writeKeyPair(domainKey, keyOut);
            X509Certificate x509 = certificate.getCertificate();
            LocalDateTime notAfter = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(x509.getNotAfter().getTime()), ZoneId.systemDefault());
            String issuer = x509.getIssuerX500Principal() == null ? "" : x509.getIssuerX500Principal().getName();
            return new IssuedCert(certOut.toString(), keyOut.toString(), notAfter, issuer);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.ACME_ISSUE_FAILED, trim(e.getMessage()));
        } finally {
            for (String token : tokens) {
                publisher.clear(token);
            }
        }
    }

    private Account loadOrCreateAccount(Session session) throws Exception {
        AcmeAccount row = this.getOne(Wrappers.<AcmeAccount>lambdaQuery().eq(AcmeAccount::getId, ACCOUNT_ID));
        if (row == null) {
            row = new AcmeAccount();
            row.setId(ACCOUNT_ID);
        }
        boolean sameDir = properties.directoryUrl().equals(row.getDirectoryUrl());
        if (sameDir && StringUtils.hasText(row.getAccountUrl()) && StringUtils.hasText(row.getKeyPemEncrypted())) {
            KeyPair key = KeyPairUtils.readKeyPair(new StringReader(aesGcmEncryptor.decrypt(row.getKeyPemEncrypted())));
            return session.login(URI.create(row.getAccountUrl()).toURL(), key).getAccount();
        }
        KeyPair key = KeyPairUtils.createKeyPair(2048);
        AccountBuilder builder = new AccountBuilder()
                .agreeToTermsOfService()
                .useKeyPair(key)
                .addContact("mailto:" + properties.email());
        Account account = builder.create(session);
        URL location = account.getLocation();
        StringWriter keyOut = new StringWriter();
        KeyPairUtils.writeKeyPair(key, keyOut);
        row.setDirectoryUrl(properties.directoryUrl());
        row.setAccountUrl(location == null ? "" : location.toString());
        row.setContactEmail(properties.email());
        row.setKeyPemEncrypted(aesGcmEncryptor.encrypt(keyOut.toString()));
        this.saveOrUpdate(row);
        return account;
    }

    private static void waitAuthorization(Authorization auth) throws Exception {
        Instant deadline = Instant.now().plusSeconds(90);
        while (Instant.now().isBefore(deadline)) {
            auth.update();
            Status status = auth.getStatus();
            if (status == Status.VALID) {
                return;
            }
            if (status == Status.INVALID) {
                throw new BusinessException(ErrorCode.ACME_ISSUE_FAILED, "HTTP-01 校验未通过，请确认域名已 CNAME 到本平台且 80 端口可被 Let's Encrypt 访问");
            }
            Thread.sleep(3000);
        }
        throw new BusinessException(ErrorCode.ACME_ISSUE_FAILED, "HTTP-01 校验超时");
    }

    private static void waitOrder(Order order) throws Exception {
        Instant deadline = Instant.now().plusSeconds(90);
        while (Instant.now().isBefore(deadline)) {
            order.update();
            Status status = order.getStatus();
            if (status == Status.VALID) {
                return;
            }
            if (status == Status.INVALID) {
                throw new BusinessException(ErrorCode.ACME_ISSUE_FAILED, "证书订单被 CA 拒绝");
            }
            Thread.sleep(3000);
        }
        throw new BusinessException(ErrorCode.ACME_ISSUE_FAILED, "等待证书签发超时");
    }

    private static String trim(String msg) {
        if (msg == null || msg.isBlank()) {
            return "Let's Encrypt 接口调用失败";
        }
        return msg.length() > 400 ? msg.substring(0, 400) : msg;
    }
}
