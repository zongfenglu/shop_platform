package com.shopplatform.domain.setting.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.setting.entity.ExpressCompany;
import com.shopplatform.domain.setting.mapper.ExpressCompanyMapper;
import com.shopplatform.domain.setting.service.ExpressCompanyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ExpressCompanyServiceImpl extends ServiceImpl<ExpressCompanyMapper, ExpressCompany>
        implements ExpressCompanyService {

    private static final List<String[]> DEFAULTS = List.of(
            new String[]{"顺丰速运", "shunfeng"}, new String[]{"京东物流", "jd"},
            new String[]{"中通快递", "zhongtong"}, new String[]{"圆通速递", "yuantong"},
            new String[]{"申通快递", "shentong"}, new String[]{"韵达快递", "yunda"},
            new String[]{"邮政EMS", "ems"}, new String[]{"极兔速递", "jtexpress"});

    @Override
    public List<ExpressCompany> listAllWithDefaults() {
        seedDefaults();
        return list(Wrappers.<ExpressCompany>lambdaQuery()
                .orderByAsc(ExpressCompany::getSort).orderByDesc(ExpressCompany::getCreateTime));
    }

    @Override
    public List<ExpressCompany> listEnabled() {
        seedDefaults();
        return list(Wrappers.<ExpressCompany>lambdaQuery()
                .eq(ExpressCompany::getStatus, "enabled")
                .orderByAsc(ExpressCompany::getSort));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public synchronized void seedDefaults() {
        if (count() > 0) return;
        for (int i = 0; i < DEFAULTS.size(); i++) {
            ExpressCompany item = new ExpressCompany();
            item.setName(DEFAULTS.get(i)[0]);
            item.setCode(DEFAULTS.get(i)[1]);
            item.setSort((i + 1) * 10);
            item.setStatus("enabled");
            save(item);
        }
    }
}
