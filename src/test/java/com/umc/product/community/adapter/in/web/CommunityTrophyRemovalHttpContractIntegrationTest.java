package com.umc.product.community.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.umc.product.support.IntegrationTestSupport;

@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "springdoc.api-docs.enabled=true")
@DisplayName("Community Trophy HTTP 계약 통합 테스트")
class CommunityTrophyRemovalHttpContractIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Test
    @DisplayName("Trophy GET/POST API와 OpenAPI handler mapping이 모두 사라진다")
    void trophy_http_and_openapi_mappings_are_absent() throws Exception {
        // Given: Trophy API가 제거된 전체 Spring MVC 런타임
        Map<?, ?> handlerMethods = requestMappingHandlerMapping.getHandlerMethods();

        // When: 기존 GET/POST endpoint를 호출하고 등록된 handler를 검사한다
        // Then: 두 HTTP 메서드는 404이며 OpenAPI에 노출될 mapping도 없다
        mockMvc.perform(get("/api/v1/trophies"))
            .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/trophies"))
            .andExpect(status().isNotFound());
        assertThat(handlerMethods.keySet().stream()
            .map(Object::toString)
            .filter(mapping -> mapping.contains("/api/v1/trophies"))
            .toList())
            .as("Trophy handler mappings")
            .isEmpty();
    }

    @Test
    @DisplayName("OpenAPI JSON에 Trophy path가 노출되지 않는다")
    void openapi_json_does_not_expose_trophy_paths() throws Exception {
        // Given: 테스트 프로필에서 OpenAPI JSON을 활성화한 전체 Spring MVC 런타임
        // When: 실제 OpenAPI 문서 endpoint를 조회한다
        String openApiJson = mockMvc.perform(get("/docs-json"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        // Then: 제거된 Trophy API path는 OpenAPI paths에도 없어야 한다
        var openApi = objectMapper.readTree(openApiJson);
        assertThat(openApi.path("paths").has("/api/v1/trophies"))
            .isFalse();
        assertThat(openApi.findValuesAsText("operationId"))
            .doesNotContain("TROPHY-001", "TROPHY-101");
    }

    @Test
    @DisplayName("게시글/WeeklyBestWorkbook/Certificate handler mapping은 보존된다")
    void post_weekly_best_workbook_and_certificate_http_mappings_are_preserved() {
        // Given/When: 전체 Spring MVC handler mapping을 읽는다
        // Then: Trophy와 독립적인 게시글/WeeklyBestWorkbook/Certificate API는 등록되어 있다
        assertThat(handlerMethodsAsText()
            .stream()
            .anyMatch(mapping -> mapping.contains("/api/v1/posts")))
            .isTrue();
        assertThat(handlerMethodsAsText()
            .stream()
            .anyMatch(mapping -> mapping.contains("/api/v2/curriculums/challenger-workbooks/weekly-best")))
            .isTrue();
        assertThat(handlerMethodsAsText()
            .stream()
            .anyMatch(mapping -> mapping.contains("/api/v1/certificates")))
            .isTrue();
    }

    private List<String> handlerMethodsAsText() {
        return requestMappingHandlerMapping.getHandlerMethods().keySet().stream()
            .map(Object::toString)
            .toList();
    }
}
