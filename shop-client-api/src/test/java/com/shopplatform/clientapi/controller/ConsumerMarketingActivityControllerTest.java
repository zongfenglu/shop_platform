package com.shopplatform.clientapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopplatform.domain.goods.service.GoodsService;
import com.shopplatform.domain.goods.service.GoodsSkuService;
import com.shopplatform.domain.marketing.entity.BargainActive;
import com.shopplatform.domain.marketing.entity.GroupActive;
import com.shopplatform.domain.marketing.service.BargainActiveService;
import com.shopplatform.domain.marketing.service.BargainRecordService;
import com.shopplatform.domain.marketing.service.GroupActiveService;
import com.shopplatform.domain.marketing.service.GroupRecordService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsumerMarketingActivityControllerTest {

    @Test
    void bargainDetail_recoversLegacyRoundedSnowflakeId() {
        long actualId = 2097153019094523906L;
        long roundedId = 2097153019094524000L;
        BargainActive active = new BargainActive();
        active.setId(actualId);
        active.setStatus("on");
        active.setGoodsId(21L);
        BargainActiveService activeService = mock(BargainActiveService.class);
        when(activeService.listOnSale()).thenReturn(List.of(active));
        ConsumerBargainController controller = new ConsumerBargainController(
                activeService, mock(BargainRecordService.class), mock(GoodsService.class), mock(GoodsSkuService.class));

        Map<String, Object> result = controller.active(roundedId).getData();

        assertEquals(String.valueOf(actualId), result.get("id"));
    }

    @Test
    void groupDetail_recoversLegacyRoundedSnowflakeId() {
        long actualId = 2097872871375908865L;
        long roundedId = 2097872871375908864L;
        GroupActive active = new GroupActive();
        active.setId(actualId);
        active.setStatus("on");
        active.setGoodsId(21L);
        active.setStartTime(LocalDateTime.now().minusHours(1));
        active.setEndTime(LocalDateTime.now().plusHours(1));
        GroupActiveService activeService = mock(GroupActiveService.class);
        when(activeService.getByCompatibleIdWithTenant(roundedId)).thenReturn(active);
        when(activeService.parseGroupPrice(active)).thenReturn(Map.of());
        ConsumerGroupController controller = new ConsumerGroupController(
                activeService, mock(GroupRecordService.class), mock(GoodsService.class),
                mock(GoodsSkuService.class), new ObjectMapper());

        Map<String, Object> result = controller.active(roundedId).getData();

        assertEquals(String.valueOf(actualId), result.get("id"));
    }
}
