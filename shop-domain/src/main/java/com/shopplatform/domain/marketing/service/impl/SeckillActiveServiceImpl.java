package com.shopplatform.domain.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.marketing.entity.SeckillActive;
import com.shopplatform.domain.marketing.mapper.SeckillActiveMapper;
import com.shopplatform.domain.marketing.service.SeckillActiveService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class SeckillActiveServiceImpl extends ServiceImpl<SeckillActiveMapper, SeckillActive> implements SeckillActiveService {

    @Override
    public List<SeckillActive> listOnSale() {
        LocalDate today = LocalDate.now();
        return list(new LambdaQueryWrapper<SeckillActive>()
                .eq(SeckillActive::getStatus, "on")
                .le(SeckillActive::getStartDate, today)
                .ge(SeckillActive::getEndDate, today)
                .orderByDesc(SeckillActive::getId));
    }

    @Override
    public List<SeckillActive> listAll() {
        return list(new LambdaQueryWrapper<SeckillActive>()
                .orderByDesc(SeckillActive::getId));
    }
}
