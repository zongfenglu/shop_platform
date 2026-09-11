package com.shopplatform.domain.file.service.impl;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.file.service.ObjectStorageUploader;
import com.shopplatform.domain.setting.entity.StoreOperationSetting;
import com.shopplatform.framework.crypto.AesGcmEncryptor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Component
public class AliyunOssStorageUploader implements ObjectStorageUploader {

    private final AesGcmEncryptor encryptor;

    public AliyunOssStorageUploader(AesGcmEncryptor encryptor) {
        this.encryptor = encryptor;
    }

    @Override
    public String provider() {
        return "aliyun_oss";
    }

    @Override
    public String upload(StoreOperationSetting setting, String objectKey, MultipartFile file, String contentType) {
        String bucket = ObjectStorageSupport.required(setting.getUploadBucket(), "Bucket");
        String region = ObjectStorageSupport.required(setting.getUploadRegion(), "Region");
        String endpoint = ObjectStorageSupport.required(setting.getUploadEndpoint(), "Endpoint");
        String accessKeyId = ObjectStorageSupport.accessKeyId(setting, encryptor);
        String accessKeySecret = ObjectStorageSupport.accessKeySecret(setting, encryptor);

        OSS client = null;
        try {
            ClientBuilderConfiguration configuration = new ClientBuilderConfiguration();
            configuration.setSignatureVersion(SignVersion.V4);
            client = OSSClientBuilder.create()
                    .endpoint(endpoint)
                    .credentialsProvider(new DefaultCredentialProvider(accessKeyId, accessKeySecret))
                    .region(region)
                    .clientConfiguration(configuration)
                    .build();
            try (InputStream input = file.getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(contentType);
            client.putObject(new PutObjectRequest(bucket, objectKey, input, metadata));
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "阿里云 OSS 上传失败");
        } finally {
            if (client != null) client.shutdown();
        }

        String defaultDomain = "https://" + bucket + "." + ObjectStorageSupport.withoutScheme(endpoint);
        return ObjectStorageSupport.publicUrl(setting.getUploadDomain(), defaultDomain, objectKey);
    }
}
