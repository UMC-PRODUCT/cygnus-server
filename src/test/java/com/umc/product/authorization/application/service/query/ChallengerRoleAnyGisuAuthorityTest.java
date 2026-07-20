package com.umc.product.authorization.application.service.query;

import static com.umc.product.support.fixture.AuthorizationFixture.슈퍼_관리자;
import static com.umc.product.support.fixture.AuthorizationFixture.일반_시스템_역할;
import static com.umc.product.support.fixture.AuthorizationFixture.중앙_역할;
import static com.umc.product.support.fixture.AuthorizationFixture.지부장;
import static com.umc.product.support.fixture.AuthorizationFixture.학교_역할;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

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
@DisplayName("ChallengerRoleQueryService 기수 무관 권한")
class ChallengerRoleAnyGisuAuthorityTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long GISU_ID = 9L;
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
    @DisplayName("회원 존재와 시스템 역할")
    class MemberAndSystemRole {

        @Test
        @DisplayName("존재하지 않는 회원은 모든 기수 무관 권한을 거부한다")
        void 존재하지_않는_회원은_모든_기수_무관_권한을_거부한다() {
            ChallengerRoleQueryService sut = sut(false);

            assertThat(sut.isSuperAdmin(MEMBER_ID)).isFalse();
            assertThat(sut.isCentralCoreInAnyGisu(MEMBER_ID)).isFalse();
            assertThat(sut.isCentralMemberInAnyGisu(MEMBER_ID)).isFalse();
            assertThat(sut.isSchoolCoreInAnyGisu(MEMBER_ID, SCHOOL_ID)).isFalse();
            assertThat(sut.isSchoolAdminInAnyGisu(MEMBER_ID, SCHOOL_ID)).isFalse();
            assertThat(sut.isChapterPresidentInAnyGisu(MEMBER_ID, CHAPTER_ID)).isFalse();
            then(loadChallengerRolePort).shouldHaveNoInteractions();
            then(listMemberSystemRoleUseCase).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("SUPER_ADMIN은 챌린저 역할 조회 없이 모든 기수 무관 권한을 허용한다")
        void SUPER_ADMIN은_모든_기수_무관_권한을_허용한다() {
            given(listMemberSystemRoleUseCase.listByMemberId(MEMBER_ID))
                .willReturn(List.of(슈퍼_관리자(MEMBER_ID)));
            ChallengerRoleQueryService sut = sut(true);

            assertThat(sut.isSuperAdmin(MEMBER_ID)).isTrue();
            assertThat(sut.isCentralCoreInAnyGisu(MEMBER_ID)).isTrue();
            assertThat(sut.isCentralMemberInAnyGisu(MEMBER_ID)).isTrue();
            assertThat(sut.isSchoolCoreInAnyGisu(MEMBER_ID, SCHOOL_ID)).isTrue();
            assertThat(sut.isSchoolAdminInAnyGisu(MEMBER_ID, SCHOOL_ID)).isTrue();
            assertThat(sut.isChapterPresidentInAnyGisu(MEMBER_ID, CHAPTER_ID)).isTrue();
            then(loadChallengerRolePort).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("지원하지 않는 시스템 역할 값은 입력 예외로 거부한다")
        void 지원하지_않는_시스템_역할은_입력_예외로_거부한다() {
            given(listMemberSystemRoleUseCase.listByMemberId(MEMBER_ID))
                .willReturn(List.of(일반_시스템_역할(MEMBER_ID)));
            ChallengerRoleQueryService sut = sut(true);

            assertThatThrownBy(() -> sut.isSuperAdmin(MEMBER_ID))
                .isInstanceOf(AuthorizationDomainException.class)
                .extracting("baseCode")
                .isEqualTo(AuthorizationErrorCode.INVALID_INPUT_VALUE);
        }

        @Test
        @DisplayName("시스템 역할이 없으면 SUPER_ADMIN 권한을 거부한다")
        void 시스템_역할이_없으면_SUPER_ADMIN_권한을_거부한다() {
            givenRegularMember();

            assertThat(sut(true).isSuperAdmin(MEMBER_ID)).isFalse();
        }
    }

    @Nested
    @DisplayName("중앙 권한")
    class CentralAuthority {

        @Test
        @DisplayName("중앙 총괄단과 중앙 일반 운영진을 계층에 맞게 구분한다")
        void 중앙_역할을_계층에_맞게_구분한다() {
            givenRegularMember();
            ChallengerRoleQueryService sut = sut(true);

            given(loadChallengerRolePort.findByMemberId(MEMBER_ID))
                .willReturn(List.of(중앙_역할(ChallengerRoleType.CENTRAL_VICE_PRESIDENT, GISU_ID)));
            assertThat(sut.isCentralCoreInAnyGisu(MEMBER_ID)).isTrue();

            given(loadChallengerRolePort.findByMemberId(MEMBER_ID))
                .willReturn(List.of(중앙_역할(ChallengerRoleType.CENTRAL_EDUCATION_TEAM_MEMBER, GISU_ID)));
            assertThat(sut.isCentralCoreInAnyGisu(MEMBER_ID)).isFalse();
            assertThat(sut.isCentralMemberInAnyGisu(MEMBER_ID)).isTrue();
        }

        @Test
        @DisplayName("학교 역할은 중앙 권한으로 인정하지 않는다")
        void 학교_역할은_중앙_권한이_아니다() {
            givenRegularMember();
            given(loadChallengerRolePort.findByMemberId(MEMBER_ID))
                .willReturn(List.of(학교_역할(ChallengerRoleType.SCHOOL_PRESIDENT, SCHOOL_ID, GISU_ID)));
            ChallengerRoleQueryService sut = sut(true);

            assertThat(sut.isCentralMemberInAnyGisu(MEMBER_ID)).isFalse();
        }
    }

    @Nested
    @DisplayName("학교 권한")
    class SchoolAuthority {

        @Test
        @DisplayName("학교 ID가 없으면 회장단과 관리자 판정을 거부한다")
        void 학교_ID가_없으면_입력_예외가_발생한다() {
            ChallengerRoleQueryService sut = sut(true);

            assertThatThrownBy(() -> sut.isSchoolCoreInAnyGisu(MEMBER_ID, null))
                .isInstanceOf(AuthorizationDomainException.class)
                .extracting("baseCode")
                .isEqualTo(AuthorizationErrorCode.INVALID_INPUT_VALUE);
            assertThatThrownBy(() -> sut.isSchoolAdminInAnyGisu(MEMBER_ID, null))
                .isInstanceOf(AuthorizationDomainException.class)
                .extracting("baseCode")
                .isEqualTo(AuthorizationErrorCode.INVALID_INPUT_VALUE);
        }

        @Test
        @DisplayName("조직 유형과 학교 ID가 모두 일치할 때만 학교 회장단으로 인정한다")
        void 조직_유형과_학교_ID가_모두_일치해야_한다() {
            givenRegularMember();
            given(loadChallengerRolePort.findByMemberId(MEMBER_ID)).willReturn(List.of(
                중앙_역할(ChallengerRoleType.CENTRAL_PRESIDENT, GISU_ID),
                학교_역할(ChallengerRoleType.SCHOOL_PRESIDENT, OTHER_SCHOOL_ID, GISU_ID),
                학교_역할(ChallengerRoleType.SCHOOL_PART_LEADER, SCHOOL_ID, GISU_ID)
            ));
            ChallengerRoleQueryService sut = sut(true);

            assertThat(sut.isSchoolCoreInAnyGisu(MEMBER_ID, SCHOOL_ID)).isFalse();
            assertThat(sut.isSchoolAdminInAnyGisu(MEMBER_ID, SCHOOL_ID)).isTrue();
        }

        @Test
        @DisplayName("같은 학교의 부회장은 학교 회장단이다")
        void 같은_학교의_부회장은_학교_회장단이다() {
            givenRegularMember();
            given(loadChallengerRolePort.findByMemberId(MEMBER_ID))
                .willReturn(List.of(학교_역할(
                    ChallengerRoleType.SCHOOL_VICE_PRESIDENT,
                    SCHOOL_ID,
                    GISU_ID
                )));
            ChallengerRoleQueryService sut = sut(true);

            assertThat(sut.isSchoolCoreInAnyGisu(MEMBER_ID, SCHOOL_ID)).isTrue();
        }
    }

    @Nested
    @DisplayName("지부장 권한")
    class ChapterAuthority {

        @Test
        @DisplayName("지부 ID가 없으면 입력 예외가 발생한다")
        void 지부_ID가_없으면_입력_예외가_발생한다() {
            assertThatThrownBy(() -> sut(true).isChapterPresidentInAnyGisu(MEMBER_ID, null))
                .isInstanceOf(AuthorizationDomainException.class)
                .extracting("baseCode")
                .isEqualTo(AuthorizationErrorCode.INVALID_INPUT_VALUE);
        }

        @Test
        @DisplayName("지부장 역할은 조직 유형과 지부 ID가 모두 일치해야 한다")
        void 지부장_역할은_조직과_ID가_모두_일치해야_한다() {
            givenRegularMember();
            given(loadChallengerRolePort.findByMemberId(MEMBER_ID)).willReturn(List.of(
                학교_역할(ChallengerRoleType.SCHOOL_PRESIDENT, CHAPTER_ID, GISU_ID),
                지부장(CHAPTER_ID + 1, GISU_ID)
            ));
            ChallengerRoleQueryService sut = sut(true);

            assertThat(sut.isChapterPresidentInAnyGisu(MEMBER_ID, CHAPTER_ID)).isFalse();

            given(loadChallengerRolePort.findByMemberId(MEMBER_ID))
                .willReturn(List.of(지부장(CHAPTER_ID, GISU_ID)));
            assertThat(sut.isChapterPresidentInAnyGisu(MEMBER_ID, CHAPTER_ID)).isTrue();
            then(loadChallengerRolePort).should(never())
                .findRolesByMemberIdAndGisuId(MEMBER_ID, GISU_ID);
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
}
