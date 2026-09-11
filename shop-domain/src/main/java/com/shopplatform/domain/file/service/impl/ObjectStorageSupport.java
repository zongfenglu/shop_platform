package com.shopplatform.domain.file.service.impl;

import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.setting.entity.StoreOperationSetting;
import com.shopplatform.framework.crypto.AesGcmEncryptor;

final class ObjectStorageSupport {

    private ObjectStorageSupport() {
    }

    static String required(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, label + "未配置");
        }
        return value.trim();
    }

    static String accessKeyId(StoreOperationSetting setting, AesGcmEncryptor encryptor) {
        return decryptRequired(setting.getUploadAccessKeyIdEncrypted(), "AccessKey ID", encryptor);
    }

    static String accessKeySecret(StoreOperationSetting setting, AesGcmEncryptor encryptor) {
        return decryptRequired(setting.getUploadAccessKeySecretEncrypted(), "AccessKey Secret", encryptor);
    }

    static String publicUrl(String configuredDomain, String defaultDomain, String objectKey) {
        String domain = configuredDomain == null || configuredDomain.isBlank()
                ? defaultDomain : configuredDomain.trim();
        while (domain.endsWith("/")) {
            domain = domain.substring(0, domain.length() - 1);
        }
        return domain + "/" + objectKey;
    }

    static String withoutScheme(String value) {
        return value.replaceFirst("^https?://", "").replaceAll("/+$", "");
    }

    private static String decryptRequired(String encrypted, String label, AesGcmEncryptor encryptor) {
        return encryptor.decrypt(required(encrypted, label));
    }
}
