package com.umc.product.global.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExternalApiCallUsagePolicyTest {

    private static final List<Path> EXTERNAL_CALL_ADAPTERS = List.of(
        Path.of("src/main/java/com/umc/product/authentication/adapter/out/external/AppleTokenVerifier.java"),
        Path.of("src/main/java/com/umc/product/authentication/adapter/out/external/GoogleTokenVerifier.java"),
        Path.of("src/main/java/com/umc/product/authentication/adapter/out/external/KakaoTokenVerifier.java"),
        Path.of("src/main/java/com/umc/product/notification/adapter/out/external/ses/SesEmailAdapter.java"),
        Path.of("src/main/java/com/umc/product/notification/adapter/out/external/webhook/DiscordWebhookAdapter.java"),
        Path.of("src/main/java/com/umc/product/notification/adapter/out/external/webhook/SlackWebhookAdapter.java"),
        Path.of("src/main/java/com/umc/product/notification/adapter/out/external/webhook/TelegramWebhookAdapter.java")
    );

    @Test
    @DisplayName("주요 외부 호출 어댑터는 구조화된 외부 API 호출 로그를 남긴다")
    void external_call_adapters_use_external_api_call_logger() throws IOException {
        List<String> violations = EXTERNAL_CALL_ADAPTERS.stream()
            .filter(path -> !usesExternalApiCallLogger(path))
            .map(Path::toString)
            .toList();

        assertThat(violations).isEmpty();
    }

    private boolean usesExternalApiCallLogger(Path path) {
        try {
            return Files.readString(path).contains("ExternalApiCallLogger.measure(");
        } catch (IOException e) {
            throw new IllegalStateException("외부 API 호출 정책 테스트 파일 읽기 실패: " + path, e);
        }
    }
}
