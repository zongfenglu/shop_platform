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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 责任链第7节点：运费。见文档三 §4：
 * "运费（运费模板 + 包邮规则 + 自提免运费）"。
 * <p>
 * 商品按运费模板分组，同一模板合并计量，不同模板的费用相加；未使用模板的商品采用统一运费。
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
        Map<Long, List<PriceContext.PriceItem>> templateGroups = new LinkedHashMap<>();
        Map<Long, BigDecimal> fixedFeeByGoods = new LinkedHashMap<>();
        for (PriceContext.PriceItem item : ctx.items()) {
            if (item.freightTemplateId() != null) {
                templateGroups.computeIfAbsent(item.freightTemplateId(), ignored -> new java.util.ArrayList<>())
                        .add(item);
            } else if (item.freightFee() != null) {
                // 同一商品购买多个 SKU 只收一次统一运费。
                fixedFeeByGoods.putIfAbsent(item.goodsId(), item.freightFee());
            }
        }

        // 兼容价格引擎的内部旧调用；消费者结算始终使用服务端写入购物项的模板。
        if (templateGroups.isEmpty() && fixedFeeByGoods.isEmpty() && ctx.freightTemplateId() != null) {
            templateGroups.put(ctx.freightTemplateId(), ctx.items());
        }

        BigDecimal totalFee = fixedFeeByGoods.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        for (Map.Entry<Long, List<PriceContext.PriceItem>> entry : templateGroups.entrySet()) {
            FreightTemplate template = freightTemplateService.getByIdWithTenant(entry.getKey());
            List<PriceContext.PriceItem> items = entry.getValue();
            BigDecimal freeMinPrice = readFreeMinPrice(template.getFreeRules());
            if (freeMinPrice == null || originalGoodsTotal(items).compareTo(freeMinPrice) < 0) {
                totalFee = totalFee.add(calculate(template.getMethod(), template.getRules(), items));
            }
        }
        state.setFreightFee(totalFee.signum() == 0
                ? BigDecimal.ZERO
                : totalFee.setScale(2, RoundingMode.HALF_UP));
    }

    private BigDecimal calculate(String method, String rulesJson, List<PriceContext.PriceItem> items) {
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

            BigDecimal measure = measure(method, items);
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

    private BigDecimal measure(String method, List<PriceContext.PriceItem> items) {
        if ("count".equals(method)) {
            return items.stream()
                    .map(item -> BigDecimal.valueOf(item.quantity()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        if (!"weight".equals(method) && !"volume".equals(method)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "不支持的运费计费方式: " + method);
        }
        BigDecimal total = BigDecimal.ZERO;
        for (PriceContext.PriceItem item : items) {
            BigDecimal unit = "weight".equals(method) ? item.weight() : item.volume();
            if (unit == null || unit.signum() < 0) {
                throw new BusinessException(ErrorCode.PARAM_INVALID,
                        ("weight".equals(method) ? "按重量" : "按体积") + "计费时 SKU 缺少有效计量数据");
            }
            total = total.add(unit.multiply(BigDecimal.valueOf(item.quantity())));
        }
        return total;
    }

    private BigDecimal originalGoodsTotal(List<PriceContext.PriceItem> items) {
        return items.stream()
                .map(item -> item.skuPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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
