package com.umc.product.member.adapter.in.web.v2.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.common.domain.enums.MemberStatus;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.member.application.port.in.query.dto.MemberProfileInfo;
import com.umc.product.member.application.port.in.query.dto.MemberSummaryV2Info;
import com.umc.product.member.application.port.in.query.dto.SearchMemberItemV2Info;
import com.umc.product.member.application.port.in.query.dto.SearchMemberV2Result;

@DisplayName("Member v2 response 변환 계약")
class MemberV2ResponseContractTest {

    @Test
    @DisplayName("요약 응답은 현재 기수·활성 챌린저·전체 이력을 모두 변환한다")
    void 요약_중첩_응답을_변환한다() {
        MemberSummaryV2Info.ActiveChallenger active = new MemberSummaryV2Info.ActiveChallenger(
            100L, ChallengerPart.SPRINGBOOT, ChallengerStatus.ACTIVE, List.of(), 3.0);
        MemberSummaryV2Info.CurrentGisuMembership current = new MemberSummaryV2Info.CurrentGisuMembership(
            20L, 12L, active, true, List.of(ChallengerRoleType.SCHOOL_PRESIDENT));
        MemberSummaryV2Info.ChallengerHistoryItem history = new MemberSummaryV2Info.ChallengerHistoryItem(
            100L, 20L, 12L, 30L, "Seoul A", ChallengerPart.SPRINGBOOT,
            ChallengerStatus.ACTIVE, List.of(), 3.0, List.of(ChallengerRoleType.SCHOOL_PRESIDENT));
        MemberSummaryV2Info source = new MemberSummaryV2Info(
            member(), MemberProfileInfo.builder().github("github").build(), true, 100L, current, List.of(history));

        MemberSummaryV2Response result = MemberSummaryV2Response.from(source);

        assertThat(result.currentGisuMemberInfo().challenger().challengerId()).isEqualTo(100L);
        assertThat(result.currentGisuMemberInfo().isAdmin()).isTrue();
        assertThat(result.challengerHistory()).singleElement()
            .satisfies(item -> {
                assertThat(item.chapterName()).isEqualTo("Seoul A");
                assertThat(item.roleTypes()).containsExactly(ChallengerRoleType.SCHOOL_PRESIDENT);
            });
    }

    @Test
    @DisplayName("현재 기수에 활성 챌린저가 없으면 중첩 challenger를 null로 유지한다")
    void 현재_기수의_null_챌린저를_유지한다() {
        MemberSummaryV2Info.CurrentGisuMembership current = new MemberSummaryV2Info.CurrentGisuMembership(
            20L, 12L, null, false, List.of());
        MemberSummaryV2Info source = new MemberSummaryV2Info(
            member(), MemberProfileInfo.builder().build(), false, 0L, current, List.of());

        assertThat(MemberSummaryV2Response.from(source).currentGisuMemberInfo().challenger()).isNull();
    }

    @Test
    @DisplayName("검색 응답은 대표 챌린저와 모든 참여 이력을 변환하고 이메일을 마스킹한다")
    void 검색_중첩_응답을_변환한다() {
        SearchMemberItemV2Info item = new SearchMemberItemV2Info(
            1L, "홍길동", "길동", "member@example.com", 10L, "테스트대학교", null,
            new SearchMemberItemV2Info.PrimaryChallenger(
                100L, 20L, 12L, ChallengerPart.SPRINGBOOT, ChallengerStatus.ACTIVE),
            true,
            List.of(new SearchMemberItemV2Info.Participation(
                101L, 19L, 11L, ChallengerPart.NODEJS, ChallengerStatus.GRADUATED))
        );
        SearchMemberV2Result source = new SearchMemberV2Result(
            new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1));

        SearchMemberV2Response result = SearchMemberV2Response.from(source).withMaskedEmails();

        assertThat(result.page().content()).singleElement()
            .satisfies(response -> {
                assertThat(response.email()).isEqualTo("mem***@example.com");
                assertThat(response.currentChallenger().challengerId()).isEqualTo(100L);
                assertThat(response.challengerRecords()).singleElement()
                    .satisfies(record -> assertThat(record.challengerId()).isEqualTo(101L));
            });
    }

    private MemberInfo member() {
        return MemberInfo.builder()
            .id(1L)
            .name("홍길동")
            .nickname("길동")
            .email("member@example.com")
            .schoolId(10L)
            .schoolName("테스트대학교")
            .status(MemberStatus.ACTIVE)
            .roles(List.of())
            .build();
    }
}
