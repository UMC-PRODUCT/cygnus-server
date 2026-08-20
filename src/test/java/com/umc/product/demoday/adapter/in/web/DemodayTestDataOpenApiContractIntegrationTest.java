package com.umc.product.demoday.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;

import com.fasterxml.jackson.databind.JsonNode;
import com.umc.product.support.IntegrationTestSupport;

@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "springdoc.api-docs.enabled=true",
    "demoday.test-data-reset.enabled=true"
})
@DisplayName("데모데이 테스트 데이터 초기화 runtime OpenAPI 계약")
class DemodayTestDataOpenApiContractIntegrationTest extends IntegrationTestSupport {

    private static final String RESET_PATH = "/api/v1/demoday/admin/test-data";
    private static final String API_RESPONSE_SCHEMA = "DemodayTestDataResetApiResponse";
    private static final String RESPONSE_SCHEMA = "DemodayTestDataResetResponse";
    private static final List<String> COUNT_FIELDS = List.of(
        "deletedPolls",
        "deletedBooths",
        "deletedEntryCodes",
        "deletedStamps",
        "deletedVotes"
    );

    @Test
    @DisplayName("runtime OpenAPI는 초기화 DELETE 경로와 삭제 건수 응답 스키마를 노출한다")
    void runtimeOpenApiExposesResetContract() throws Exception {
        // given
        String openApiJson = mockMvc.perform(get("/docs-json"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        JsonNode openApi = objectMapper.readTree(openApiJson);

        // when
        JsonNode deleteOperation = openApi.path("paths").path(RESET_PATH).path("delete");
        JsonNode responseSchema = deleteOperation
            .path("responses")
            .path("200")
            .path("content")
            .path("application/json")
            .path("schema");
        JsonNode apiResponseProperties = openApi
            .path("components")
            .path("schemas")
            .path(API_RESPONSE_SCHEMA)
            .path("properties");
        JsonNode countProperties = openApi
            .path("components")
            .path("schemas")
            .path(RESPONSE_SCHEMA)
            .path("properties");

        // then
        assertThat(deleteOperation.isMissingNode()).isFalse();
        assertThat(deleteOperation.path("operationId").asText())
            .isEqualTo("resetDemodayTestData");
        assertThat(responseSchema.path("$ref").asText())
            .isEqualTo("#/components/schemas/" + API_RESPONSE_SCHEMA);
        assertThat(fieldNames(apiResponseProperties))
            .containsExactlyInAnyOrder("success", "code", "message", "result");
        assertThat(apiResponseProperties.path("success").path("type").asText()).isEqualTo("boolean");
        assertThat(apiResponseProperties.path("code").path("type").asText()).isEqualTo("string");
        assertThat(apiResponseProperties.path("message").path("type").asText()).isEqualTo("string");
        assertThat(apiResponseProperties.path("result").path("$ref").asText())
            .isEqualTo("#/components/schemas/" + RESPONSE_SCHEMA);
        assertThat(fieldNames(countProperties)).containsExactlyInAnyOrderElementsOf(COUNT_FIELDS);
        COUNT_FIELDS.forEach(field -> {
            assertThat(countProperties.path(field).path("type").asText()).isEqualTo("integer");
            assertThat(countProperties.path(field).path("format").asText()).isEqualTo("int32");
        });
    }

    private static List<String> fieldNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return List.copyOf(names);
    }
}
