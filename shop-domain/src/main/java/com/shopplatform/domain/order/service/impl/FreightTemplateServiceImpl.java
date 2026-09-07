package com.shopplatform.domain.order.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.order.entity.FreightTemplate;
import com.shopplatform.domain.order.mapper.FreightTemplateMapper;
import com.shopplatform.domain.order.service.FreightTemplateService;
import org.springframework.stereotype.Service;

@Service
public class FreightTemplateServiceImpl extends ServiceImpl<FreightTemplateMapper, FreightTemplate> implements FreightTemplateService {
}
