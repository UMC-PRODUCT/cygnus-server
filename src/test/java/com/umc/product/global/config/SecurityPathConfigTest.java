package com.umc.product.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;

class SecurityPathConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withInitializer(new ConfigDataApplicationContextInitializer());

    @Test
    @DisplayName("문서 공개 경로는 Scalar와 AsyncAPI 및 문서 카탈로그에 필요한 경로만 포함한다")
    void documentationPathsExposeScalarAndCatalogOnly() {
        assertThat(SecurityPathConfig.ASYNCAPI_ENTRY_PATH).isEqualTo("/docs/asyncapi");
        assertThat(SecurityPathConfig.ASYNCAPI_ENTRY_SLASH_PATH).isEqualTo("/docs/asyncapi/");
        assertThat(SecurityPathConfig.ASYNCAPI_HTML_PATH).isEqualTo("/docs/asyncapi.html");
        assertThat(SecurityPathConfig.ASYNCAPI_DOCUMENT_PATH).isEqualTo("/docs/asyncapi.yaml");
        assertThat(SecurityPathConfig.DOCUMENTATION_PATHS)
            .contains(
                "/docs",
                "/docs/",
                "/docs/**",
                "/docs-json",
                "/docs-json/**",
                "/webjars/markdown-it/**",
                "/umc-logo.svg"
            )
            .doesNotContain(
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/v3/api-docs/**",
                "/swagger-resources/**",
                "/webjars/**",
                "/webjars/swagger-ui/**"
            );
    }

    @Test
    @DisplayName("GraphQL 파일럿 경로는 공개 접근과 로깅 제외 대상이다")
    void graphqlPilotPathsArePublicAndExcludedFromLogging() {
        assertThat(SecurityPathConfig.SECURITY_PERMIT_ALL_PATHS)
            .contains(
                "/graphql",
                "/graphiql",
                "/graphiql/**"
            );
        assertThat(SecurityPathConfig.loggingExcludedPaths())
            .contains(
                "/graphql",
                "/graphiql",
                "/graphiql/**"
            );
    }

    @Test
    @DisplayName("Swagger 경로는 인증 여부와 무관하게 차단 대상이다")
    void swaggerPathsAreBlockedExplicitly() {
        assertThat(SecurityPathConfig.SWAGGER_BLOCKED_PATHS)
            .contains(
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/v3/api-docs",
                "/v3/api-docs/**",
                "/swagger-resources/**",
                "/webjars/swagger-ui/**"
            )
            .doesNotContain(
                "/docs",
                "/docs/**",
                "/docs-json",
                "/docs-json/**",
                "/webjars/markdown-it/**"
            );
    }

    @Test
    @DisplayName("Springdoc은 Swagger UI를 끄고 Scalar가 사용할 OpenAPI JSON만 제공한다")
    void springdocDisablesSwaggerUiAndKeepsOpenApiJson() {
        YamlPropertiesFactoryBean yamlPropertiesFactoryBean = new YamlPropertiesFactoryBean();
        yamlPropertiesFactoryBean.setResources(new ClassPathResource("application.yml"));

        Properties properties = yamlPropertiesFactoryBean.getObject();

        assertThat(properties)
            .containsEntry("springdoc.swagger-ui.enabled", Boolean.FALSE)
            .containsEntry("springdoc.api-docs.path", "/docs-json")
            .containsEntry("springdoc.api-docs.enabled", "${OPENAPI_ENABLE:${SWAGGER_ENABLE:false}}")
            .containsEntry("spring.graphql.http.path", "/graphql")
            .containsEntry("management.metrics.distribution.percentiles-histogram.http.server.requests", Boolean.TRUE)
            .containsEntry("management.metrics.distribution.percentiles-histogram.graphql.request", Boolean.TRUE)
            .containsEntry("management.metrics.distribution.percentiles-histogram.graphql.datafetcher", Boolean.TRUE)
            .containsEntry("management.metrics.distribution.percentiles-histogram.graphql.dataloader", Boolean.TRUE)
            .containsEntry("management.metrics.enable.graphql", Boolean.TRUE);
    }

    @Test
    @DisplayName("alpha 프로필은 GraphiQL을 기본 활성화한다")
    void alphaProfileEnablesGraphiqlByDefault() {
        contextRunner
            .withPropertyValues("spring.profiles.active=alpha")
            .run(context -> assertThat(context.getEnvironment().getProperty("spring.graphql.graphiql.enabled"))
                .isEqualTo("true"));
    }

    @Test
    @DisplayName("local 프로필은 GraphiQL을 기본 활성화한다")
    void localProfileEnablesGraphiqlByDefault() {
        contextRunner
            .withPropertyValues("spring.profiles.active=local")
            .run(context -> assertThat(context.getEnvironment().getProperty("spring.graphql.graphiql.enabled"))
                .isEqualTo("true"));
    }

    @Test
    @DisplayName("maintenance allowlist는 SSO 공개 진입 경로를 명시적으로 포함한다")
    void maintenanceAllowlistContainsSsoPublicPaths() {
        assertThat(SecurityPathConfig.MAINTENANCE_ALWAYS_ALLOW_PATHS)
            .contains(
                "/api/v1/auth/sso/oauth/**",
                "/api/v1/auth/sso/**"
            );
    }
}
