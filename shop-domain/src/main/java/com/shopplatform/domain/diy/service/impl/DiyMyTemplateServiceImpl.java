package com.shopplatform.domain.diy.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.diy.entity.DiyMyTemplate;
import com.shopplatform.domain.diy.mapper.DiyMyTemplateMapper;
import com.shopplatform.domain.diy.service.DiyMyTemplateService;
import com.shopplatform.framework.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DiyMyTemplateServiceImpl extends ServiceImpl<DiyMyTemplateMapper, DiyMyTemplate> implements DiyMyTemplateService {

    @Override
    public List<DiyMyTemplate> listMine() {
        return this.list(Wrappers.<DiyMyTemplate>lambdaQuery()
                .eq(DiyMyTemplate::getShopId, TenantContext.getRequired())
                .orderByDesc(DiyMyTemplate::getUpdateTime));
    }

    @Override
    public DiyMyTemplate save(String name, String pageDataJson) {
        DiyMyTemplate template = new DiyMyTemplate();
        template.setShopId(TenantContext.getRequired());
        template.setName(name);
        template.setPageData(pageDataJson);
        this.save(template);
        return template;
    }
}
