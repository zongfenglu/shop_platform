package com.shopplatform.domain.stats.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.stats.StatGoodsDailyService;
import com.shopplatform.domain.stats.entity.StatGoodsDaily;
import com.shopplatform.domain.stats.mapper.StatGoodsDailyMapper;
import org.springframework.stereotype.Service;

@Service
public class StatGoodsDailyServiceImpl extends ServiceImpl<StatGoodsDailyMapper, StatGoodsDaily>
        implements StatGoodsDailyService {
}
