package com.umc.product.project.application.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authorization.domain.AuthorizationPrincipal;
import com.umc.product.authorization.domain.AuthorizationRoleTuple;
import com.umc.product.authorization.domain.AuthorizationSubjectSnapshot;
import com.umc.product.authorization.domain.SystemRoleType;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;

class ProjectPolicyCommonSubjectConversionTest {

    private static final Instant NOW = Instant.parse("2026-07-24T00:00:00Z");

    @Test
    @DisplayName("공용 MEMBER snapshot을 Project snapshot으로 정보 손실 없이 변환한다")
    void convertsCommonMemberSnapshot() {
        AuthorizationSubjectSnapshot common = AuthorizationSubjectSnapshot.member(
            1L,
            2L,
            NOW,
            Set.of(SystemRoleType.SUPER_ADMIN),
            List.of(new AuthorizationRoleTuple(
                ChallengerRoleType.CENTRAL_PRESIDENT,
                OrganizationType.CENTRAL,
                null,
                null,
                3L,
                NOW.minusSeconds(10),
                NOW.plusSeconds(10))),
            List.of(),
            Map.of());

        ProjectPolicySubjectSnapshot project = ProjectPolicySubjectSnapshot.from(common);

        assertThat(project.principal()).isEqualTo(new ProjectPolicyPrincipal.Member(1L));
        assertThat(project.evaluatedAt()).isEqualTo(NOW);
        assertThat(project.superAdmin()).isTrue();
        assertThat(project.roles()).singleElement()
            .extracting(ProjectPolicyRoleTuple::gisuId)
            .isEqualTo(3L);
    }

    @Test
    @DisplayName("Project가 아직 지원하지 않는 공용 principal은 명시적으로 거부한다")
    void rejectsUnsupportedProjectPrincipal() {
        AuthorizationSubjectSnapshot anonymous = new AuthorizationSubjectSnapshot(
            new AuthorizationPrincipal.Anonymous(),
            java.util.OptionalLong.empty(),
            NOW,
            Set.of(),
            List.of(),
            List.of(),
            Map.of());

        assertThatThrownBy(() -> ProjectPolicySubjectSnapshot.from(anonymous))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ANONYMOUS");
    }
}
