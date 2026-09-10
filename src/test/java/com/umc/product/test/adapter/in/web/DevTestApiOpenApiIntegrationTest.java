package com.umc.product.test.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.fasterxml.jackson.databind.JsonNode;
import com.umc.product.support.IntegrationTestSupport;

@TestPropertySource(properties = {
    "springdoc.api-docs.enabled=true",
    "app.seed.enabled=true",
    "app.test-api.enabled=true",
    "app.environment=dev"
})
@DisplayName("개발 환경 테스트 API의 OpenAPI 인증 계약")
class DevTestApiOpenApiIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("공개 테스트 API는 개발 서버의 Basic 인증만 요구한다")
    void 공개_테스트_API는_Basic_인증을_명시한다() throws Exception {
        // given
        JsonNode openApi = readOpenApi();
        JsonNode expectedSecurity = objectMapper.readTree("[{\"Test API Basic Auth\":[]}]");

        // when
        JsonNode paths = openApi.path("paths");
        JsonNode basicAuth = openApi.path("components").path("securitySchemes").path("Test API Basic Auth");

        // then
        assertThat(paths.path("/test/seed/members").path("post").path("security")).isEqualTo(expectedSecurity);
        assertThat(paths.path("/test/token/access").path("get").path("security")).isEqualTo(expectedSecurity);
        assertThat(paths.path("/test/health-check").path("get").path("security")).isEqualTo(expectedSecurity);
        assertThat(basicAuth.path("type").asText()).isEqualTo("http");
        assertThat(basicAuth.path("scheme").asText()).isEqualTo("basic");
    }

    @Test
    @DisplayName("회원 조회와 인증 확인 테스트 API는 기존 Access Token 인증을 유지한다")
    void 회원_API와_인증_확인_API는_JWT_인증을_유지한다() throws Exception {
        // given
        JsonNode openApi = readOpenApi();
        JsonNode expectedSecurity = objectMapper.readTree("[{\"Access Token\":[]}]");

        // when
        JsonNode globalSecurity = openApi.path("security");

        // then
        assertThat(globalSecurity).isEqualTo(expectedSecurity);
        for (String path : List.of("/api/v1/member/me", "/test/check-authenticated")) {
            JsonNode operation = openApi.path("paths").path(path).path("get");
            assertThat(operation.isMissingNode()).as(path).isFalse();
            JsonNode effectiveSecurity = operation.has("security") ? operation.path("security") : globalSecurity;
            assertThat(effectiveSecurity).as(path).isEqualTo(expectedSecurity);
        }
    }

    @Test
    @DisplayName("JWT 없는 시딩 요청은 인증 오류 대신 입력값 검증까지 도달한다")
    void 익명_시딩_요청은_잘못된_건수에_검증_오류를_반환한다() throws Exception {
        // given
        String invalidRequest = """
            {"count": -1, "force": false}
            """;

        // when & then
        mockMvc.perform(post("/test/seed/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
            .andExpect(status().isBadRequest())
            .andExpect(result -> assertThat(result.getResolvedException())
                .isInstanceOf(MethodArgumentNotValidException.class));
    }

    private JsonNode readOpenApi() throws Exception {
        String openApiJson = mockMvc.perform(get("/docs-json"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return objectMapper.readTree(openApiJson);
    }
}
