package com.umc.product.member.adapter.in.web.dto.response;

import static com.umc.product.support.fixture.ChallengerUnitFixture.챌린저;
import static com.umc.product.support.fixture.MemberUnitFixture.회원;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.challenger.adapter.in.web.dto.response.ChallengerInfoResponse;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.MemberStatus;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.member.application.port.in.query.dto.MemberProfileInfo;
import com.umc.product.member.domain.Member;
import com.umc.product.member.domain.exception.MemberDomainException;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterInfo;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;

@DisplayName("MemberInfo 응답 계약")
class MemberInfoResponseTest {

    @Test
    @SuppressWarnings("deprecation")
    @DisplayName("구버전 factory는 학교 이름과 프로필 URL의 기본값을 유지한다")
    void 구버전_factory_호환성을_유지한다() {
        Member member = 회원(1L, 10L);

        MemberInfo defaultInfo = MemberInfo.from(member);
        MemberInfo detailedInfo = MemberInfo.from(member, "테스트대학교", "https://cdn/profile");

        assertThat(defaultInfo.schoolName()).isEqualTo("알 수 없음 ");
        assertThat(defaultInfo.profileImageLink()).isNull();
        assertThat(defaultInfo.roles()).isNull();
        assertThat(detailedInfo.schoolName()).isEqualTo("테스트대학교");
        assertThat(detailedInfo.profileImageLink()).isEqualTo("https://cdn/profile");
        assertThat(detailedInfo.roles()).isNull();
    }

    @Test
    @DisplayName("현재 factory는 회원·학교·프로필·역할 정보를 빠짐없이 변환한다")
    void 현재_factory가_전체_정보를_변환한다() {
        Member member = 회원(1L, 10L);

        MemberInfo result = MemberInfo.from(member, "테스트대학교", "https://cdn/profile", List.of());

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("홍길동");
        assertThat(result.schoolId()).isEqualTo(10L);
        assertThat(result.roles()).isEmpty();
    }

    @Test
    @DisplayName("학교가 배정되지 않은 회원은 학교 필수 기능을 사용할 수 없다")
    void 학교_미배정을_거부한다() {
        MemberInfo info = memberInfo(null);

        assertThatThrownBy(info::validateHasSchool)
            .isInstanceOf(MemberDomainException.class);
    }

    @Test
    @DisplayName("학교가 배정된 회원은 학교 검증을 통과한다")
    void 학교_배정_회원을_허용한다() {
        MemberInfo info = memberInfo(10L);

        info.validateHasSchool();
    }

    @Test
    @DisplayName("MemberInfo 공개 변환은 이메일만 비식별 기본값으로 바꾼다")
    void memberInfo를_공개용으로_변환한다() {
        MemberInfo source = memberInfo(10L);

        MemberInfo result = source.toPublic();

        assertThat(result.email()).isEqualTo("알 수 없음");
        assertThat(result.id()).isEqualTo(source.id());
        assertThat(result.status()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(result.roles()).isSameAs(source.roles());
    }

    @Test
    @DisplayName("프로필 없는 회원 응답은 프로필을 비워 둔다")
    void 프로필_없는_회원_응답을_변환한다() {
        MemberInfo source = memberInfo(10L);

        MemberInfoResponse result = MemberInfoResponse.from(source, List.of());

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.profile()).isNull();
        assertThat(result.challengerRecords()).isEmpty();
    }

    @Test
    @DisplayName("프로필 있는 회원 응답은 프로필과 챌린저 이력을 함께 변환한다")
    void 프로필과_챌린저_이력을_변환한다() {
        MemberInfo source = memberInfo(10L);
        MemberProfileInfo profile = MemberProfileInfo.builder().github("github").build();
        ChallengerInfoResponse record = challengerResponse();

        MemberInfoResponse result = MemberInfoResponse.from(source, profile, List.of(record));

        assertThat(result.profile()).isSameAs(profile);
        assertThat(result.challengerRecords()).containsExactly(record);
    }

    @Test
    @DisplayName("회원 공개 응답은 이메일·상태와 모든 챌린저 상벌점을 제거한다")
    void 공개_응답에서_민감_정보를_제거한다() {
        MemberInfoResponse source = MemberInfoResponse.from(memberInfo(10L), List.of(challengerResponse()));

        MemberInfoResponse result = source.toPublic();

        assertThat(result.email()).isNull();
        assertThat(result.status()).isNull();
        assertThat(result.challengerRecords()).singleElement()
            .satisfies(record -> {
                assertThat(record.points()).isEmpty();
                assertThat(record.challengerPoints()).isEmpty();
                assertThat(record.memberStatus()).isNull();
            });
    }

    private MemberInfo memberInfo(Long schoolId) {
        return MemberInfo.builder()
            .id(1L)
            .name("홍길동")
            .nickname("길동")
            .email("member@example.com")
            .schoolId(schoolId)
            .schoolName("테스트대학교")
            .profileImageId("profile-id")
            .profileImageLink("https://cdn/profile")
            .status(MemberStatus.ACTIVE)
            .roles(List.of())
            .build();
    }

    private ChallengerInfoResponse challengerResponse() {
        ChallengerInfo info = ChallengerInfo.from(챌린저(100L, 1L, 20L), List.of());
        return ChallengerInfoResponse.from(
            info,
            memberInfo(10L),
            new GisuInfo(20L, 12L, null, null, true),
            new ChapterInfo(30L, "Seoul A")
        );
    }
}
