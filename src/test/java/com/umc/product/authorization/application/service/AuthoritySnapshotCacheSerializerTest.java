package com.umc.product.authorization.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.authorization.domain.AuthoritySnapshot;
import com.umc.product.authorization.domain.RoleAttribute;
import com.umc.product.authorization.domain.SubjectAttributes.GisuChallengerInfo;
import com.umc.product.authorization.domain.SystemRoleType;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AuthoritySnapshotCacheSerializer")
class AuthoritySnapshotCacheSerializerTest {

    @Test
    @DisplayName("AuthoritySnapshot을 캐시 가능한 JSON 문자열로 직렬화하고 다시 복원한다")
    void serialize_and_deserialize() {
        AuthoritySnapshotCacheSerializer serializer = new AuthoritySnapshotCacheSerializer(
            new ObjectMapper().findAndRegisterModules()
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

        assertThat(payload).contains("\"systemRoles\"");
        assertThat(payload).contains("SUPER_ADMIN");
        assertThat(restored.memberId()).isEqualTo(1L);
        assertThat(restored.gisuChallengerInfos()).hasSize(1);
        assertThat(restored.challengerRoles()).hasSize(1);
        assertThat(restored.isSuperAdmin()).isTrue();
        assertThat(restored.isSchoolCoreInGisu(9L, 30L)).isTrue();
    }
}
