package com.shopplatform.domain.diy.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.shop.service.PackageFeatureChecker;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 装修页面内容校验：结构合法性（items数组、组件类型白名单）+ 套餐功能位校验。
 * 发布（publish）和保存草稿（updateDraft）前均调用，保证草稿和已发布内容都不会出现套餐锁定组件。
 */
@Component
public class DiyPageContentValidator {

    private final ObjectMapper objectMapper;
    private final PackageFeatureChecker packageFeatureChecker;

    public DiyPageContentValidator(ObjectMapper objectMapper, PackageFeatureChecker packageFeatureChecker) {
        this.objectMapper = objectMapper;
        this.packageFeatureChecker = packageFeatureChecker;
    }

    public void validate(String contentJson, Long shopId) {
        JsonNode root;
        try {
            root = objectMapper.readTree(contentJson);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.DIY_PAGE_DATA_INVALID);
        }
        JsonNode items = root == null ? null : root.get("items");
        if (items == null || !items.isArray()) {
            throw new BusinessException(ErrorCode.DIY_PAGE_DATA_INVALID);
        }
        for (JsonNode item : items) {
            JsonNode typeNode = item.get("type");
            if (typeNode == null || typeNode.asText().isEmpty()) {
                throw new BusinessException(ErrorCode.DIY_PAGE_DATA_INVALID);
            }
            Optional<DiyComponentType> componentType = DiyComponentType.fromType(typeNode.asText());
            if (componentType.isEmpty()) {
                throw new BusinessException(ErrorCode.DIY_PAGE_DATA_INVALID);
            }
            String requiredMenu = componentType.get().getRequiredMenu();
            if (requiredMenu != null) {
                packageFeatureChecker.requireMenu(shopId, requiredMenu);
            }
        }
    }
}
