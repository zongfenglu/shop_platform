package com.shopplatform.domain.file.service.impl;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.file.service.ObjectStorageUploader;
import com.shopplatform.domain.setting.entity.StoreOperationSetting;
import com.shopplatform.framework.crypto.AesGcmEncryptor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Component
public class TencentCosStorageUploader implements ObjectStorageUploader {

    private final AesGcmEncryptor encryptor;

    public TencentCosStorageUploader(AesGcmEncryptor encryptor) {
        this.encryptor = encryptor;
    }

    @Override
    public String provider() {
        return "tencent_cos";
    }

    @Override
    public String upload(StoreOperationSetting setting, String objectKey, MultipartFile file, String contentType) {
        String bucket = ObjectStorageSupport.required(setting.getUploadBucket(), "Bucket");
        String regionName = ObjectStorageSupport.required(setting.getUploadRegion(), "Region");
        String accessKeyId = ObjectStorageSupport.accessKeyId(setting, encryptor);
        String accessKeySecret = ObjectStorageSupport.accessKeySecret(setting, encryptor);
        COSClient client = null;
        try {
            COSCredentials credentials = new BasicCOSCredentials(accessKeyId, accessKeySecret);
            ClientConfig configuration = new ClientConfig(new Region(regionName));
            configuration.setHttpProtocol(HttpProtocol.https);
            client = new COSClient(credentials, configuration);
            try (InputStream input = file.getInputStream()) {
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentLength(file.getSize());
                metadata.setContentType(contentType);
                client.putObject(new PutObjectRequest(bucket, objectKey, input, metadata));
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "腾讯云 COS 上传失败");
        } finally {
            if (client != null) client.shutdown();
        }

        String defaultDomain = "https://" + bucket + ".cos." + regionName + ".myqcloud.com";
        return ObjectStorageSupport.publicUrl(setting.getUploadDomain(), defaultDomain, objectKey);
    }
}
