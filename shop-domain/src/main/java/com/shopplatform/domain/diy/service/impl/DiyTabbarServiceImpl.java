package com.shopplatform.domain.diy.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.diy.entity.DiyTabbar;
import com.shopplatform.domain.diy.mapper.DiyTabbarMapper;
import com.shopplatform.domain.diy.service.DiyTabbarService;
import com.shopplatform.framework.tenant.TenantContext;
import org.springframework.stereotype.Service;

@Service
public class DiyTabbarServiceImpl extends ServiceImpl<DiyTabbarMapper, DiyTabbar> implements DiyTabbarService {

    private static final String DEFAULT_ITEMS = "["
            + "{\"icon\":\"\",\"activeIcon\":\"\",\"text\":\"首页\",\"path\":\"/pages/index/index\"},"
            + "{\"icon\":\"\",\"activeIcon\":\"\",\"text\":\"分类\",\"path\":\"/pages/goods/list\"},"
            + "{\"icon\":\"\",\"activeIcon\":\"\",\"text\":\"购物车\",\"path\":\"/pages/cart/index\"},"
            + "{\"icon\":\"\",\"activeIcon\":\"\",\"text\":\"我的\",\"path\":\"/pages/my/index\"}]";

    private final ObjectMapper objectMapper;

    public DiyTabbarServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public DiyTabbar getOrCreateDefault() {
        Long shopId = TenantContext.getRequired();
        DiyTabbar tabbar = this.getOne(Wrappers.<DiyTabbar>lambdaQuery().eq(DiyTabbar::getShopId, shopId));
        if (tabbar != null) {
            return tabbar;
        }
        tabbar = new DiyTabbar();
        tabbar.setShopId(shopId);
        tabbar.setItems(DEFAULT_ITEMS);
        this.save(tabbar);
        return tabbar;
    }

    @Override
    public DiyTabbar peek() {
        Long shopId = TenantContext.getRequired();
        return this.getOne(Wrappers.<DiyTabbar>lambdaQuery().eq(DiyTabbar::getShopId, shopId));
    }

    @Override
    public DiyTabbar save(String itemsJson, String styleJson) {
        int count = countItems(itemsJson);
        if (count < 2 || count > 5) {
            throw new BusinessException(ErrorCode.DIY_TABBAR_ITEM_COUNT_INVALID);
        }
        DiyTabbar tabbar = getOrCreateDefault();
        tabbar.setItems(itemsJson);
        tabbar.setStyle(styleJson);
        this.updateById(tabbar);
        return tabbar;
    }

    private int countItems(String itemsJson) {
        JsonNode node;
        try {
            node = objectMapper.readTree(itemsJson);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.DIY_TABBAR_ITEM_COUNT_INVALID);
        }
        if (node == null || !node.isArray()) {
            throw new BusinessException(ErrorCode.DIY_TABBAR_ITEM_COUNT_INVALID);
        }
        return node.size();
    }
}
