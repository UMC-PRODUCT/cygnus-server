package com.umc.product.storage.adapter.out.s3;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.umc.product.storage.domain.exception.StorageErrorCode;
import com.umc.product.storage.domain.exception.StorageException;

import lombok.extern.slf4j.Slf4j;

/**
 * AWS S3 및 CloudFront 설정 프로퍼티
 */
@ConfigurationProperties(prefix = "storage.s3")
@Slf4j
public record S3StorageProperties(
    String bucketName,
    String region,
    String accessKeyId,
    String secretAccessKey,
    CloudFront cloudfront
) {
    public record CloudFront(
        String distributionDomain,
        boolean enabled,
        String keyPairId,
        String privateKey,
        String privateKeyParameterName
    ) {
        public CloudFront {
            if (enabled) {
                if (!hasText(keyPairId) || (!hasText(privateKey) && !hasText(privateKeyParameterName))) {
                    log.warn("CloudFront가 활성화되었으나 Signed URL 생성을 위한 Key가 누락되어 있습니다.");
                }

                if (!hasText(distributionDomain)) {
                    throw new StorageException(StorageErrorCode.NO_ENV_KEYS);
                }
            }
        }

        private static boolean hasText(String value) {
            return value != null && !value.isBlank();
        }
    }
}
