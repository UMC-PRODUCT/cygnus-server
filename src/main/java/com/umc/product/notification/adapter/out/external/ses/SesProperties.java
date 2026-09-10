package com.umc.product.notification.adapter.out.external.ses;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * AWS SES v2 인프라 설정.
 *
 * <p>{@code accessKeyId}/{@code secretAccessKey}가 비어 있으면 DefaultCredentialsProvider 체인
 * (EC2 Instance Profile / IRSA / 환경변수 등)으로 폴백한다. S3 자격증명 해석 패턴과 동일하다.
 *
 * <p>{@code configurationSet}은 운영 환경에서 바운스/컴플레인트 트래킹을 활성화할 때만 주입한다.
 * 미설정 시 {@link SesEmailAdapter}는 SendEmail 요청에 해당 필드를 붙이지 않는다.
 *
 * <p>발신자(From) 식별자는 비즈니스 식별자이므로 application 레이어의
 * {@code EmailSenderProperties}로 분리되어 있다.
 */
@Validated
@ConfigurationProperties(prefix = "app.notification.email.ses")
@ConditionalOnProperty(prefix = "app.notification.email", name = "provider", havingValue = "ses", matchIfMissing = true)
public record SesProperties(
    @NotBlank String region,
    @NotBlank String accessKeyId,
    @NotBlank String secretAccessKey,
    String configurationSet
) {

    public boolean hasStaticCredentials() {
        return accessKeyId != null && !accessKeyId.isBlank()
            && secretAccessKey != null && !secretAccessKey.isBlank();
    }

    public boolean hasConfigurationSet() {
        return configurationSet != null && !configurationSet.isBlank();
    }
}
