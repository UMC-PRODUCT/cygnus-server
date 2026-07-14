package com.umc.product.authorization.application.service.policy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.global.config.JacksonConfig;

class GlobalObjectMapperPolicyIsolationTest {

    @DisplayName("전역 ObjectMapper는 기존의 관대한 역직렬화 동작을 유지한다")
    @Test
    void globalObjectMapperRemainsPermissive() throws Exception {
        // given
        ObjectMapper objectMapper = globalObjectMapper();

        // when
        LegacyPayload payload = objectMapper.readValue("{\"name\":\"legacy\",\"unknown\":true}", LegacyPayload.class);
        String duplicateValue = objectMapper.readTree("{\"name\":\"first\",\"name\":\"last\"}").path("name").asText();

        // then
        assertThat(payload.name()).isEqualTo("legacy");
        assertThat(duplicateValue).isEqualTo("last");
    }

    @DisplayName("전역 ObjectMapper는 숫자를 문자열로 직렬화하는 기존 설정을 유지한다")
    @Test
    void globalObjectMapperKeepsNumberSerializationSetting() throws Exception {
        // given
        ObjectMapper objectMapper = globalObjectMapper();

        // when
        String json = objectMapper.writeValueAsString(new LegacyNumberPayload(42L));

        // then
        assertThat(json).isEqualTo("{\"value\":\"42\"}");
    }

    private ObjectMapper globalObjectMapper() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        new JacksonConfig().jsonCustomizer().customize(builder);
        return builder.build();
    }

    private record LegacyPayload(String name) {}

    private record LegacyNumberPayload(Long value) {}
}
