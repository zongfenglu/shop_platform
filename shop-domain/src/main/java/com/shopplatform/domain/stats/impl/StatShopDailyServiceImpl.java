package com.shopplatform.domain.stats.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.stats.StatShopDailyService;
import com.shopplatform.domain.stats.entity.StatShopDaily;
import com.shopplatform.domain.stats.mapper.StatShopDailyMapper;
import org.springframework.stereotype.Service;

@Service
public class StatShopDailyServiceImpl extends ServiceImpl<StatShopDailyMapper, StatShopDaily>
        implements StatShopDailyService {
}
