package com.shopplatform.domain.pricing.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.order.entity.FreightTemplate;
import com.shopplatform.domain.order.service.FreightTemplateService;
import com.shopplatform.domain.pricing.PriceContext;
import com.shopplatform.domain.pricing.PriceHandler;
import com.shopplatform.domain.pricing.PriceHandlerType;
import com.shopplatform.domain.pricing.PriceWorkingState;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 责任链第7节点：运费。见文档三 §4：
 * "运费（运费模板 + 包邮规则 + 自提免运费）"。
 * <p>
 * 支持按件数、重量或体积的首段/续段阶梯计费，以及按原价小计的满额包邮。
 * 地区差异化包邮仍由地址/地区规则阶段处理。
 */
@Component
public class FreightHandler implements PriceHandler {

    private final FreightTemplateService freightTemplateService;
    private final ObjectMapper objectMapper;

    public FreightHandler(FreightTemplateService freightTemplateService, ObjectMapper objectMapper) {
        this.freightTemplateService = freightTemplateService;
        this.objectMapper = objectMapper;
    }

    @Override
    public PriceHandlerType type() {
        return PriceHandlerType.FREIGHT;
    }

    @Override
    public void handle(PriceContext ctx, PriceWorkingState state) {
        if ("pickup".equals(ctx.deliveryType())) {
            state.setFreightFee(BigDecimal.ZERO);
            return;
        }
        // 满减规则命中的"满X包邮"优先于此处的模板计费
        if (state.isFreeExpress()) {
            state.setFreightFee(BigDecimal.ZERO);
            return;
        }
        if (ctx.freightTemplateId() == null) {
            state.setFreightFee(BigDecimal.ZERO);
            return;
        }

        FreightTemplate template = freightTemplateService.getByIdWithTenant(ctx.freightTemplateId());
        BigDecimal freeMinPrice = readFreeMinPrice(template.getFreeRules());
        if (freeMinPrice != null && state.originalGoodsTotal().compareTo(freeMinPrice) >= 0) {
            // 满额包邮判定用原价小计，不受前面 Handler 折扣影响——避免"先靠优惠券把小计砍到临界值以下，
            // 结果反而触发了包邮"这种优惠叠加造成的悖论，判定口径必须固定为原价。
            state.setFreightFee(BigDecimal.ZERO);
            return;
        }

        state.setFreightFee(calculate(template.getMethod(), template.getRules(), ctx));
    }

    private BigDecimal calculate(String method, String rulesJson, PriceContext ctx) {
        try {
            JsonNode rules = objectMapper.readTree(rulesJson);
            if (!rules.isArray() || rules.isEmpty()) {
                return BigDecimal.ZERO;
            }
            // 地区匹配待地址规则接入后处理；当前取默认规则（通常 region=["*"]）。
            JsonNode rule = rules.get(0);
            BigDecimal first = decimal(rule, "first", BigDecimal.ONE);
            BigDecimal firstFee = new BigDecimal(rule.path("firstFee").asText("0"));
            BigDecimal additional = decimal(rule, "additional", BigDecimal.ONE);
            if (first.signum() <= 0 || additional.signum() <= 0) {
                throw new BusinessException(ErrorCode.PARAM_INVALID, "运费模板阶梯参数必须大于0");
            }
            BigDecimal additionalFee = new BigDecimal(rule.path("additionalFee").asText("0"));

            BigDecimal measure = measure(method, ctx);
            if (measure.compareTo(first) <= 0) {
                return firstFee.setScale(2, RoundingMode.HALF_UP);
            }
            BigDecimal remaining = measure.subtract(first);
            int additionalUnits = remaining.divide(additional, 0, RoundingMode.CEILING).intValueExact();
            return firstFee.add(additionalFee.multiply(BigDecimal.valueOf(additionalUnits)))
                    .setScale(2, RoundingMode.HALF_UP);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "运费模板规则无效");
        }
    }

    private BigDecimal measure(String method, PriceContext ctx) {
        if ("count".equals(method)) {
            return ctx.items().stream()
                    .map(item -> BigDecimal.valueOf(item.quantity()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        if (!"weight".equals(method) && !"volume".equals(method)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "不支持的运费计费方式: " + method);
        }
        BigDecimal total = BigDecimal.ZERO;
        for (PriceContext.PriceItem item : ctx.items()) {
            BigDecimal unit = "weight".equals(method) ? item.weight() : item.volume();
            if (unit == null || unit.signum() < 0) {
                throw new BusinessException(ErrorCode.PARAM_INVALID,
                        ("weight".equals(method) ? "按重量" : "按体积") + "计费时 SKU 缺少有效计量数据");
            }
            total = total.add(unit.multiply(BigDecimal.valueOf(item.quantity())));
        }
        return total;
    }

    private BigDecimal decimal(JsonNode node, String field, BigDecimal fallback) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? fallback : new BigDecimal(value.asText());
    }

    private BigDecimal readFreeMinPrice(String freeRulesJson) {
        if (freeRulesJson == null || freeRulesJson.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(freeRulesJson);
            if (node.has("minPrice")) {
                return new BigDecimal(node.get("minPrice").asText());
            }
        } catch (Exception ignored) {
            // 规则解析失败按"无包邮规则"处理，不阻断下单
        }
        return null;
    }
}
