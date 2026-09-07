package com.shopplatform.domain.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.marketing.entity.BargainActive;
import com.shopplatform.domain.marketing.mapper.BargainActiveMapper;
import com.shopplatform.domain.marketing.service.BargainActiveService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BargainActiveServiceImpl extends ServiceImpl<BargainActiveMapper, BargainActive> implements BargainActiveService {

    @Override
    public List<BargainActive> listOnSale() {
        return list(new LambdaQueryWrapper<BargainActive>()
                .eq(BargainActive::getStatus, "on")
                .orderByDesc(BargainActive::getId));
    }

    @Override
    public List<BargainActive> listAll() {
        return list(new LambdaQueryWrapper<BargainActive>()
                .orderByDesc(BargainActive::getId));
    }
}
