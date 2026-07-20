package com.umc.product.challenger.adapter.in.web.dto.response;

import static com.umc.product.support.fixture.AuthorizationFixture.학교_역할;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleInfo;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerPointInfo;
import com.umc.product.challenger.domain.enums.PointType;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.common.domain.enums.MemberStatus;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterInfo;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;

@DisplayName("ChallengerInfoResponse 계약")
class ChallengerInfoResponseTest {

    @Test
    @DisplayName("역할 없는 구버전 응답은 상벌점 별칭과 회원 상태 별칭을 동일하게 유지한다")
    void 구버전_응답_별칭을_유지한다() {
        ChallengerInfo info = challengerInfo();

        ChallengerInfoResponse result = ChallengerInfoResponse.from(info, memberInfo(), gisuInfo(), chapterInfo());

        assertThat(result.challengerPoints()).isSameAs(info.challengerPoints());
        assertThat(result.points()).isSameAs(info.challengerPoints());
        assertThat(result.memberStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(result.status()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(result.email()).isNull();
        assertThat(result.roles()).isNull();
    }

    @Test
    @DisplayName("역할 포함 응답은 같은 기수의 역할을 API 응답으로 변환한다")
    void 같은_기수_역할을_변환한다() {
        ChallengerRoleInfo role = ChallengerRoleInfo.from(
            학교_역할(ChallengerRoleType.SCHOOL_PRESIDENT, 10L, 20L),
            gisuInfo()
        );

        ChallengerInfoResponse result = ChallengerInfoResponse.from(
            challengerInfo(), memberInfo(), gisuInfo(), chapterInfo(), List.of(role)
        );

        assertThat(result.roles()).singleElement()
            .satisfies(response -> {
                assertThat(response.roleType()).isEqualTo(ChallengerRoleType.SCHOOL_PRESIDENT);
                assertThat(response.gisuId()).isEqualTo(20L);
                assertThat(response.gisu()).isEqualTo(12L);
            });
        assertThat(result.challengerStatus()).isEqualTo(ChallengerStatus.ACTIVE);
    }

    @Test
    @DisplayName("공개 응답은 상벌점·이메일·회원 상태를 제거하고 활동 식별 정보는 보존한다")
    void 공개_응답을_마스킹한다() {
        ChallengerInfoResponse source = ChallengerInfoResponse.from(
            challengerInfo(), memberInfo(), gisuInfo(), chapterInfo(), List.of()
        );

        ChallengerInfoResponse result = source.toPublic();

        assertThat(result.challengerId()).isEqualTo(source.challengerId());
        assertThat(result.points()).isEmpty();
        assertThat(result.challengerPoints()).isEmpty();
        assertThat(result.totalPoints()).isEqualTo(3.5);
        assertThat(result.email()).isNull();
        assertThat(result.memberStatus()).isNull();
        assertThat(result.status()).isNull();
    }

    private ChallengerInfo challengerInfo() {
        return ChallengerInfo.builder()
            .challengerId(100L)
            .memberId(1L)
            .gisuId(20L)
            .part(ChallengerPart.SPRINGBOOT)
            .challengerPoints(List.of(ChallengerPointInfo.builder()
                .id(1L)
                .challengerId(100L)
                .pointType(PointType.CUSTOM)
                .point(3.5)
                .description("기여")
                .build()))
            .totalPoints(3.5)
            .challengerStatus(ChallengerStatus.ACTIVE)
            .build();
    }

    private MemberInfo memberInfo() {
        return MemberInfo.builder()
            .id(1L)
            .name("홍길동")
            .nickname("길동")
            .email("member@example.com")
            .schoolId(10L)
            .schoolName("테스트대학교")
            .profileImageLink("https://cdn/profile")
            .status(MemberStatus.ACTIVE)
            .roles(List.of())
            .build();
    }

    private GisuInfo gisuInfo() {
        return new GisuInfo(20L, 12L, null, null, true);
    }

    private ChapterInfo chapterInfo() {
        return new ChapterInfo(30L, "Seoul A");
    }
}
