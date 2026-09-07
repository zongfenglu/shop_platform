package com.shopplatform.domain.mp.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.mp.MpCodeTemplateService;
import com.shopplatform.domain.mp.entity.MpCodeTemplate;
import com.shopplatform.domain.mp.mapper.MpCodeTemplateMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class MpCodeTemplateServiceImpl extends ServiceImpl<MpCodeTemplateMapper, MpCodeTemplate>
        implements MpCodeTemplateService {

    @Override
    public List<MpCodeTemplate> listAll() {
        return this.list(Wrappers.<MpCodeTemplate>lambdaQuery().orderByDesc(MpCodeTemplate::getCreateTime));
    }

    @Override
    public MpCodeTemplate save(String templateId, String userVersion, String userDesc) {
        if (!StringUtils.hasText(templateId) || !StringUtils.hasText(userVersion)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "请填写模板 ID 与版本号");
        }
        MpCodeTemplate row = this.getOne(Wrappers.<MpCodeTemplate>lambdaQuery()
                .eq(MpCodeTemplate::getTemplateId, templateId.trim()));
        if (row == null) {
            row = new MpCodeTemplate();
            row.setTemplateId(templateId.trim());
        }
        row.setUserVersion(userVersion.trim());
        row.setUserDesc(userDesc == null ? "" : userDesc.trim());
        row.setStatus("online");
        this.saveOrUpdate(row);
        return this.getOne(Wrappers.<MpCodeTemplate>lambdaQuery()
                .eq(MpCodeTemplate::getTemplateId, templateId.trim()));
    }

    @Override
    public void disable(Long id) {
        MpCodeTemplate row = this.getOne(Wrappers.<MpCodeTemplate>lambdaQuery().eq(MpCodeTemplate::getId, id));
        if (row == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "模板不存在");
        }
        row.setStatus("disabled");
        this.updateById(row);
    }
}
