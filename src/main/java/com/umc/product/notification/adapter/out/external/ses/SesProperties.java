package com.umc.product.notification.adapter.out.external.ses;

import java.time.Duration;

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
public record SesProperties(
    @NotBlank String region,
    @NotBlank String accessKeyId,
    @NotBlank String secretAccessKey,
    String configurationSet,
    Duration apiCallTimeout,
    Duration apiCallAttemptTimeout
) {

    private static final Duration DEFAULT_API_CALL_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration DEFAULT_API_CALL_ATTEMPT_TIMEOUT = Duration.ofSeconds(10);

    public SesProperties {
        apiCallTimeout = apiCallTimeout == null ? DEFAULT_API_CALL_TIMEOUT : apiCallTimeout;
        apiCallAttemptTimeout = apiCallAttemptTimeout == null
            ? DEFAULT_API_CALL_ATTEMPT_TIMEOUT
            : apiCallAttemptTimeout;
        if (apiCallTimeout.compareTo(Duration.ZERO) <= 0) {
            throw new IllegalArgumentException("SES api call timeout은 0보다 커야 합니다.");
        }
        if (apiCallAttemptTimeout.compareTo(Duration.ZERO) <= 0) {
            throw new IllegalArgumentException("SES api call attempt timeout은 0보다 커야 합니다.");
        }
        if (apiCallAttemptTimeout.compareTo(apiCallTimeout) > 0) {
            throw new IllegalArgumentException("SES api call attempt timeout은 api call timeout을 초과할 수 없습니다.");
        }
    }

    public boolean hasStaticCredentials() {
        return accessKeyId != null && !accessKeyId.isBlank()
            && secretAccessKey != null && !secretAccessKey.isBlank();
    }

    public boolean hasConfigurationSet() {
        return configurationSet != null && !configurationSet.isBlank();
    }

}
