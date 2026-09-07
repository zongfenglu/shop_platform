package com.shopplatform.domain.shop.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shopplatform.domain.file.service.StorageService;
import com.shopplatform.domain.goods.service.GoodsService;
import com.shopplatform.domain.shop.entity.ShopQuotaUsage;
import com.shopplatform.domain.shop.entity.StoreUser;
import com.shopplatform.domain.shop.service.ShopQuotaSnapshotService;
import com.shopplatform.domain.shop.service.ShopQuotaUsageService;
import com.shopplatform.domain.shop.service.ShopService;
import com.shopplatform.domain.shop.service.StoreUserService;
import com.shopplatform.framework.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ShopQuotaSnapshotServiceImpl implements ShopQuotaSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(ShopQuotaSnapshotServiceImpl.class);

    private final ShopService shopService;
    private final ShopQuotaUsageService shopQuotaUsageService;
    private final GoodsService goodsService;
    private final StoreUserService storeUserService;
    private final StorageService storageService;

    public ShopQuotaSnapshotServiceImpl(ShopService shopService,
                                        ShopQuotaUsageService shopQuotaUsageService,
                                        GoodsService goodsService,
                                        StoreUserService storeUserService,
                                        StorageService storageService) {
        this.shopService = shopService;
        this.shopQuotaUsageService = shopQuotaUsageService;
        this.goodsService = goodsService;
        this.storeUserService = storeUserService;
        this.storageService = storageService;
    }

    @Override
    public int snapshotAll() {
        List<Long> shopIds = shopService.listAllShopIds();
        LocalDate today = LocalDate.now();
        int ok = 0;
        for (Long shopId : shopIds) {
            TenantContext.set(shopId);
            try {
                upsert(shopId, today);
                ok++;
            } catch (Exception e) {
                log.error("配额快照失败 shopId={}", shopId, e);
            } finally {
                TenantContext.clear();
            }
        }
        return ok;
    }

    private void upsert(Long shopId, LocalDate statDate) {
        int goods = (int) Math.min(Integer.MAX_VALUE, goodsService.count());
        int staff = (int) Math.min(Integer.MAX_VALUE, storeUserService.count(Wrappers.<StoreUser>lambdaQuery()));
        long storage = storageService.directorySize(shopId);

        ShopQuotaUsage row = shopQuotaUsageService.getOne(Wrappers.<ShopQuotaUsage>lambdaQuery()
                .eq(ShopQuotaUsage::getShopId, shopId)
                .eq(ShopQuotaUsage::getStatDate, statDate));
        if (row == null) {
            row = new ShopQuotaUsage();
            row.setShopId(shopId);
            row.setStatDate(statDate);
        }
        row.setGoodsCount(goods);
        row.setStaffCount(staff);
        row.setStorageBytes(storage);
        row.setSmsMonthUsed(0);
        if (row.getId() == null) {
            shopQuotaUsageService.save(row);
        } else {
            shopQuotaUsageService.updateById(row);
        }
    }
}
