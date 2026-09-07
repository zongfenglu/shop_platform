package com.shopplatform.domain.diy.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.exception.TenantAccessDeniedException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.diy.entity.DiyPage;
import com.shopplatform.domain.diy.entity.DiyTemplate;
import com.shopplatform.domain.diy.mapper.DiyPageMapper;
import com.shopplatform.domain.diy.service.DiyPageService;
import com.shopplatform.domain.diy.service.DiyTemplateService;
import com.shopplatform.domain.diy.support.DiyPageContentValidator;
import com.shopplatform.domain.shop.service.PackageQuotaChecker;
import com.shopplatform.framework.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DiyPageServiceImpl extends ServiceImpl<DiyPageMapper, DiyPage> implements DiyPageService {

    private static final String DEFAULT_DRAFT_DATA = "{\"page\":{\"name\":\"新页面\"},\"items\":[]}";

    private final DiyTemplateService diyTemplateService;
    private final DiyPageContentValidator contentValidator;
    private final PackageQuotaChecker packageQuotaChecker;

    public DiyPageServiceImpl(DiyTemplateService diyTemplateService,
                              DiyPageContentValidator contentValidator,
                              PackageQuotaChecker packageQuotaChecker) {
        this.diyTemplateService = diyTemplateService;
        this.contentValidator = contentValidator;
        this.packageQuotaChecker = packageQuotaChecker;
    }

    @Override
    public List<DiyPage> list() {
        return this.list(Wrappers.<DiyPage>lambdaQuery()
                .eq(DiyPage::getShopId, TenantContext.getRequired())
                .orderByDesc(DiyPage::getUpdateTime));
    }

    @Override
    @Transactional
    public DiyPage createPage(CreatePageCommand command) {
        packageQuotaChecker.requireDiyPage();
        if (!"home".equals(command.pageType()) && !"custom".equals(command.pageType())) {
            throw new BusinessException(ErrorCode.DIY_PAGE_TYPE_NOT_SUPPORTED);
        }
        DiyPage page = new DiyPage();
        page.setShopId(TenantContext.getRequired());
        page.setPageType(command.pageType());
        page.setName(command.name());
        boolean becomeHome = "home".equals(command.pageType())
                && getDefaultHome(TenantContext.getRequired()) == null;
        page.setIsDefault(becomeHome);
        page.setVersion(0);

        if (command.copyFromPageId() != null) {
            DiyPage source = this.getByIdWithTenant(command.copyFromPageId());
            page.setDraftData(source.getPageData() != null ? source.getPageData() : source.getDraftData());
        } else {
            page.setDraftData(DEFAULT_DRAFT_DATA);
        }
        this.save(page);
        return page;
    }

    @Override
    public DiyPage updateDraft(Long id, String draftDataJson) {
        DiyPage page = this.getByIdWithTenant(id);
        contentValidator.validate(draftDataJson, page.getShopId());
        page.setDraftData(draftDataJson);
        this.updateById(page);
        return page;
    }

    @Override
    public DiyPage publish(Long id) {
        DiyPage page = this.getByIdWithTenant(id);
        contentValidator.validate(page.getDraftData(), page.getShopId());
        page.setPageData(page.getDraftData());
        page.setVersion(page.getVersion() + 1);
        page.setPublishTime(LocalDateTime.now());
        this.updateById(page);
        return page;
    }

    @Override
    public DiyPage copyPage(Long id, String newName) {
        packageQuotaChecker.requireDiyPage();
        DiyPage source = this.getByIdWithTenant(id);
        DiyPage copy = new DiyPage();
        copy.setShopId(source.getShopId());
        copy.setPageType(source.getPageType());
        copy.setName(newName != null && !newName.isBlank() ? newName : source.getName() + " 副本");
        copy.setDraftData(source.getPageData() != null ? source.getPageData() : source.getDraftData());
        copy.setIsDefault(false);
        copy.setVersion(0);
        this.save(copy);
        return copy;
    }

    @Override
    @Transactional
    public void setHome(Long id) {
        DiyPage page = this.getByIdWithTenant(id);
        clearDefaultFlag(page.getShopId());
        page.setPageType("home");
        page.setIsDefault(true);
        this.updateById(page);
    }

    @Override
    public void delete(Long id) {
        DiyPage page = this.getByIdWithTenant(id);
        if (Boolean.TRUE.equals(page.getIsDefault())) {
            throw new BusinessException(ErrorCode.DIY_PAGE_TYPE_NOT_SUPPORTED, "当前首页不能删除");
        }
        this.removeById(id);
    }

    @Override
    public DiyPage applyTemplate(Long templateId, String name) {
        packageQuotaChecker.requireDiyPage();
        DiyTemplate template;
        try {
            template = diyTemplateService.getByIdWithTenant(templateId);
        } catch (TenantAccessDeniedException e) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "行业模板不存在");
        }
        DiyPage page = new DiyPage();
        page.setShopId(TenantContext.getRequired());
        page.setPageType("custom");
        page.setName(name != null && !name.isBlank() ? name : template.getName());
        page.setDraftData(template.getPageData());
        page.setIsDefault(false);
        page.setVersion(0);
        this.save(page);
        return page;
    }

    @Override
    public DiyPage getDefaultHome(Long shopId) {
        return this.getOne(Wrappers.<DiyPage>lambdaQuery()
                .eq(DiyPage::getShopId, shopId)
                .eq(DiyPage::getIsDefault, true)
                .last("LIMIT 1"), false);
    }

    private void clearDefaultFlag(Long shopId) {
        this.update(Wrappers.<DiyPage>lambdaUpdate()
                .eq(DiyPage::getShopId, shopId)
                .eq(DiyPage::getIsDefault, true)
                .set(DiyPage::getIsDefault, false));
    }
}
