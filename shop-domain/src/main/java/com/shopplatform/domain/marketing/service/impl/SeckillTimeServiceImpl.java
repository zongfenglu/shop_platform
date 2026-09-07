package com.shopplatform.domain.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.marketing.entity.SeckillTime;
import com.shopplatform.domain.marketing.mapper.SeckillTimeMapper;
import com.shopplatform.domain.marketing.service.SeckillTimeService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SeckillTimeServiceImpl extends ServiceImpl<SeckillTimeMapper, SeckillTime> implements SeckillTimeService {

    @Override
    public List<SeckillTime> listAll() {
        return list(new LambdaQueryWrapper<SeckillTime>()
                .orderByAsc(SeckillTime::getSort)
                .orderByAsc(SeckillTime::getId));
    }
}
