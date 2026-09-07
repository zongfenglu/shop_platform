package com.shopplatform.domain.pricing.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.domain.order.entity.FreightTemplate;
import com.shopplatform.domain.order.service.FreightTemplateService;
import com.shopplatform.domain.pricing.PriceContext;
import com.shopplatform.domain.pricing.PriceWorkingState;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FreightHandlerTest {

    private final FreightTemplateService templateService = mock(FreightTemplateService.class);
    private final FreightHandler handler = new FreightHandler(templateService, new ObjectMapper());

    @Test
    void weightMethod_usesQuantityWeightedSkuWeightAndCeilingForAdditionalUnits() {
        stubTemplate("weight", "[{\"first\":1,\"firstFee\":10,\"additional\":1,\"additionalFee\":3}]");
        PriceContext ctx = context(new PriceContext.PriceItem(1L, 11L, "A", "", "", 
                new BigDecimal("20"), null, 2, new BigDecimal("0.75"), null));

        PriceWorkingState state = new PriceWorkingState(ctx);
        handler.handle(ctx, state);

        // 1.5kg: first 1kg = 10, remaining 0.5kg rounds up to one additional unit = 3.
        assertEquals(new BigDecimal("13.00"), state.freightFee());
    }

    @Test
    void volumeMethod_supportsDecimalThresholds() {
        stubTemplate("volume", "[{\"first\":0.5,\"firstFee\":8,\"additional\":0.25,\"additionalFee\":2.5}]");
        PriceContext ctx = context(new PriceContext.PriceItem(1L, 11L, "A", "", "", 
                new BigDecimal("20"), null, 3, null, new BigDecimal("0.3")));

        PriceWorkingState state = new PriceWorkingState(ctx);
        handler.handle(ctx, state);

        // 0.9m3: first 0.5m3 = 8, remaining 0.4m3 = 2 additional units * 2.5.
        assertEquals(new BigDecimal("13.00"), state.freightFee());
    }

    @Test
    void weightMethod_withoutSkuMeasurement_failsExplicitly() {
        stubTemplate("weight", "[{\"first\":1,\"firstFee\":10,\"additional\":1,\"additionalFee\":3}]");
        PriceContext ctx = context(new PriceContext.PriceItem(1L, 11L, "A", "", "", 
                new BigDecimal("20"), null, 1, null, null));

        assertThrows(BusinessException.class, () -> handler.handle(ctx, new PriceWorkingState(ctx)));
    }

    @Test
    void freeShipping_shortCircuitsBeforeMeasurementValidation() {
        stubTemplate("weight", "[{\"first\":1,\"firstFee\":10,\"additional\":1,\"additionalFee\":3}]");
        FreightTemplate template = template();
        template.setFreeRules("{\"minPrice\":19}");
        when(templateService.getByIdWithTenant(1L)).thenReturn(template);
        PriceContext ctx = context(new PriceContext.PriceItem(1L, 11L, "A", "", "", 
                new BigDecimal("20"), null, 1, null, null));

        PriceWorkingState state = new PriceWorkingState(ctx);
        handler.handle(ctx, state);

        assertEquals(BigDecimal.ZERO, state.freightFee());
    }

    private PriceContext context(PriceContext.PriceItem item) {
        return new PriceContext(1L, 2L, List.of(item), "express", 1L, null, null, "none", null);
    }

    private void stubTemplate(String method, String rules) {
        FreightTemplate template = template();
        template.setMethod(method);
        template.setRules(rules);
        when(templateService.getByIdWithTenant(1L)).thenReturn(template);
    }

    private FreightTemplate template() {
        FreightTemplate template = new FreightTemplate();
        template.setId(1L);
        template.setMethod("count");
        template.setRules("[]");
        return template;
    }
}
