package com.shopplatform.domain.diy.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.diy.entity.DiyTemplate;
import com.shopplatform.domain.diy.mapper.DiyTemplateMapper;
import com.shopplatform.domain.diy.service.DiyTemplateService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DiyTemplateServiceImpl extends ServiceImpl<DiyTemplateMapper, DiyTemplate> implements DiyTemplateService {

    @Override
    public List<DiyTemplate> listShown() {
        return this.list(Wrappers.<DiyTemplate>lambdaQuery()
                .eq(DiyTemplate::getIsShow, true)
                .orderByAsc(DiyTemplate::getSort));
    }
}
