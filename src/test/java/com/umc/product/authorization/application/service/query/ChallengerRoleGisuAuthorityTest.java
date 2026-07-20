package com.umc.product.authorization.application.service.query;

import static com.umc.product.support.fixture.AuthorizationFixture.슈퍼_관리자;
import static com.umc.product.support.fixture.AuthorizationFixture.중앙_역할;
import static com.umc.product.support.fixture.AuthorizationFixture.지부장;
import static com.umc.product.support.fixture.AuthorizationFixture.학교_역할;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.application.port.out.LoadChallengerRolePort;
import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.authorization.domain.exception.AuthorizationErrorCode;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.member.application.port.in.query.ListMemberSystemRoleUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChallengerRoleQueryService 기수 범위 권한")
class ChallengerRoleGisuAuthorityTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long GISU_ID = 9L;
    private static final Long OTHER_GISU_ID = 10L;
    private static final Long SCHOOL_ID = 30L;
    private static final Long OTHER_SCHOOL_ID = 31L;
    private static final Long CHAPTER_ID = 40L;

    @Mock
    LoadChallengerRolePort loadChallengerRolePort;

    @Mock
    GetGisuUseCase getGisuUseCase;

    @Mock
    ListMemberSystemRoleUseCase listMemberSystemRoleUseCase;

    @Nested
    @DisplayName("입력 경계")
    class InputBoundary {

        @Test
        @DisplayName("기수 기반 권한 조회는 null 기수를 모두 거부한다")
        void null_기수는_모든_기수_기반_권한에서_거부한다() {
            ChallengerRoleQueryService sut = sut(true);

            assertInvalid(() -> sut.listByMemberIdAndGisuId(MEMBER_ID, null));
            assertInvalid(() -> sut.getAllRoleTypesByMemberIdAndGisuId(MEMBER_ID, null));
            assertInvalid(() -> sut.hasRoleTypeInGisu(MEMBER_ID, null, ChallengerRoleType.CENTRAL_PRESIDENT));
            assertInvalid(() -> sut.hasAnyRoleTypeInGisu(MEMBER_ID, null, ChallengerRoleType.CENTRAL_PRESIDENT));
            assertInvalid(() -> sut.hasAllRoleTypeInGisu(MEMBER_ID, null, ChallengerRoleType.CENTRAL_PRESIDENT));
            assertInvalid(() -> sut.isCentralCoreInGisu(MEMBER_ID, null));
            assertInvalid(() -> sut.isCentralMemberInGisu(MEMBER_ID, null));
            assertInvalid(() -> sut.isSchoolCoreInGisu(MEMBER_ID, null, SCHOOL_ID));
            assertInvalid(() -> sut.isSchoolAdminInGisu(MEMBER_ID, null, SCHOOL_ID));
            assertInvalid(() -> sut.isChapterPresidentInGisu(MEMBER_ID, null, CHAPTER_ID));
            then(loadChallengerRolePort).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("학교와 지부 기반 권한은 null 조직 ID를 거부한다")
        void null_조직_ID는_거부한다() {
            ChallengerRoleQueryService sut = sut(true);

            assertInvalid(() -> sut.getAllRoleTypesByMemberIdAndSchoolId(MEMBER_ID, null));
            assertInvalid(() -> sut.isSchoolCoreInGisu(MEMBER_ID, GISU_ID, null));
            assertInvalid(() -> sut.isSchoolAdminInGisu(MEMBER_ID, GISU_ID, null));
            assertInvalid(() -> sut.isChapterPresidentInGisu(MEMBER_ID, GISU_ID, null));
            then(loadChallengerRolePort).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("단일 역할 조회는 null 역할을 false로 처리한다")
        void null_역할은_false다() {
            assertThat(sut(true).hasRoleTypeInGisu(MEMBER_ID, GISU_ID, null)).isFalse();
            then(loadChallengerRolePort).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("ANY 역할 조회는 null 또는 빈 역할 배열을 false로 처리한다")
        void ANY의_null과_빈_배열은_false다() {
            ChallengerRoleQueryService sut = sut(true);

            assertThat(sut.hasAnyRoleTypeInGisu(MEMBER_ID, GISU_ID, (ChallengerRoleType[]) null)).isFalse();
            assertThat(sut.hasAnyRoleTypeInGisu(MEMBER_ID, GISU_ID)).isFalse();
            then(loadChallengerRolePort).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("ALL 역할 조회는 존재하는 회원의 null 또는 빈 역할 배열을 true로 처리한다")
        void ALL의_null과_빈_배열은_true다() {
            ChallengerRoleQueryService sut = sut(true);

            assertThat(sut.hasAllRoleTypeInGisu(MEMBER_ID, GISU_ID, (ChallengerRoleType[]) null)).isTrue();
            assertThat(sut.hasAllRoleTypeInGisu(MEMBER_ID, GISU_ID)).isTrue();
            then(loadChallengerRolePort).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("삭제 회원과 SUPER_ADMIN")
    class MemberBoundary {

        @Test
        @DisplayName("존재하지 않는 회원은 빈 ALL 조건을 포함해 모든 기수 권한을 거부한다")
        void 존재하지_않는_회원은_모든_기수_권한을_거부한다() {
            ChallengerRoleQueryService sut = sut(false);

            assertThat(sut.hasRoleTypeInGisu(MEMBER_ID, GISU_ID, ChallengerRoleType.CENTRAL_PRESIDENT)).isFalse();
            assertThat(sut.hasAnyRoleTypeInGisu(MEMBER_ID, GISU_ID, ChallengerRoleType.CENTRAL_PRESIDENT)).isFalse();
            assertThat(sut.hasAllRoleTypeInGisu(MEMBER_ID, GISU_ID)).isFalse();
            assertThat(sut.isCentralCoreInGisu(MEMBER_ID, GISU_ID)).isFalse();
            assertThat(sut.isCentralMemberInGisu(MEMBER_ID, GISU_ID)).isFalse();
            assertThat(sut.isSchoolCoreInGisu(MEMBER_ID, GISU_ID, SCHOOL_ID)).isFalse();
            assertThat(sut.isSchoolAdminInGisu(MEMBER_ID, GISU_ID, SCHOOL_ID)).isFalse();
            assertThat(sut.isChapterPresidentInGisu(MEMBER_ID, GISU_ID, CHAPTER_ID)).isFalse();
            then(loadChallengerRolePort).shouldHaveNoInteractions();
            then(listMemberSystemRoleUseCase).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("SUPER_ADMIN은 역할 저장소를 조회하지 않고 모든 조직 권한을 허용한다")
        void SUPER_ADMIN은_모든_조직_권한을_허용한다() {
            given(listMemberSystemRoleUseCase.listByMemberId(MEMBER_ID))
                .willReturn(List.of(슈퍼_관리자(MEMBER_ID)));
            ChallengerRoleQueryService sut = sut(true);

            assertThat(sut.isCentralCoreInGisu(MEMBER_ID, GISU_ID)).isTrue();
            assertThat(sut.isCentralMemberInGisu(MEMBER_ID, GISU_ID)).isTrue();
            assertThat(sut.isSchoolCoreInGisu(MEMBER_ID, GISU_ID, SCHOOL_ID)).isTrue();
            assertThat(sut.isSchoolAdminInGisu(MEMBER_ID, GISU_ID, SCHOOL_ID)).isTrue();
            assertThat(sut.isChapterPresidentInGisu(MEMBER_ID, GISU_ID, CHAPTER_ID)).isTrue();
            then(loadChallengerRolePort).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("역할 집합 판정")
    class RoleSetAuthority {

        @Test
        @DisplayName("단일 역할과 ANY 역할은 정확히 일치하는 역할만 허용한다")
        void 단일과_ANY는_정확히_일치하는_역할만_허용한다() {
            given(loadChallengerRolePort.findRolesByMemberIdAndGisuId(MEMBER_ID, GISU_ID))
                .willReturn(List.of(
                    중앙_역할(ChallengerRoleType.CENTRAL_PRESIDENT, GISU_ID),
                    학교_역할(ChallengerRoleType.SCHOOL_PART_LEADER, SCHOOL_ID, GISU_ID)
                ));
            ChallengerRoleQueryService sut = sut(true);

            assertThat(sut.hasRoleTypeInGisu(
                MEMBER_ID,
                GISU_ID,
                ChallengerRoleType.CENTRAL_PRESIDENT
            )).isTrue();
            assertThat(sut.hasRoleTypeInGisu(
                MEMBER_ID,
                GISU_ID,
                ChallengerRoleType.CENTRAL_VICE_PRESIDENT
            )).isFalse();
            assertThat(sut.hasAnyRoleTypeInGisu(
                MEMBER_ID,
                GISU_ID,
                ChallengerRoleType.CENTRAL_VICE_PRESIDENT,
                ChallengerRoleType.SCHOOL_PART_LEADER
            )).isTrue();
        }

        @Test
        @DisplayName("ALL 역할은 중복 입력을 제거하고 모든 대상 역할의 포함 여부를 확인한다")
        void ALL은_중복을_제거하고_모든_역할을_확인한다() {
            given(loadChallengerRolePort.findRolesByMemberIdAndGisuId(MEMBER_ID, GISU_ID))
                .willReturn(List.of(
                    중앙_역할(ChallengerRoleType.CENTRAL_PRESIDENT, GISU_ID),
                    학교_역할(ChallengerRoleType.SCHOOL_PART_LEADER, SCHOOL_ID, GISU_ID)
                ));
            ChallengerRoleQueryService sut = sut(true);

            assertThat(sut.hasAllRoleTypeInGisu(
                MEMBER_ID,
                GISU_ID,
                ChallengerRoleType.CENTRAL_PRESIDENT,
                ChallengerRoleType.CENTRAL_PRESIDENT,
                ChallengerRoleType.SCHOOL_PART_LEADER
            )).isTrue();
            assertThat(sut.hasAllRoleTypeInGisu(
                MEMBER_ID,
                GISU_ID,
                ChallengerRoleType.CENTRAL_PRESIDENT,
                ChallengerRoleType.SCHOOL_PRESIDENT
            )).isFalse();
        }
    }

    @Nested
    @DisplayName("조직 범위 판정")
    class OrganizationScope {

        @Test
        @DisplayName("중앙 권한은 같은 기수의 중앙 역할 계층만 인정한다")
        void 중앙_권한은_같은_기수의_역할만_인정한다() {
            givenRegularMember();
            given(loadChallengerRolePort.findRolesByMemberIdAndGisuId(MEMBER_ID, GISU_ID))
                .willReturn(List.of(중앙_역할(ChallengerRoleType.CENTRAL_EDUCATION_TEAM_MEMBER, GISU_ID)));
            ChallengerRoleQueryService sut = sut(true);

            assertThat(sut.isCentralCoreInGisu(MEMBER_ID, GISU_ID)).isFalse();
            assertThat(sut.isCentralMemberInGisu(MEMBER_ID, GISU_ID)).isTrue();
        }

        @Test
        @DisplayName("학교 권한은 조직 유형과 학교 ID와 역할 계층이 모두 일치해야 한다")
        void 학교_권한은_모든_범위가_일치해야_한다() {
            givenRegularMember();
            given(loadChallengerRolePort.findRolesByMemberIdAndGisuId(MEMBER_ID, GISU_ID)).willReturn(List.of(
                중앙_역할(ChallengerRoleType.CENTRAL_PRESIDENT, GISU_ID),
                학교_역할(ChallengerRoleType.SCHOOL_PRESIDENT, OTHER_SCHOOL_ID, GISU_ID),
                학교_역할(ChallengerRoleType.SCHOOL_PART_LEADER, SCHOOL_ID, GISU_ID)
            ));
            ChallengerRoleQueryService sut = sut(true);

            assertThat(sut.isSchoolCoreInGisu(MEMBER_ID, GISU_ID, SCHOOL_ID)).isFalse();
            assertThat(sut.isSchoolAdminInGisu(MEMBER_ID, GISU_ID, SCHOOL_ID)).isTrue();
        }

        @Test
        @DisplayName("지부장 권한은 같은 기수와 지부에 부여된 지부장 역할만 인정한다")
        void 지부장_권한은_기수와_지부가_일치해야_한다() {
            givenRegularMember();
            given(loadChallengerRolePort.findRolesByMemberIdAndGisuId(MEMBER_ID, GISU_ID)).willReturn(List.of(
                학교_역할(ChallengerRoleType.SCHOOL_PRESIDENT, CHAPTER_ID, GISU_ID),
                지부장(CHAPTER_ID + 1, GISU_ID)
            ));
            ChallengerRoleQueryService sut = sut(true);

            assertThat(sut.isChapterPresidentInGisu(MEMBER_ID, GISU_ID, CHAPTER_ID)).isFalse();

            given(loadChallengerRolePort.findRolesByMemberIdAndGisuId(MEMBER_ID, GISU_ID))
                .willReturn(List.of(지부장(CHAPTER_ID, GISU_ID)));
            assertThat(sut.isChapterPresidentInGisu(MEMBER_ID, GISU_ID, CHAPTER_ID)).isTrue();
        }

        @Test
        @DisplayName("기수 범위 조회 port의 결과는 서비스에서 기수를 재검증하지 않고 신뢰한다")
        void 기수_범위_port의_계약을_신뢰한다() {
            givenRegularMember();
            given(loadChallengerRolePort.findRolesByMemberIdAndGisuId(MEMBER_ID, GISU_ID))
                .willReturn(List.of(중앙_역할(ChallengerRoleType.CENTRAL_PRESIDENT, OTHER_GISU_ID)));
            ChallengerRoleQueryService sut = sut(true);

            assertThat(sut.isCentralCoreInGisu(MEMBER_ID, GISU_ID)).isTrue();
        }
    }

    private void givenRegularMember() {
        given(listMemberSystemRoleUseCase.listByMemberId(MEMBER_ID)).willReturn(List.of());
    }

    private ChallengerRoleQueryService sut(boolean memberExists) {
        return new ChallengerRoleQueryService(
            loadChallengerRolePort,
            getGisuUseCase,
            listMemberSystemRoleUseCase,
            memberId -> memberExists
        );
    }

    private void assertInvalid(Runnable action) {
        assertThatThrownBy(action::run)
            .isInstanceOf(AuthorizationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AuthorizationErrorCode.INVALID_INPUT_VALUE);
    }
}
