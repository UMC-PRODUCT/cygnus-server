package com.umc.product.test.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import com.fasterxml.jackson.databind.JsonNode;
import com.umc.product.support.IntegrationTestSupport;

@TestPropertySource(properties = {
    "springdoc.api-docs.enabled=true",
    "app.seed.enabled=true",
    "app.test-api.enabled=true",
    "app.environment=local"
})
@DisplayName("로컬 환경 테스트 API의 OpenAPI 인증 계약")
class LocalTestApiOpenApiIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("공개 테스트 API는 인증을 요구하지 않고 회원 인증 API는 JWT를 유지한다")
    void 공개_테스트_API의_인증만_해제한다() throws Exception {
        // given
        String openApiJson = mockMvc.perform(get("/docs-json"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        JsonNode openApi = objectMapper.readTree(openApiJson);
        JsonNode expectedPublicSecurity = objectMapper.createArrayNode();
        JsonNode expectedJwtSecurity = objectMapper.readTree("[{\"Access Token\":[]}]");

        // when
        JsonNode paths = openApi.path("paths");
        JsonNode globalSecurity = openApi.path("security");

        // then
        assertThat(paths.path("/test/seed/members").path("post").path("security"))
            .isEqualTo(expectedPublicSecurity);
        assertThat(paths.path("/test/token/access").path("get").path("security"))
            .isEqualTo(expectedPublicSecurity);
        assertThat(paths.path("/test/health-check").path("get").path("security"))
            .isEqualTo(expectedPublicSecurity);
        assertThat(globalSecurity).isEqualTo(expectedJwtSecurity);
        for (String path : List.of("/api/v1/member/me", "/test/check-authenticated")) {
            JsonNode operation = paths.path(path).path("get");
            assertThat(operation.isMissingNode()).as(path).isFalse();
            JsonNode effectiveSecurity = operation.has("security") ? operation.path("security") : globalSecurity;
            assertThat(effectiveSecurity).as(path).isEqualTo(expectedJwtSecurity);
        }
    }
}
