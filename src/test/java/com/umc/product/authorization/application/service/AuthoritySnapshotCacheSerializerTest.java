package com.umc.product.authorization.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.authorization.application.service.dto.AuthoritySnapshotCacheDto;
import com.umc.product.authorization.domain.AuthoritySnapshot;
import com.umc.product.authorization.domain.RoleAttribute;
import com.umc.product.authorization.domain.SubjectAttributes.GisuChallengerInfo;
import com.umc.product.authorization.domain.SystemRoleType;
import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;
import com.umc.product.global.config.JacksonConfig;

@DisplayName("AuthoritySnapshotCacheSerializer")
class AuthoritySnapshotCacheSerializerTest {

    @Test
    @DisplayName("AuthoritySnapshot을 캐시 가능한 JSON 문자열로 직렬화하고 다시 복원한다")
    void serialize_and_deserialize() {
        AuthoritySnapshotCacheSerializer serializer = new AuthoritySnapshotCacheSerializer(
            productionObjectMapper()
        );
        AuthoritySnapshot snapshot = AuthoritySnapshot.of(
            1L,
            30L,
            List.of(GisuChallengerInfo.builder()
                .gisuId(9L)
                .chapterId(90L)
                .part(ChallengerPart.SPRINGBOOT)
                .challengerId(100L)
                .build()),
            List.of(new RoleAttribute(
                ChallengerRoleType.SCHOOL_PRESIDENT,
                OrganizationType.SCHOOL,
                30L,
                null,
                9L
            )),
            Set.of(SystemRoleType.SUPER_ADMIN)
        );

        String payload = serializer.serialize(snapshot);
        AuthoritySnapshot restored = serializer.deserialize(payload);

        assertThat(payload).contains("\"schemaVersion\":\"1\"");
        assertThat(payload).contains("\"systemRoles\"");
        assertThat(payload).contains("SUPER_ADMIN");
        assertThat(restored.memberId()).isEqualTo(1L);
        assertThat(restored.gisuChallengerInfos()).hasSize(1);
        assertThat(restored.challengerRoles()).hasSize(1);
        assertThat(restored.isSuperAdmin()).isTrue();
        assertThat(restored.isSchoolCoreInGisu(9L, 30L)).isTrue();
    }

    @Test
    @DisplayName("schema version이 없거나 지원하지 않는 캐시 payload는 복원하지 않는다")
    void reject_missing_or_unsupported_schema_version() {
        AuthoritySnapshotCacheSerializer serializer = new AuthoritySnapshotCacheSerializer(
            new ObjectMapper().findAndRegisterModules()
        );

        assertThatThrownBy(() -> serializer.deserialize("{}"))
            .isInstanceOf(AuthorizationDomainException.class);
        assertThatThrownBy(() -> serializer.deserialize("{\"schemaVersion\":2}"))
            .isInstanceOf(AuthorizationDomainException.class);
    }

    @Test
    @DisplayName("null·공백·잘못된 JSON 캐시 payload는 복원하지 않는다")
    void reject_empty_or_malformed_payload() {
        AuthoritySnapshotCacheSerializer serializer = new AuthoritySnapshotCacheSerializer(
            new ObjectMapper().findAndRegisterModules()
        );

        assertThatThrownBy(() -> serializer.deserialize(null))
            .isInstanceOf(AuthorizationDomainException.class);
        assertThatThrownBy(() -> serializer.deserialize("  "))
            .isInstanceOf(AuthorizationDomainException.class);
        assertThatThrownBy(() -> serializer.deserialize("{"))
            .isInstanceOf(AuthorizationDomainException.class);
    }

    @Test
    @DisplayName("ObjectMapper 직렬화 실패는 정책 평가 실패로 변환한다")
    void wrap_serialization_failure() throws Exception {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        given(objectMapper.writeValueAsString(any()))
            .willThrow(new JsonProcessingException("serialization failed") { });
        AuthoritySnapshotCacheSerializer serializer = new AuthoritySnapshotCacheSerializer(objectMapper);

        assertThatThrownBy(() -> serializer.serialize(AuthoritySnapshot.of(
            1L, null, List.of(), List.of(), Set.of())))
            .isInstanceOf(AuthorizationDomainException.class);
    }

    @Test
    @DisplayName("ObjectMapper가 null tree를 반환하면 schema 검증에서 거부한다")
    void reject_null_json_tree() throws Exception {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        given(objectMapper.readTree("null-tree")).willReturn(null);
        AuthoritySnapshotCacheSerializer serializer = new AuthoritySnapshotCacheSerializer(objectMapper);

        assertThatThrownBy(() -> serializer.deserialize("null-tree"))
            .isInstanceOf(AuthorizationDomainException.class);
    }

    @Test
    @DisplayName("캐시 DTO의 null collection은 불변 빈 collection으로 정규화한다")
    void normalize_null_collections() {
        AuthoritySnapshotCacheDto dto = new AuthoritySnapshotCacheDto(1, 1L, null, null, null, null);

        assertThat(dto.gisuChallengerInfos()).isEmpty();
        assertThat(dto.challengerRoles()).isEmpty();
        assertThat(dto.systemRoles()).isEmpty();
        assertThat(dto.toDomain().memberId()).isEqualTo(1L);
    }

    private ObjectMapper productionObjectMapper() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        new JacksonConfig().jsonCustomizer().customize(builder);
        return builder.build();
    }
}
