package com.umc.product.storage.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class FileCleanupPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withInitializer(new ConfigDataApplicationContextInitializer())
        .withUserConfiguration(TestConfig.class);

    @Test
    @DisplayName("repository application 설정은 cleanup을 fail-safe 기본값으로 bind한다")
    void repository_cleanup_기본값을_bind한다() {
        contextRunner.run(context -> {
            FileCleanupProperties properties = context.getBean(FileCleanupProperties.class);

            assertThat(properties.enabled()).isFalse();
            assertThat(properties.pollInterval()).isEqualTo(Duration.ofMinutes(1));
            assertThat(properties.pendingRetention()).isEqualTo(Duration.ofHours(24));
            assertThat(properties.unreferencedRetention()).isEqualTo(Duration.ofHours(168));
            assertThat(properties.claimTimeout()).isEqualTo(Duration.ofMinutes(15));
            assertThat(properties.batchSize()).isEqualTo(100);
            assertThat(properties.maxAttempts()).isEqualTo(10);
            assertThat(properties.initialBackoff()).isEqualTo(Duration.ofMinutes(1));
            assertThat(properties.maxBackoff()).isEqualTo(Duration.ofHours(6));
        });
    }

    @Test
    @DisplayName("exponential backoff는 설정된 최댓값에서 cap한다")
    void backoff는_max에서_cap한다() {
        contextRunner.run(context -> {
            FileCleanupProperties properties = context.getBean(FileCleanupProperties.class);

            assertThat(properties.backoffForAttempt(1)).isEqualTo(Duration.ofMinutes(1));
            assertThat(properties.backoffForAttempt(3)).isEqualTo(Duration.ofMinutes(4));
            assertThat(properties.backoffForAttempt(20)).isEqualTo(Duration.ofHours(6));
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(FileCleanupProperties.class)
    static class TestConfig {
    }
}
