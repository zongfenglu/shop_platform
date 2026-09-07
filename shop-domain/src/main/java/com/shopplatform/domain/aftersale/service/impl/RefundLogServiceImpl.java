package com.shopplatform.domain.aftersale.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.aftersale.entity.RefundLog;
import com.shopplatform.domain.aftersale.mapper.RefundLogMapper;
import com.shopplatform.domain.aftersale.service.RefundLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefundLogServiceImpl extends ServiceImpl<RefundLogMapper, RefundLog> implements RefundLogService {

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveIndependently(RefundLog log) {
        this.save(log);
    }
}
