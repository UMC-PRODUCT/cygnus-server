package com.umc.product.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class EmailTemplatePropertiesBindingTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(EmailTemplatePropertiesConfiguration.class);

    @Test
    @DisplayName("기본 설정은 서비스 origin만 허용한다")
    void production_기본_origin을_확인한다() {
        contextRunner.run(context -> assertThat(context.getBean(EmailTemplateProperties.class).allowedActionOrigins())
            .containsExactly("https://university.neordinary.com"));
    }

    @Test
    @DisplayName("test profile override는 localhost를 명시적으로 추가한다")
    void test_profile_localhost_origin을_확인한다() {
        contextRunner
            .withPropertyValues(
                "app.notification.email.template.allowed-action-origins[0]=https://university.neordinary.com",
                "app.notification.email.template.allowed-action-origins[1]=http://localhost"
            )
            .run(context -> assertThat(context.getBean(EmailTemplateProperties.class).allowedActionOrigins())
                .containsExactly("https://university.neordinary.com", "http://localhost"));
    }

    @Test
    @DisplayName("origin 설정은 외부 list 변경으로부터 immutable snapshot을 유지한다")
    void origin_list_snapshot을_보장한다() {
        List<String> origins = new ArrayList<>(List.of(" https://university.neordinary.com "));
        EmailTemplateProperties properties = new EmailTemplateProperties(origins);
        origins.add("http://evil.test");

        assertThat(properties.allowedActionOrigins()).containsExactly("https://university.neordinary.com");
        assertThat(properties.allowedActionOrigins()).isUnmodifiable();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(EmailTemplateProperties.class)
    static class EmailTemplatePropertiesConfiguration {
    }
}
