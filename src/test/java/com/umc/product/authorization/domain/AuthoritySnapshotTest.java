package com.umc.product.authorization.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;

@DisplayName("AuthoritySnapshot")
class AuthoritySnapshotTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long SCHOOL_ID = 30L;
    private static final Long OTHER_SCHOOL_ID = 31L;
    private static final Long GISU_ID = 9L;
    private static final Long OTHER_GISU_ID = 10L;

    @Test
    @DisplayName("SUPER_ADMIN은 system role로 분리해서 표현하고 모든 운영진 정책을 통과한다")
    void super_admin_system_role() {
        AuthoritySnapshot snapshot = AuthoritySnapshot.of(
            MEMBER_ID,
            SCHOOL_ID,
            List.of(),
            List.of(),
            Set.of(SystemRoleType.SUPER_ADMIN)
        );

        assertThat(snapshot.isSuperAdmin()).isTrue();
        assertThat(snapshot.isCentralCoreInGisu(GISU_ID)).isTrue();
        assertThat(snapshot.isSchoolCoreInGisu(GISU_ID, OTHER_SCHOOL_ID)).isTrue();
    }

    @Test
    @DisplayName("기수 무관 challenger role 정책은 AnyGisu 이름으로만 사용한다")
    void central_core_in_any_gisu() {
        AuthoritySnapshot snapshot = AuthoritySnapshot.of(
            MEMBER_ID,
            SCHOOL_ID,
            List.of(),
            List.of(new RoleAttribute(
                ChallengerRoleType.CENTRAL_VICE_PRESIDENT,
                OrganizationType.CENTRAL,
                null,
                null,
                GISU_ID
            )),
            Set.of()
        );

        assertThat(snapshot.isCentralCoreInAnyGisu()).isTrue();
    }

    @Test
    @DisplayName("중앙 총괄단 여부는 같은 기수의 challenger role로 판단한다")
    void central_core_in_gisu() {
        AuthoritySnapshot snapshot = AuthoritySnapshot.of(
            MEMBER_ID,
            SCHOOL_ID,
            List.of(),
            List.of(new RoleAttribute(
                ChallengerRoleType.CENTRAL_VICE_PRESIDENT,
                OrganizationType.CENTRAL,
                null,
                null,
                GISU_ID
            )),
            Set.of()
        );

        assertThat(snapshot.isCentralCoreInGisu(GISU_ID)).isTrue();
        assertThat(snapshot.isCentralCoreInGisu(OTHER_GISU_ID)).isFalse();
    }

    @Test
    @DisplayName("학교 회장단 여부는 같은 기수와 학교의 challenger role로 판단한다")
    void school_core_in_gisu() {
        AuthoritySnapshot snapshot = AuthoritySnapshot.of(
            MEMBER_ID,
            SCHOOL_ID,
            List.of(),
            List.of(new RoleAttribute(
                ChallengerRoleType.SCHOOL_VICE_PRESIDENT,
                OrganizationType.SCHOOL,
                SCHOOL_ID,
                null,
                GISU_ID
            )),
            Set.of()
        );

        assertThat(snapshot.isSchoolCoreInGisu(GISU_ID, SCHOOL_ID)).isTrue();
        assertThat(snapshot.isSchoolCoreInGisu(GISU_ID, OTHER_SCHOOL_ID)).isFalse();
        assertThat(snapshot.isSchoolCoreInGisu(OTHER_GISU_ID, SCHOOL_ID)).isFalse();
    }
}
