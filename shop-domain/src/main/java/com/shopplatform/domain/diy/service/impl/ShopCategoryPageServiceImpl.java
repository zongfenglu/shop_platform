package com.shopplatform.domain.diy.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.diy.entity.ShopCategoryPage;
import com.shopplatform.domain.diy.mapper.ShopCategoryPageMapper;
import com.shopplatform.domain.diy.service.ShopCategoryPageService;
import com.shopplatform.framework.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Set;

@Service
public class ShopCategoryPageServiceImpl extends ServiceImpl<ShopCategoryPageMapper, ShopCategoryPage>
        implements ShopCategoryPageService {

    private static final Set<String> STYLES = Set.of(
            ShopCategoryPage.STYLE_LEVEL1_LARGE,
            ShopCategoryPage.STYLE_LEVEL1_SMALL,
            ShopCategoryPage.STYLE_LEVEL2);

    public static final String DEFAULT_SHARE_TITLE = "全部分类";

    @Override
    public ShopCategoryPage getOrCreate() {
        ShopCategoryPage row = this.getOne(Wrappers.lambdaQuery());
        if (row != null) {
            return row;
        }
        row = new ShopCategoryPage();
        row.setShopId(TenantContext.getRequired());
        row.setStyle(ShopCategoryPage.STYLE_LEVEL1_SMALL);
        row.setShareTitle(DEFAULT_SHARE_TITLE);
        this.save(row);
        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShopCategoryPage saveStyle(String style, String shareTitle) {
        if (!StringUtils.hasText(style) || !STYLES.contains(style)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "不支持的分类页样式");
        }
        ShopCategoryPage row = getOrCreate();
        row.setStyle(style);
        row.setShareTitle(StringUtils.hasText(shareTitle) ? shareTitle.trim() : DEFAULT_SHARE_TITLE);
        if (row.getShareTitle().length() > 64) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "分享标题不能超过 64 字");
        }
        this.updateById(row);
        return row;
    }
}
