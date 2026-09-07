package com.shopplatform.domain.shop.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.diy.entity.DiyPage;
import com.shopplatform.domain.diy.mapper.DiyPageMapper;
import com.shopplatform.domain.goods.entity.Goods;
import com.shopplatform.domain.goods.mapper.GoodsMapper;
import com.shopplatform.domain.offlinestore.entity.OfflineStore;
import com.shopplatform.domain.offlinestore.mapper.OfflineStoreMapper;
import com.shopplatform.domain.shop.entity.Shop;
import com.shopplatform.domain.shop.entity.ShopPackage;
import com.shopplatform.domain.shop.entity.StoreUser;
import com.shopplatform.domain.shop.mapper.StoreUserMapper;
import com.shopplatform.domain.shop.service.PackageQuotaChecker;
import com.shopplatform.domain.shop.service.ShopPackageService;
import com.shopplatform.domain.shop.service.ShopService;
import com.shopplatform.framework.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PackageQuotaCheckerImpl implements PackageQuotaChecker {

    private final ShopService shopService;
    private final ShopPackageService shopPackageService;
    private final GoodsMapper goodsMapper;
    private final StoreUserMapper storeUserMapper;
    private final OfflineStoreMapper offlineStoreMapper;
    private final DiyPageMapper diyPageMapper;
    private final ObjectMapper objectMapper;

    public PackageQuotaCheckerImpl(ShopService shopService,
                                   ShopPackageService shopPackageService,
                                   GoodsMapper goodsMapper,
                                   StoreUserMapper storeUserMapper,
                                   OfflineStoreMapper offlineStoreMapper,
                                   DiyPageMapper diyPageMapper,
                                   ObjectMapper objectMapper) {
        this.shopService = shopService;
        this.shopPackageService = shopPackageService;
        this.goodsMapper = goodsMapper;
        this.storeUserMapper = storeUserMapper;
        this.offlineStoreMapper = offlineStoreMapper;
        this.diyPageMapper = diyPageMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public void requireGoods() {
        require("goods_max", "商品数", goodsMapper.selectCount(
                Wrappers.<Goods>lambdaQuery().ne(Goods::getStatus, "deleted")));
    }

    @Override
    public void requireStaff() {
        require("staff_max", "员工账号", storeUserMapper.selectCount(Wrappers.<StoreUser>lambdaQuery()));
    }

    @Override
    public void requireStore() {
        require("store_max", "自提门店", offlineStoreMapper.selectCount(Wrappers.<OfflineStore>lambdaQuery()));
    }

    @Override
    public void requireDiyPage() {
        require("diy_page_max", "装修页", diyPageMapper.selectCount(Wrappers.<DiyPage>lambdaQuery()));
    }

    private void require(String field, String label, long used) {
        long max = currentMax(field);
        if (max < 0) {
            return;
        }
        if (used >= max) {
            throw new BusinessException(ErrorCode.QUOTA_EXCEEDED, label + "已达套餐上限 " + max);
        }
    }

    private long currentMax(String field) {
        Long shopId = TenantContext.getRequired();
        Shop shop = shopService.getOne(Wrappers.<Shop>lambdaQuery().eq(Shop::getId, shopId));
        if (shop == null || shop.getPackageId() == null) {
            return -1;
        }
        ShopPackage pkg = shopPackageService.getOne(
                Wrappers.<ShopPackage>lambdaQuery().eq(ShopPackage::getId, shop.getPackageId()));
        if (pkg == null || !StringUtils.hasText(pkg.getQuota())) {
            return -1;
        }
        try {
            JsonNode node = objectMapper.readTree(pkg.getQuota()).get(field);
            if (node == null || !node.isNumber()) {
                return -1;
            }
            return node.asLong();
        } catch (Exception e) {
            return -1;
        }
    }
}
