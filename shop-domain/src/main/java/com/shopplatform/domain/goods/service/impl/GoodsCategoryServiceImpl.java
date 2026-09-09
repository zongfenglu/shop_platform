package com.shopplatform.domain.goods.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.goods.entity.Goods;
import com.shopplatform.domain.goods.entity.GoodsCategory;
import com.shopplatform.domain.goods.mapper.GoodsCategoryMapper;
import com.shopplatform.domain.goods.service.GoodsCategoryService;
import com.shopplatform.domain.goods.service.GoodsService;
import com.shopplatform.framework.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class GoodsCategoryServiceImpl extends ServiceImpl<GoodsCategoryMapper, GoodsCategory>
        implements GoodsCategoryService {

    private final GoodsService goodsService;

    public GoodsCategoryServiceImpl(GoodsService goodsService) {
        this.goodsService = goodsService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GoodsCategory create(SaveCommand command) {
        Long parentId = normalizeParentId(command.parentId());
        if (parentId != 0L) {
            GoodsCategory parent = getByIdWithTenant(parentId);
            if (levelOf(parent) >= MAX_LEVEL) {
                throw new BusinessException(ErrorCode.PARAM_INVALID, "分类最多三级，不能再添加子分类");
            }
        }
        GoodsCategory category = new GoodsCategory();
        category.setShopId(TenantContext.getRequired());
        category.setParentId(parentId);
        applyFields(category, command, true);
        this.save(category);
        return category;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GoodsCategory update(Long id, SaveCommand command) {
        GoodsCategory category = getByIdWithTenant(id);
        applyFields(category, command, false);
        this.updateById(category);
        return category;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        getByIdWithTenant(id);
        long children = this.count(Wrappers.<GoodsCategory>lambdaQuery().eq(GoodsCategory::getParentId, id));
        if (children > 0) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "请先删除子分类");
        }
        long used = goodsService.count(Wrappers.<Goods>lambdaQuery()
                .apply("JSON_CONTAINS(category_ids, {0})", String.valueOf(id)));
        if (used > 0) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "仍有商品使用该分类，不能删除");
        }
        this.removeById(id);
    }

    private void applyFields(GoodsCategory category, SaveCommand command, boolean creating) {
        String name = command.name() == null ? "" : command.name().trim();
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "分类名称不能为空");
        }
        if (name.length() > 64) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "分类名称不能超过 64 字");
        }
        String image = command.image() == null ? "" : command.image().trim();
        if (image.length() > 255) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "分类图片地址过长");
        }
        category.setName(name);
        category.setImage(StringUtils.hasText(image) ? image : null);
        category.setSort(command.sort() == null ? 0 : command.sort());
        if (command.isShow() != null) {
            category.setIsShow(command.isShow());
        } else if (creating) {
            category.setIsShow(true);
        }
    }

    /** 从当前节点走到根，一级=1。 */
    private int levelOf(GoodsCategory category) {
        int level = 1;
        Long parentId = category.getParentId();
        while (parentId != null && parentId != 0L) {
            GoodsCategory parent = findParent(parentId);
            if (parent == null) {
                break;
            }
            level++;
            if (level > MAX_LEVEL) {
                return level;
            }
            parentId = parent.getParentId();
        }
        return level;
    }

    /**
     * 查父分类。不用裸 getById（绕过租户校验，ArchUnit 禁止）；也不用 getByIdWithTenant——
     * 父节点缺失（脏数据）时 levelOf 要宽容 break 而不是抛 404。lambdaQuery 走租户拦截器。
     */
    protected GoodsCategory findParent(Long parentId) {
        return lambdaQuery().eq(GoodsCategory::getId, parentId).one();
    }

    private static Long normalizeParentId(Long parentId) {
        return parentId == null ? 0L : parentId;
    }
}
