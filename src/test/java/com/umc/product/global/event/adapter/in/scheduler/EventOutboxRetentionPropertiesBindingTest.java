package com.umc.product.global.event.adapter.in.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class EventOutboxRetentionPropertiesBindingTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withInitializer(new ConfigDataApplicationContextInitializer())
        .withUserConfiguration(RetentionPropertiesConfiguration.class);

    @Test
    @DisplayName("application.yml의 terminal payload 보존 기본값을 바인딩한다")
    void testCase001() {
        contextRunner.run(context -> {
            EventOutboxRetentionProperties properties = context.getBean(EventOutboxRetentionProperties.class);

            assertThat(properties.publishedPayloadRetention()).isEqualTo(Duration.ofHours(24));
            assertThat(properties.failedPayloadRetention()).isEqualTo(Duration.ofDays(30));
            assertThat(properties.batchSize()).isEqualTo(500);
            assertThat(properties.maxBatchesPerRun()).isEqualTo(20);
        });
    }

    @Test
    @DisplayName("실행당 최대 batch 설정은 두 terminal 상태를 위해 2 이상이어야 한다")
    void testCase002() {
        contextRunner
            .withPropertyValues("app.event-outbox.retention.max-batches-per-run=1")
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .hasRootCauseInstanceOf(IllegalArgumentException.class)
                    .rootCause()
                    .hasMessageContaining("최대 batch 수는 2 이상");
            });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(EventOutboxRetentionProperties.class)
    static class RetentionPropertiesConfiguration {
    }
}
