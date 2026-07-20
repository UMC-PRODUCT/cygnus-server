package com.umc.product.authorization.domain;

import static com.umc.product.support.fixture.AuthorizationFixture.중앙_역할;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleInfo;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;

@DisplayName("Authorization 잔여 domain 계약")
class AuthorizationResidualContractTest {

    @Test
    @SuppressWarnings("removal")
    @DisplayName("구버전 역할 Info factory도 entity 필드를 보존한다")
    void legacy_role_info_factory() {
        ChallengerRole role = 중앙_역할(ChallengerRoleType.CENTRAL_PRESIDENT, 9L);
        ReflectionTestUtils.setField(role, "id", 1L);

        ChallengerRoleInfo result = ChallengerRoleInfo.from(role);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.challengerId()).isEqualTo(10L);
        assertThat(result.roleType()).isEqualTo(ChallengerRoleType.CENTRAL_PRESIDENT);
        assertThat(result.organizationType()).isEqualTo(OrganizationType.CENTRAL);
        assertThat(result.gisuId()).isEqualTo(9L);
        assertThat(result.gisu()).isNull();
    }

    @Test
    @DisplayName("중앙 외 역할은 생성과 수정 모두 organization ID가 필수다")
    void non_central_role_requires_organization_id() {
        assertThatThrownBy(() -> ChallengerRole.create(
            10L, ChallengerRoleType.SCHOOL_PRESIDENT, null, null, 9L))
            .isInstanceOf(IllegalArgumentException.class);

        ChallengerRole role = 중앙_역할(ChallengerRoleType.CENTRAL_PRESIDENT, 9L);
        assertThatThrownBy(() -> role.update(
            ChallengerRoleType.CHAPTER_PRESIDENT, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("중앙 멤버와 지부장 정책은 각 역할 범위에서 true를 반환한다")
    void residual_snapshot_policies() {
        AuthoritySnapshot snapshot = AuthoritySnapshot.of(
            1L,
            30L,
            List.of(),
            List.of(
                new RoleAttribute(
                    ChallengerRoleType.CENTRAL_OPERATING_TEAM_MEMBER,
                    OrganizationType.CENTRAL, null, null, 9L),
                new RoleAttribute(
                    ChallengerRoleType.CHAPTER_PRESIDENT,
                    OrganizationType.CHAPTER, 20L, null, 9L)
            ),
            Set.of()
        );

        assertThat(snapshot.isCentralMemberInAnyGisu()).isTrue();
        assertThat(snapshot.isChapterPresidentInAnyGisu(20L)).isTrue();
        assertThat(snapshot.isChapterPresidentInGisu(9L, 20L)).isTrue();
    }
}
