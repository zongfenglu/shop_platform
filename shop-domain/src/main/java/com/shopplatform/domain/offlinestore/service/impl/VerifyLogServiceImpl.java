package com.shopplatform.domain.offlinestore.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.offlinestore.entity.VerifyLog;
import com.shopplatform.domain.offlinestore.mapper.VerifyLogMapper;
import com.shopplatform.domain.offlinestore.service.VerifyLogService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VerifyLogServiceImpl extends ServiceImpl<VerifyLogMapper, VerifyLog> implements VerifyLogService {

    @Override
    public List<VerifyLog> listByStore(Long storeId) {
        LambdaQueryWrapper<VerifyLog> wrapper = new LambdaQueryWrapper<VerifyLog>()
                .orderByDesc(VerifyLog::getVerifyTime);
        if (storeId != null) {
            wrapper.eq(VerifyLog::getStoreId, storeId);
        }
        return list(wrapper);
    }
}
