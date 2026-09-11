package com.shopplatform.domain.goods.support;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shopplatform.domain.goods.entity.Goods;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoodsCategoryQueryTest {

    @Test
    void containsAny_matchesNumericAndStringJsonIds() {
        var wrapper = Wrappers.<Goods>lambdaQuery();

        GoodsCategoryQuery.applyContainsAny(wrapper, List.of(2097131103839346689L, 12L));

        assertTrue(wrapper.getSqlSegment().contains("JSON_OVERLAPS(category_ids"));
        assertEquals(List.of("[2097131103839346689,\"2097131103839346689\",12,\"12\"]"),
                wrapper.getParamNameValuePairs().values().stream().toList());
    }

    @Test
    void containsAny_rejectsEmptyCategorySet() {
        var wrapper = Wrappers.<Goods>lambdaQuery();

        GoodsCategoryQuery.applyContainsAny(wrapper, List.of());

        assertTrue(wrapper.getSqlSegment().contains("1 = 0"));
    }
}
