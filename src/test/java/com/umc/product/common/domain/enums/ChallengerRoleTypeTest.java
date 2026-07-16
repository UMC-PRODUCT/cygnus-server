package com.umc.product.common.domain.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@DisplayName("ChallengerRoleType")
class ChallengerRoleTypeTest {

    @Test
    @DisplayName("SUPER_ADMIN은 challenger role 유형에 포함되지 않는다")
    void excludes_super_admin() {
        assertThat(Arrays.stream(ChallengerRoleType.values()).map(Enum::name))
            .doesNotContain("SUPER_ADMIN");
    }

    @Test
    @DisplayName("SUPER_ADMIN challenger role 요청은 역직렬화되지 않는다")
    void rejects_super_admin_json() {
        ObjectMapper objectMapper = new ObjectMapper();

        assertThatThrownBy(() -> objectMapper.readValue("\"SUPER_ADMIN\"", ChallengerRoleType.class))
            .isInstanceOf(JsonProcessingException.class);
    }
}
