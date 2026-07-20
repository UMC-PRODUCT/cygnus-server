package com.umc.product.authorization.domain;

import static com.umc.product.support.fixture.AuthorizationFixture.역할_속성;
import static com.umc.product.support.fixture.AuthorizationFixture.학교_역할_속성;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;

@DisplayName("AuthoritySnapshot 권한 매트릭스")
class AuthoritySnapshotMatrixTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long SCHOOL_ID = 30L;
    private static final Long OTHER_SCHOOL_ID = 31L;
    private static final Long GISU_ID = 9L;
    private static final Long OTHER_GISU_ID = 10L;
    private static final Long CHAPTER_ID = 40L;

    @Nested
    @DisplayName("불변 snapshot 생성")
    class Creation {

        @Test
        @DisplayName("null 컬렉션은 빈 불변 컬렉션으로 정규화한다")
        void null_컬렉션은_빈_불변_컬렉션으로_정규화한다() {
            AuthoritySnapshot snapshot = AuthoritySnapshot.of(MEMBER_ID, SCHOOL_ID, null, null, null);

            assertThat(snapshot.gisuChallengerInfos()).isEmpty();
            assertThat(snapshot.challengerRoles()).isEmpty();
            assertThat(snapshot.systemRoles()).isEmpty();
            assertThatThrownBy(() -> snapshot.challengerRoles().add(null))
                .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("입력 컬렉션 변경이 snapshot에 전파되지 않는다")
        void 입력_컬렉션을_방어적으로_복사한다() {
            List<RoleAttribute> roles = new ArrayList<>();
            roles.add(역할_속성(ChallengerRoleType.CENTRAL_PRESIDENT, null, GISU_ID));
            Set<SystemRoleType> systemRoles = new HashSet<>();
            AuthoritySnapshot snapshot = AuthoritySnapshot.of(
                MEMBER_ID,
                SCHOOL_ID,
                List.of(),
                roles,
                systemRoles
            );

            roles.clear();
            systemRoles.add(SystemRoleType.SUPER_ADMIN);

            assertThat(snapshot.challengerRoles()).hasSize(1);
            assertThat(snapshot.systemRoles()).isEmpty();
        }

        @Test
        @DisplayName("SubjectAttributes와 snapshot은 정보 손실 없이 왕복한다")
        void SubjectAttributes와_정보_손실_없이_왕복한다() {
            SubjectAttributes.GisuChallengerInfo challengerInfo =
                new SubjectAttributes.GisuChallengerInfo(GISU_ID, CHAPTER_ID, ChallengerPart.WEB, 100L);
            RoleAttribute role = 학교_역할_속성(
                ChallengerRoleType.SCHOOL_PART_LEADER,
                SCHOOL_ID,
                GISU_ID
            );
            SubjectAttributes subject = SubjectAttributes.builder()
                .memberId(MEMBER_ID)
                .schoolId(SCHOOL_ID)
                .gisuChallengerInfos(List.of(challengerInfo))
                .roleAttributes(List.of(role))
                .systemRoles(Set.of(SystemRoleType.SUPER_ADMIN))
                .build();

            AuthoritySnapshot snapshot = AuthoritySnapshot.from(subject);
            SubjectAttributes restored = snapshot.toSubjectAttributes();

            assertThat(restored).isEqualTo(subject);
            assertThat(subject.toAuthoritySnapshot()).isEqualTo(snapshot);
        }
    }

    @Nested
    @DisplayName("SUPER_ADMIN")
    class SuperAdmin {

        @Test
        @DisplayName("SUPER_ADMIN은 역할 목록이 없어도 모든 권한 정책을 통과한다")
        void SUPER_ADMIN은_모든_권한을_통과한다() {
            AuthoritySnapshot snapshot = AuthoritySnapshot.of(
                MEMBER_ID,
                SCHOOL_ID,
                List.of(),
                List.of(),
                Set.of(SystemRoleType.SUPER_ADMIN)
            );

            assertThat(snapshot.isSuperAdmin()).isTrue();
            assertThat(snapshot.isCentralCoreInAnyGisu()).isTrue();
            assertThat(snapshot.isCentralCoreInGisu(GISU_ID)).isTrue();
            assertThat(snapshot.isCentralMemberInAnyGisu()).isTrue();
            assertThat(snapshot.isCentralMemberInGisu(GISU_ID)).isTrue();
            assertThat(snapshot.isSchoolCoreInAnyGisu(SCHOOL_ID)).isTrue();
            assertThat(snapshot.isSchoolCoreInGisu(GISU_ID, SCHOOL_ID)).isTrue();
            assertThat(snapshot.isSchoolAdminInAnyGisu(SCHOOL_ID)).isTrue();
            assertThat(snapshot.isSchoolAdminInGisu(GISU_ID, SCHOOL_ID)).isTrue();
            assertThat(snapshot.isChapterPresidentInAnyGisu(CHAPTER_ID)).isTrue();
            assertThat(snapshot.isChapterPresidentInGisu(GISU_ID, CHAPTER_ID)).isTrue();
        }
    }

    @Nested
    @DisplayName("챌린저 역할 범위")
    class ChallengerRoleScope {

        @Test
        @DisplayName("중앙 권한은 역할 계층과 기수 범위를 함께 적용한다")
        void 중앙_권한은_역할_계층과_기수를_적용한다() {
            AuthoritySnapshot snapshot = snapshot(List.of(
                역할_속성(ChallengerRoleType.CENTRAL_VICE_PRESIDENT, null, GISU_ID),
                역할_속성(ChallengerRoleType.CENTRAL_EDUCATION_TEAM_MEMBER, null, OTHER_GISU_ID),
                학교_역할_속성(ChallengerRoleType.SCHOOL_PRESIDENT, SCHOOL_ID, GISU_ID)
            ));

            assertThat(snapshot.isSuperAdmin()).isFalse();
            assertThat(snapshot.isCentralCoreInAnyGisu()).isTrue();
            assertThat(snapshot.isCentralCoreInGisu(GISU_ID)).isTrue();
            assertThat(snapshot.isCentralCoreInGisu(OTHER_GISU_ID)).isFalse();
            assertThat(snapshot.isCentralMemberInAnyGisu()).isTrue();
            assertThat(snapshot.isCentralMemberInGisu(OTHER_GISU_ID)).isTrue();
            assertThat(snapshot.isCentralMemberInGisu(99L)).isFalse();
        }

        @Test
        @DisplayName("학교 권한은 조직 유형과 학교와 기수와 역할 계층을 모두 적용한다")
        void 학교_권한은_모든_범위를_적용한다() {
            AuthoritySnapshot snapshot = snapshot(List.of(
                역할_속성(ChallengerRoleType.CENTRAL_PRESIDENT, null, GISU_ID),
                학교_역할_속성(ChallengerRoleType.SCHOOL_VICE_PRESIDENT, SCHOOL_ID, GISU_ID),
                학교_역할_속성(ChallengerRoleType.SCHOOL_PART_LEADER, OTHER_SCHOOL_ID, OTHER_GISU_ID)
            ));

            assertThat(snapshot.isSchoolCoreInAnyGisu(SCHOOL_ID)).isTrue();
            assertThat(snapshot.isSchoolCoreInAnyGisu(OTHER_SCHOOL_ID)).isFalse();
            assertThat(snapshot.isSchoolCoreInGisu(GISU_ID, SCHOOL_ID)).isTrue();
            assertThat(snapshot.isSchoolCoreInGisu(OTHER_GISU_ID, SCHOOL_ID)).isFalse();
            assertThat(snapshot.isSchoolAdminInAnyGisu(OTHER_SCHOOL_ID)).isTrue();
            assertThat(snapshot.isSchoolAdminInAnyGisu(99L)).isFalse();
            assertThat(snapshot.isSchoolAdminInGisu(OTHER_GISU_ID, OTHER_SCHOOL_ID)).isTrue();
            assertThat(snapshot.isSchoolAdminInGisu(GISU_ID, OTHER_SCHOOL_ID)).isFalse();
        }

        @Test
        @DisplayName("지부장 권한은 조직 유형과 지부와 기수를 모두 적용한다")
        void 지부장_권한은_모든_범위를_적용한다() {
            AuthoritySnapshot snapshot = snapshot(List.of(
                학교_역할_속성(ChallengerRoleType.SCHOOL_PRESIDENT, CHAPTER_ID, GISU_ID),
                역할_속성(ChallengerRoleType.CHAPTER_PRESIDENT, CHAPTER_ID, GISU_ID)
            ));

            assertThat(snapshot.isChapterPresidentInAnyGisu(CHAPTER_ID)).isTrue();
            assertThat(snapshot.isChapterPresidentInAnyGisu(CHAPTER_ID + 1)).isFalse();
            assertThat(snapshot.isChapterPresidentInGisu(GISU_ID, CHAPTER_ID)).isTrue();
            assertThat(snapshot.isChapterPresidentInGisu(OTHER_GISU_ID, CHAPTER_ID)).isFalse();
        }

        private AuthoritySnapshot snapshot(List<RoleAttribute> roles) {
            return AuthoritySnapshot.of(MEMBER_ID, SCHOOL_ID, List.of(), roles, Set.of());
        }
    }
}
