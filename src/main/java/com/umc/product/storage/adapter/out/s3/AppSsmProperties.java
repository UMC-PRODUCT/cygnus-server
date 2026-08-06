package com.umc.product.storage.adapter.out.s3;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ssm")
public record AppSsmProperties(
    String region,
    String accessKeyId,
    String secretAccessKey
) {
    public boolean hasStaticCredentials() {
        return accessKeyId != null && !accessKeyId.isBlank()
            && secretAccessKey != null && !secretAccessKey.isBlank();
    }

    public boolean hasRegion() {
        return region != null && !region.isBlank();
    }
}
