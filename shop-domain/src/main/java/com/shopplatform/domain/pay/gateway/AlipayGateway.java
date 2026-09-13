package com.shopplatform.domain.pay.gateway;

import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayResponse;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.domain.AlipayTradeWapPayModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.ijpay.alipay.AliPayApi;
import com.ijpay.alipay.AliPayApiConfig;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.pay.service.ShopPayConfigService.DecryptedAlipayPayConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/** IJPay 支付宝适配器。显式传入 AlipayClient，避免多租户配置依赖全局 ThreadLocal。 */
@Component
public class AlipayGateway {

    private static final Logger log = LoggerFactory.getLogger(AlipayGateway.class);

    public String createWapPrepay(DecryptedAlipayPayConfig config, String outTradeNo, BigDecimal payPrice,
                                  String subject, String notifyUrl, String returnUrl) {
        AlipayTradeWapPayModel model = new AlipayTradeWapPayModel();
        model.setOutTradeNo(outTradeNo);
        model.setTotalAmount(payPrice.toPlainString());
        model.setSubject(subject);
        model.setProductCode("QUICK_WAP_WAY");
        model.setQuitUrl(returnUrl);

        AlipayTradeWapPayRequest request = new AlipayTradeWapPayRequest();
        request.setBizModel(model);
        request.setNotifyUrl(notifyUrl);
        request.setReturnUrl(returnUrl);
        try {
            AlipayResponse response = AliPayApi.pageExecute(client(config), request, "GET");
            if (!response.isSuccess() || response.getBody() == null || response.getBody().isBlank()) {
                log.error("支付宝WAP下单被拒绝 outTradeNo={} code={} subCode={}",
                        outTradeNo, response.getCode(), response.getSubCode());
                throw new BusinessException(ErrorCode.PAY_CHANNEL_ERROR, "支付宝下单失败，请检查商户配置");
            }
            return response.getBody();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("支付宝WAP下单失败 outTradeNo={}", outTradeNo, e);
            throw new BusinessException(ErrorCode.PAY_CHANNEL_ERROR, "支付宝下单失败，请稍后重试");
        }
    }

    public boolean verifyNotification(DecryptedAlipayPayConfig config, Map<String, String> parameters) {
        try {
            return AlipaySignature.rsaCheckV1(parameters, config.alipayPublicKey(), "UTF-8", "RSA2");
        } catch (Exception e) {
            log.error("支付宝回调验签失败", e);
            return false;
        }
    }

    public QueryResult queryOrder(DecryptedAlipayPayConfig config, String outTradeNo) {
        AlipayTradeQueryModel model = new AlipayTradeQueryModel();
        model.setOutTradeNo(outTradeNo);
        try {
            AlipayTradeQueryResponse response = AliPayApi.tradeQueryToResponse(client(config), false, model);
            return new QueryResult(response.getTradeNo(), response.getOutTradeNo(), response.getTotalAmount(),
                    response.getTradeStatus());
        } catch (Exception e) {
            log.error("支付宝主动查单失败 outTradeNo={}", outTradeNo, e);
            throw new BusinessException(ErrorCode.PAY_CHANNEL_ERROR, "支付宝查单失败，请稍后重试");
        }
    }

    public RefundResult createRefund(DecryptedAlipayPayConfig config, String outTradeNo, String outRequestNo,
                                     BigDecimal refundAmount, String reason) {
        AlipayTradeRefundModel model = new AlipayTradeRefundModel();
        model.setOutTradeNo(outTradeNo);
        model.setOutRequestNo(outRequestNo);
        model.setRefundAmount(refundAmount.toPlainString());
        model.setRefundReason(reason);
        try {
            AlipayTradeRefundResponse response = AliPayApi.tradeRefundToResponse(client(config), false, model);
            if (!response.isSuccess()) {
                log.error("支付宝退款被拒绝 outTradeNo={} code={} subCode={}",
                        outTradeNo, response.getCode(), response.getSubCode());
                throw new BusinessException(ErrorCode.PAY_CHANNEL_ERROR,
                        response.getSubMsg() == null ? "支付宝退款失败" : response.getSubMsg());
            }
            return new RefundResult(response.getTradeNo(), response.getBody());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("支付宝退款请求失败 outTradeNo={} outRequestNo={}", outTradeNo, outRequestNo, e);
            throw new BusinessException(ErrorCode.PAY_CHANNEL_ERROR, "支付宝退款失败，请稍后重试");
        }
    }

    private AlipayClient client(DecryptedAlipayPayConfig config) {
        return AliPayApiConfig.builder()
                .setAppId(config.appId())
                .setPrivateKey(config.privateKey())
                .setAliPayPublicKey(config.alipayPublicKey())
                .setServiceUrl(config.gatewayUrl())
                .setCharset("UTF-8")
                .setSignType("RSA2")
                .build()
                .getAliPayClient();
    }

    public record QueryResult(String transactionId, String outTradeNo, String totalAmount, String tradeStatus) {
    }

    public record RefundResult(String transactionId, String rawResponse) {
    }
}
