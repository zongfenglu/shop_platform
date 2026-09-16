package com.shopplatform.domain.marketing.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PointsGoodsTest {

    @Test
    void mutuallyExclusiveReferencesAreAlwaysWrittenOnUpdate() throws NoSuchFieldException {
        assertAlwaysUpdate("goodsId");
        assertAlwaysUpdate("couponId");
    }

    private void assertAlwaysUpdate(String fieldName) throws NoSuchFieldException {
        TableField tableField = PointsGoods.class.getDeclaredField(fieldName).getAnnotation(TableField.class);
        assertEquals(FieldStrategy.ALWAYS, tableField.updateStrategy());
    }
}
