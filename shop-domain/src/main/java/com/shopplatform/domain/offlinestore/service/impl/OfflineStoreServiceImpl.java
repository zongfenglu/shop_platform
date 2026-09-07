package com.shopplatform.domain.offlinestore.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.offlinestore.entity.OfflineStore;
import com.shopplatform.domain.offlinestore.mapper.OfflineStoreMapper;
import com.shopplatform.domain.offlinestore.service.OfflineStoreService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OfflineStoreServiceImpl extends ServiceImpl<OfflineStoreMapper, OfflineStore> implements OfflineStoreService {

    @Override
    public List<OfflineStore> listEnabled() {
        return list(new LambdaQueryWrapper<OfflineStore>()
                .eq(OfflineStore::getStatus, "enabled")
                .orderByDesc(OfflineStore::getCreateTime));
    }
}
