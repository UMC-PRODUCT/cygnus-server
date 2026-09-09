package com.umc.product.global.config;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Configuration
public class OpenApiConfig {

    private static final String DEFAULT_API_VERSION = "local";

    private final String accessToken = "Access Token";
    private final ObjectProvider<BuildProperties> buildPropertiesProvider;

    @Bean
    public OpenAPI umcProductApi() {

        return new OpenAPI()
            .info(apiInfo())
            .servers(List.of(new Server().url("/").description("현재 접속 서버")))
            .components(securityComponents())
            .addSecurityItem(securityRequirement());
    }

    private Info apiInfo() {
        String version = apiVersion();

        String description = """
            #### 국내 최대 규모 대학생 개발 연합 동아리, University MakeUs Challenge

            ### UMC PRODUCT

            > *Focus on Growth, We Handle the Ops*

            UMC PRODUCT 서버팀이 제작하여 제공하는, UMC WEB & APP을 위한 API 입니다.

            ### Server Team Members

            - 중앙대학교 **하늘/박경운** [1st Lead, 2nd Lead]
            - 한양대학교 ERICA **와나/강하나** [1st, 2nd]
            - 동국대학교 **박박지현/박지현** [1st, 2nd]
            - 동국대학교 **갈래/김민서** [1st, 2nd]
            - 동덕여자대학교 **세니/박세은** [1st, 2nd]
            - 중앙대학교 **스읍/이예은** [1st, 2nd]
            - 한양대학교 ERICA **라미/권도희** [2nd]
            - 가천대학교 **우디/박성현** [2nd]
            - 한성대학교 **리버/이재원** [2nd]
            """;

        return new Info()
            .title("UMC PRODUCT API")
            .version(version)
            .description(description);
    }

    private String apiVersion() {
        return Optional.ofNullable(buildPropertiesProvider.getIfAvailable())
            .map(BuildProperties::getVersion)
            .filter(version -> !version.isBlank())
            .orElse(DEFAULT_API_VERSION);
    }

    @Bean
    public ModelResolver modelResolver(ObjectMapper objectMapper) {
        // Spring이 관리하는 ObjectMapper를 Swagger에 전달
        return new ModelResolver(objectMapper);
    }

    private Components securityComponents() {
        return new Components()
            .addSecuritySchemes(accessToken,
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("아래에 Access Token을 넣어주세요.")
            );
    }

    private SecurityRequirement securityRequirement() {
        return new SecurityRequirement()
            .addList(accessToken);
    }
}
