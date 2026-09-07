package com.shopplatform.domain.platform.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.platform.entity.PlatformSetting;
import com.shopplatform.domain.platform.mapper.PlatformSettingMapper;
import com.shopplatform.domain.platform.service.PlatformSettingService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PlatformSettingServiceImpl extends ServiceImpl<PlatformSettingMapper, PlatformSetting>
        implements PlatformSettingService {

    private final ObjectMapper objectMapper;

    public PlatformSettingServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Map<String, Object>> listValues(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return Map.of();
        }
        List<PlatformSetting> rows = this.list(
                Wrappers.<PlatformSetting>lambdaQuery().in(PlatformSetting::getSettingKey, keys));
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (PlatformSetting row : rows) {
            result.put(row.getSettingKey(), parse(row.getSettingValue()));
        }
        return result;
    }

    @Override
    public Map<String, Object> findValue(String settingKey) {
        PlatformSetting row = getByKey(settingKey);
        return row == null ? Map.of() : parse(row.getSettingValue());
    }

    @Override
    public PlatformSetting saveValue(String settingKey, Map<String, Object> value) {
        if (!StringUtils.hasText(settingKey)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "配置 key 不能为空");
        }
        if (!settingKey.matches("^[a-z][a-z0-9_\\-]{1,63}$")) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "配置 key 仅支持小写字母、数字、下划线和中划线");
        }
        PlatformSetting row = getByKey(settingKey);
        if (row == null) {
            row = new PlatformSetting();
            row.setSettingKey(settingKey);
        }
        row.setSettingValue(write(value));
        this.saveOrUpdate(row);
        return row;
    }

    private PlatformSetting getByKey(String settingKey) {
        if (!StringUtils.hasText(settingKey)) {
            return null;
        }
        return this.getOne(Wrappers.<PlatformSetting>lambdaQuery()
                .eq(PlatformSetting::getSettingKey, settingKey));
    }

    private Map<String, Object> parse(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String write(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存配置失败");
        }
    }
}
