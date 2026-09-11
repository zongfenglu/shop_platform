package com.shopplatform.domain.goods.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.shopplatform.domain.goods.entity.Goods;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/** 商品 category_ids JSON 列的兼容查询。 */
public final class GoodsCategoryQuery {

    private GoodsCategoryQuery() {
    }

    public static void applyContainsAny(LambdaQueryWrapper<Goods> wrapper, Collection<Long> categoryIds) {
        Set<Long> ids = new LinkedHashSet<>(categoryIds == null ? Set.of() : categoryIds);
        ids.removeIf(Objects::isNull);
        if (ids.isEmpty()) {
            wrapper.apply("1 = 0");
            return;
        }

        // 历史数据是 JSON 数字数组，新数据受全局 Long 序列化配置影响是字符串数组。
        // 同时放入两种 JSON 标量，让 JSON_OVERLAPS 对两类存量数据都能命中。
        ArrayNode candidates = JsonNodeFactory.instance.arrayNode();
        ids.forEach(id -> {
            candidates.add(id);
            candidates.add(String.valueOf(id));
        });
        wrapper.apply("JSON_OVERLAPS(category_ids, {0})", candidates.toString());
    }
}
