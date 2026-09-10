package com.umc.product.notification.adapter.out.external.ses;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;

/**
 * SES v2 클라이언트 빈 등록.
 *
 * <p>자격증명 해석 정책은 {@code S3Config} 와 동일하게 static 우선, 미설정 시 DefaultCredentialsProvider 폴백.
 */
@Validated
@Configuration
@ConditionalOnProperty(prefix = "app.notification.email", name = "provider", havingValue = "ses", matchIfMissing = true)
@EnableConfigurationProperties(SesProperties.class)
public class SesEmailConfig {

    @Bean
    public SesV2Client sesV2Client(SesProperties properties) {
        return SesV2Client.builder()
            .region(Region.of(properties.region()))
            .credentialsProvider(resolveCredentials(properties))
            .build();
    }

    private AwsCredentialsProvider resolveCredentials(SesProperties properties) {
        if (properties.hasStaticCredentials()) {
            return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.accessKeyId(), properties.secretAccessKey())
            );
        }
        return DefaultCredentialsProvider.builder().build();
    }
}
