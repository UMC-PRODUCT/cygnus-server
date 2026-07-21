package com.umc.product.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.models.OpenAPI;

class OpenApiConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(OpenApiConfig.class)
        .withBean(ObjectMapper.class, ObjectMapper::new);

    @Test
    @DisplayName("빌드 정보가 있으면 OpenAPI 버전에 빌드 버전을 사용한다")
    void usesBuildVersionWhenBuildPropertiesExists() {
        contextRunner
            .withBean(BuildProperties.class, () -> buildProperties("2.0.0"))
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context.getBean(OpenAPI.class).getInfo().getVersion()).isEqualTo("2.0.0");
            });
    }

    @Test
    @DisplayName("빌드 정보가 없으면 OpenAPI 버전에 local을 사용한다")
    void usesLocalVersionWhenBuildPropertiesIsMissing() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(OpenAPI.class).getInfo().getVersion()).isEqualTo("local");
        });
    }

    @Test
    @DisplayName("빌드 버전이 비어 있으면 OpenAPI 버전에 local을 사용한다")
    void usesLocalVersionWhenBuildVersionIsBlank() {
        contextRunner
            .withBean(BuildProperties.class, () -> buildProperties(""))
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context.getBean(OpenAPI.class).getInfo().getVersion()).isEqualTo("local");
            });
    }

    private BuildProperties buildProperties(String version) {
        Properties properties = new Properties();
        properties.setProperty("version", version);
        return new BuildProperties(properties);
    }
}
