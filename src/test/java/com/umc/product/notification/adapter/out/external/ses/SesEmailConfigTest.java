package com.umc.product.notification.adapter.out.external.ses;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.services.sesv2.SesV2Client;

@DisplayName("AWS SES v2 client 설정")
class SesEmailConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfiguration.class)
        .withPropertyValues(
            "app.notification.email.ses.region=ap-northeast-2",
            "app.notification.email.ses.access-key-id=access-key",
            "app.notification.email.ses.secret-access-key=secret-key"
        );

    @Test
    @DisplayName("timeout 미지정 시 api call 30초와 attempt 10초 기본값을 binding한다")
    void SES_timeout_기본값을_binding한다() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            SesProperties properties = context.getBean(SesProperties.class);
            assertThat(properties.apiCallTimeout()).isEqualTo(Duration.ofSeconds(30));
            assertThat(properties.apiCallAttemptTimeout()).isEqualTo(Duration.ofSeconds(10));
        });
    }

    @Test
    @DisplayName("설정된 positive timeout을 binding하고 AWS override configuration에 적용한다")
    void SES_timeout을_AWS_client_override에_적용한다() {
        contextRunner
            .withPropertyValues(
                "app.notification.email.ses.api-call-timeout=PT20S",
                "app.notification.email.ses.api-call-attempt-timeout=PT5S"
            )
            .run(context -> {
                SesProperties properties = context.getBean(SesProperties.class);
                assertThat(properties.apiCallTimeout()).isEqualTo(Duration.ofSeconds(20));
                assertThat(properties.apiCallAttemptTimeout()).isEqualTo(Duration.ofSeconds(5));
                try (SesV2Client client = new SesEmailConfig().sesV2Client(properties)) {
                    assertThat(client.serviceClientConfiguration().overrideConfiguration().apiCallTimeout())
                        .contains(Duration.ofSeconds(20));
                    assertThat(client.serviceClientConfiguration().overrideConfiguration().apiCallAttemptTimeout())
                        .contains(Duration.ofSeconds(5));
                }
            });
    }

    @Test
    @DisplayName("0 이하 api call timeout은 configuration validation에서 거부한다")
    void positive_api_call_timeout을_강제한다() {
        contextRunner
            .withPropertyValues("app.notification.email.ses.api-call-timeout=PT0S")
            .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("0 이하 api call attempt timeout은 configuration validation에서 거부한다")
    void positive_api_call_attempt_timeout을_강제한다() {
        contextRunner
            .withPropertyValues("app.notification.email.ses.api-call-attempt-timeout=PT0S")
            .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("attempt timeout이 call timeout보다 크면 configuration validation에서 거부한다")
    void attempt_timeout은_call_timeout을_초과할_수_없다() {
        contextRunner
            .withPropertyValues(
                "app.notification.email.ses.api-call-timeout=PT5S",
                "app.notification.email.ses.api-call-attempt-timeout=PT6S"
            )
            .run(context -> assertThat(context).hasFailed());
    }

    @ParameterizedTest
    @ValueSource(strings = {"PT5M", "PT6M"})
    @DisplayName("processing lease 이상의 api call timeout은 configuration startup에서 거부한다")
    void api_call_timeout은_processing_lease보다_짧아야_한다(String apiCallTimeout) {
        contextRunner
            .withPropertyValues("app.notification.email.ses.api-call-timeout=" + apiCallTimeout)
            .run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(SesProperties.class)
    static class PropertiesConfiguration {
    }
}
