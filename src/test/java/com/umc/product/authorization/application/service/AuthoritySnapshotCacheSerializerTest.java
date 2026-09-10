package com.umc.product.authorization.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    @DisplayName("지부 없는 비수강 중앙 운영진의 캐시를 복원해도 기수와 역할 범위를 유지한다")
    void 지부_없는_중앙_운영진의_권한_범위를_복원한다() {
        // given
        AuthoritySnapshotCacheSerializer serializer = new AuthoritySnapshotCacheSerializer(productionObjectMapper());
        AuthoritySnapshot snapshot = AuthoritySnapshot.of(
            1L,
            30L,
            List.of(GisuChallengerInfo.builder().gisuId(11L).challengerId(100L).build()),
            List.of(new RoleAttribute(
                ChallengerRoleType.CENTRAL_OPERATING_TEAM_MEMBER, OrganizationType.CENTRAL, null, null, 11L
            )),
            Set.of()
        );

        // when
        AuthoritySnapshot restored = serializer.deserialize(serializer.serialize(snapshot));

        // then
        assertThat(restored.gisuChallengerInfos()).isEqualTo(snapshot.gisuChallengerInfos());
        assertThat(restored.gisuChallengerInfos().getFirst().chapterId()).isNull();
        assertThat(restored.isCentralMemberInGisu(11L)).isTrue();
        assertThat(restored.isCentralMemberInGisu(10L)).isFalse();
        assertThat(restored.isSchoolCoreInGisu(11L, 30L)).isFalse();
        assertThat(restored.isChapterPresidentInGisu(11L, 90L)).isFalse();
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

    private ObjectMapper productionObjectMapper() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        new JacksonConfig().jsonCustomizer().customize(builder);
        return builder.build();
    }
}
