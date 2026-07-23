package com.umc.product.global.config;

import static org.mockito.BDDMockito.willReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.support.IntegrationTestSupport;

@DisplayName("SecurityConfig 통합 테스트")
class SecurityConfigIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("docs 진입 경로는 Scalar HTML로 리다이렉트한다")
    void docsEntryRedirectsToScalar() throws Exception {
        mockMvc.perform(get("/docs"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/docs/scalar.html"));
    }

    @Test
    @DisplayName("AsyncAPI 문서 진입 경로는 전용 HTML로 리다이렉트한다")
    void asyncApiEntryRedirectsToDocumentationHtml() throws Exception {
        mockMvc.perform(get("/docs/asyncapi"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/docs/asyncapi.html"));

        mockMvc.perform(get("/docs/asyncapi/"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/docs/asyncapi.html"));
    }

    @Test
    @DisplayName("AsyncAPI UI와 원본 YAML은 인증 없이 제공한다")
    void asyncApiDocumentationIsServedWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/docs/asyncapi.html"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("AsyncApiStandalone.render")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("/docs/asyncapi.yaml")));

        mockMvc.perform(get("/docs/asyncapi.yaml"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("asyncapi: 3.0.0")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString(
                "/app/community/threads/{threadId}/messages"
            )));

        mockMvc.perform(get("/docs/community-thread.html"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString(
                "Community Thread WebSocket Console"
            )));

        mockMvc.perform(get("/docs/community-thread-websocket.html"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Apollo Sandbox 문서는 인증 없이 정적 HTML로 제공한다")
    void apolloSandboxDocumentationIsServedWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/docs/apollo-sandbox.html"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("EmbeddedSandbox")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("initialEndpoint: '/graphql'")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString(
                "https://embeddable-sandbox.cdn.apollographql.com"
            )));
    }

    @Test
    @DisplayName("인증된 요청이어도 Swagger UI 경로는 접근할 수 없다")
    void authenticatedRequestCannotAccessSwaggerUi() throws Exception {
        String token = "swagger-block-token";
        willReturn(new com.umc.product.global.security.ParsedAccessToken(1L, List.of("USER"), null))
            .given(jwtTokenProvider)
            .parseAndValidateAccessToken(token);

        mockMvc.perform(get("/swagger-ui/index.html")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("인증된 요청이어도 기존 OpenAPI JSON 경로는 접근할 수 없다")
    void authenticatedRequestCannotAccessDefaultOpenApiJson() throws Exception {
        String token = "swagger-api-docs-block-token";
        willReturn(new com.umc.product.global.security.ParsedAccessToken(1L, List.of("USER"), null))
            .given(jwtTokenProvider)
            .parseAndValidateAccessToken(token);

        mockMvc.perform(get("/v3/api-docs")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
    }
}
