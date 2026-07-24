package com.umc.product.authorization.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;

class AuthorizationSubjectSnapshotTest {

    private static final Instant START = Instant.parse("2026-07-01T00:00:00Z");
    private static final Instant END = Instant.parse("2026-08-01T00:00:00Z");

    @Test
    @DisplayName("ChallengerRole tuple은 Gisu 시작을 포함하고 종료를 제외한다")
    void evaluatesRoleAtHalfOpenGisuBoundary() {
        AuthorizationRoleTuple role = role();

        assertThat(role.isActiveAt(START.minusNanos(1))).isFalse();
        assertThat(role.isActiveAt(START)).isTrue();
        assertThat(role.isActiveAt(END.minusNanos(1))).isTrue();
        assertThat(role.isActiveAt(END)).isFalse();
    }

    @Test
    @DisplayName("ANONYMOUS와 SYSTEM과 CAPABILITY principal은 member authority fact를 가질 수 없다")
    void rejectsMemberFactsForTrustedNonMemberPrincipals() {
        assertThat(AuthorizationSubjectSnapshot.anonymous(START).roles()).isEmpty();
        assertThat(AuthorizationSubjectSnapshot.system("scheduler", START).roles()).isEmpty();
        assertThat(AuthorizationSubjectSnapshot.capability(
            "FORM_RESPONSE_ACCESS", "response-1", START).roles()).isEmpty();

        assertThatThrownBy(() -> new AuthorizationSubjectSnapshot(
            new AuthorizationPrincipal.SystemPrincipal("scheduler"),
            OptionalLong.empty(),
            START,
            Set.of(SystemRoleType.SUPER_ADMIN),
            List.of(),
            List.of(),
            Map.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("member authority fact");
    }

    @Test
    @DisplayName("MEMBER principal은 system role과 tuple 상관관계를 그대로 보존한다")
    void preservesMemberAuthorityTuples() {
        AuthorizationSubjectSnapshot snapshot = AuthorizationSubjectSnapshot.member(
            1L,
            2L,
            START,
            Set.of(SystemRoleType.SUPER_ADMIN),
            List.of(role()),
            List.of(new AuthorizationChallengerTuple(
                3L,
                4L,
                5L,
                ChallengerPart.PLAN,
                START,
                END)),
            Map.of(new AuthorizationSchoolChapterKey(4L, 2L), 5L));

        assertThat(snapshot.requireMemberId()).isEqualTo(1L);
        assertThat(snapshot.isSuperAdmin()).isTrue();
        assertThat(snapshot.roles()).singleElement().satisfies(role -> {
            assertThat(role.gisuId()).isEqualTo(4L);
            assertThat(role.organizationId()).isEqualTo(5L);
            assertThat(role.roleType()).isEqualTo(ChallengerRoleType.CHAPTER_PRESIDENT);
        });
    }

    @Test
    @DisplayName("학교가 없는 MEMBER도 전역 system role snapshot을 만들 수 있다")
    void allowsMemberWithoutSchoolForGlobalPolicy() {
        AuthorizationSubjectSnapshot snapshot = AuthorizationSubjectSnapshot.member(
            1L,
            null,
            START,
            Set.of(SystemRoleType.SUPER_ADMIN),
            List.of(),
            List.of(),
            Map.of());

        assertThat(snapshot.schoolId()).isEmpty();
        assertThat(snapshot.isSuperAdmin()).isTrue();
    }

    private AuthorizationRoleTuple role() {
        return new AuthorizationRoleTuple(
            ChallengerRoleType.CHAPTER_PRESIDENT,
            OrganizationType.CHAPTER,
            5L,
            null,
            4L,
            START,
            END);
    }
}
