package com.shopplatform.domain.shop.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.shop.entity.ShopQuotaUsage;
import com.shopplatform.domain.shop.mapper.ShopQuotaUsageMapper;
import com.shopplatform.domain.shop.service.ShopQuotaUsageService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Service
public class ShopQuotaUsageServiceImpl extends ServiceImpl<ShopQuotaUsageMapper, ShopQuotaUsage>
        implements ShopQuotaUsageService {

    @Override
    public List<ShopQuotaUsage> listLatestSnapshots(int limit) {
        List<ShopQuotaUsage> rows = this.list(Wrappers.<ShopQuotaUsage>lambdaQuery()
                .orderByDesc(ShopQuotaUsage::getStatDate)
                .orderByDesc(ShopQuotaUsage::getCreateTime));
        LinkedHashMap<Long, ShopQuotaUsage> latest = new LinkedHashMap<>();
        for (ShopQuotaUsage row : rows) {
            if (row.getShopId() == null) {
                continue;
            }
            latest.putIfAbsent(row.getShopId(), row);
            if (limit > 0 && latest.size() >= limit) {
                break;
            }
        }
        return new ArrayList<>(latest.values());
    }
}
