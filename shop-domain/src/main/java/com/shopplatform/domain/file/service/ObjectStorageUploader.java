package com.shopplatform.domain.file.service;

import com.shopplatform.domain.setting.entity.StoreOperationSetting;
import org.springframework.web.multipart.MultipartFile;

/** 单个对象存储厂商的上传适配器。 */
public interface ObjectStorageUploader {

    String provider();

    /** 上传对象并返回浏览器可直接访问的绝对 URL。 */
    String upload(StoreOperationSetting setting, String objectKey, MultipartFile file, String contentType);
}
