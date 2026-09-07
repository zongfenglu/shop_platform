package com.shopplatform.domain.pay.gateway;

import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.pay.service.ShopPayConfigService.DecryptedPayConfig;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.h5.H5Service;
import com.wechat.pay.java.service.payments.h5.model.Amount;
import com.wechat.pay.java.service.payments.h5.model.H5Info;
import com.wechat.pay.java.service.payments.h5.model.PrepayRequest;
import com.wechat.pay.java.service.payments.h5.model.QueryOrderByOutTradeNoRequest;
import com.wechat.pay.java.service.payments.h5.model.SceneInfo;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.refund.RefundService;
import com.wechat.pay.java.service.refund.model.AmountReq;
import com.wechat.pay.java.service.refund.model.CreateRequest;
import com.wechat.pay.java.service.refund.model.Refund;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 微信支付v3网关封装（H5支付场景）。见文档三 §9："租户自有商户号"——每个商户的 {@link RSAAutoCertificateConfig}
 * 独立缓存，互不影响；该 Config 构建时会向微信服务器发起真实网络请求下载平台证书，
 * 因此没有真实商户号/证书配置的开发环境里这一步必然失败——这是外部依赖限制，不是代码缺陷，
 * 与 Testcontainers 在本机 Docker Desktop 上的 npipe 限制是同一类问题（见 CONTRIBUTING.md）。
 */
@Component
public class WxPayGateway {

    private static final Logger log = LoggerFactory.getLogger(WxPayGateway.class);

    private final ConcurrentHashMap<String, RSAAutoCertificateConfig> configCache = new ConcurrentHashMap<>();

    public String createH5Prepay(DecryptedPayConfig payConfig, String outTradeNo, BigDecimal payPrice,
                                  String description, String notifyUrl, String clientIp) {
        H5Service service = new H5Service.Builder().config(resolveConfig(payConfig)).build();

        PrepayRequest request = new PrepayRequest();
        request.setAppid(payConfig.appId());
        request.setMchid(payConfig.mchId());
        request.setDescription(description);
        request.setOutTradeNo(outTradeNo);
        request.setNotifyUrl(notifyUrl);

        Amount amount = new Amount();
        amount.setTotal(yuanToFen(payPrice));
        request.setAmount(amount);

        H5Info h5Info = new H5Info();
        h5Info.setType("Wap");
        SceneInfo sceneInfo = new SceneInfo();
        sceneInfo.setPayerClientIp(clientIp);
        sceneInfo.setH5Info(h5Info);
        request.setSceneInfo(sceneInfo);

        try {
            return service.prepay(request).getH5Url();
        } catch (Exception e) {
            log.error("微信支付下单失败 outTradeNo={}", outTradeNo, e);
            throw new BusinessException(ErrorCode.PAY_CHANNEL_ERROR, "支付渠道异常，请稍后重试");
        }
    }

    public Transaction queryOrder(DecryptedPayConfig payConfig, String outTradeNo) {
        H5Service service = new H5Service.Builder().config(resolveConfig(payConfig)).build();
        QueryOrderByOutTradeNoRequest request = new QueryOrderByOutTradeNoRequest();
        request.setMchid(payConfig.mchId());
        request.setOutTradeNo(outTradeNo);
        try {
            return service.queryOrderByOutTradeNo(request);
        } catch (Exception e) {
            log.error("微信支付主动查单失败 outTradeNo={}", outTradeNo, e);
            throw new BusinessException(ErrorCode.PAY_CHANNEL_ERROR, "支付渠道异常，请稍后重试");
        }
    }

    /**
     * 退款申请。{@code totalFen} 必须是发起支付时的原始总金额（分），微信退款接口要求金额一致性校验，
     * 传错 total 会导致退款请求被拒——调用方必须传对应订单的 payPrice，不能只传本次退款金额。
     */
    public Refund createRefund(DecryptedPayConfig payConfig, String outTradeNo, String outRefundNo,
                                long refundFen, long totalFen, String reason) {
        RefundService service = new RefundService.Builder().config(resolveConfig(payConfig)).build();
        CreateRequest request = new CreateRequest();
        request.setOutTradeNo(outTradeNo);
        request.setOutRefundNo(outRefundNo);
        request.setReason(reason);
        AmountReq amount = new AmountReq();
        amount.setRefund(refundFen);
        amount.setTotal(totalFen);
        amount.setCurrency("CNY");
        request.setAmount(amount);
        try {
            return service.create(request);
        } catch (Exception e) {
            log.error("微信支付退款申请失败 outTradeNo={} outRefundNo={}", outTradeNo, outRefundNo, e);
            throw new BusinessException(ErrorCode.PAY_CHANNEL_ERROR, "退款渠道异常，请稍后重试");
        }
    }

    /** 验签 + AEAD解密回调报文，返回值已经是明文 Transaction，Controller/Service 不需要接触密文细节。 */
    public Transaction parseNotification(DecryptedPayConfig payConfig, RequestParam requestParam) {
        NotificationParser parser = new NotificationParser(resolveConfig(payConfig));
        try {
            return parser.parse(requestParam, Transaction.class);
        } catch (Exception e) {
            log.error("微信支付回调验签/解密失败", e);
            throw new BusinessException(ErrorCode.PAY_CALLBACK_INVALID_SIGN, "回调验签失败");
        }
    }

    private RSAAutoCertificateConfig resolveConfig(DecryptedPayConfig payConfig) {
        String cacheKey = payConfig.shopId() + ":" + payConfig.mchId();
        return configCache.computeIfAbsent(cacheKey, k -> {
            try {
                return new RSAAutoCertificateConfig.Builder()
                        .merchantId(payConfig.mchId())
                        .privateKey(payConfig.mchPrivateKeyPem())
                        .merchantSerialNumber(payConfig.mchCertSerialNo())
                        .apiV3Key(payConfig.apiV3Key())
                        .build();
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.PAY_CHANNEL_ERROR, "支付配置无效或无法连接微信服务器");
            }
        });
    }

    private int yuanToFen(BigDecimal yuan) {
        return yuan.multiply(BigDecimal.valueOf(100)).intValue();
    }
}
