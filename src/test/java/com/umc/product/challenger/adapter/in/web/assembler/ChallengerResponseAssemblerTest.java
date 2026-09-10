package com.umc.product.challenger.adapter.in.web.assembler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleInfo;
import com.umc.product.challenger.adapter.in.web.dto.response.ChallengerInfoResponse;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.GisuLearningType;
import com.umc.product.common.domain.enums.OrganizationType;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterInfo;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;

@ExtendWith(MockitoExtension.class)
@DisplayName("챌린저 응답의 지부 정보")
class ChallengerResponseAssemblerTest {

    @Mock
    GetChallengerUseCase getChallengerUseCase;
    @Mock
    GetMemberUseCase getMemberUseCase;
    @Mock
    GetGisuUseCase getGisuUseCase;
    @Mock
    GetChapterUseCase getChapterUseCase;
    @InjectMocks
    ChallengerResponseAssembler assembler;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("비수강 소속의 단건과 목록 조회는 지부 연결 유무를 동일하게 반영한다")
    void 단건과_목록에서_지부_연결을_동일하게_반영한다(boolean hasChapter) {
        // given
        ChallengerInfo challenger = challenger();
        MemberInfo member = member();
        GisuInfo gisu = gisu();
        ChapterInfo chapter = hasChapter ? new ChapterInfo(20L, "현재 지부") : null;
        given(getChallengerUseCase.getById(100L)).willReturn(challenger);
        given(getChallengerUseCase.getAllByMemberId(1L)).willReturn(List.of(challenger));
        given(getMemberUseCase.getById(1L)).willReturn(member);
        given(getGisuUseCase.getById(6L)).willReturn(gisu);
        given(getGisuUseCase.getByIds(Set.of(6L))).willReturn(List.of(gisu));
        given(getChapterUseCase.findByGisuAndSchool(6L, 30L)).willReturn(Optional.ofNullable(chapter));
        given(getChapterUseCase.getChapterMapByGisuIdsAndSchoolIds(Set.of(6L), Set.of(30L)))
            .willReturn(hasChapter ? Map.of(6L, Map.of(30L, chapter)) : Map.of());

        // when
        ChallengerInfoResponse single = assembler.fromChallengerId(100L);
        List<ChallengerInfoResponse> all = assembler.fromMemberId(1L);

        // then
        assertThat(all).containsExactly(single);
        assertThat(single.challengerId()).isEqualTo(100L);
        assertThat(single.gisuId()).isEqualTo(6L);
        assertThat(single.schoolId()).isEqualTo(30L);
        assertThat(single.tracks()).isEmpty();
        assertThat(single.chapterId()).isEqualTo(hasChapter ? 20L : null);
        assertThat(single.chapterName()).isEqualTo(hasChapter ? "현재 지부" : null);
    }

    @Test
    @DisplayName("지부가 없어도 역할을 포함한 응답에서 중앙 운영진 역할을 보존한다")
    void 지부_없는_응답에_중앙_역할을_보존한다() {
        // given
        ChallengerRoleInfo role = ChallengerRoleInfo.builder().id(200L).challengerId(100L)
            .gisuId(6L).roleType(ChallengerRoleType.CENTRAL_OPERATING_TEAM_MEMBER)
            .organizationType(OrganizationType.CENTRAL).build();

        // when
        ChallengerInfoResponse response = ChallengerInfoResponse.from(
            challenger(), member(), gisu(), null, List.of(role)
        );

        // then
        assertThat(response.chapterId()).isNull();
        assertThat(response.chapterName()).isNull();
        assertThat(response.roles()).singleElement().satisfies(result -> {
            assertThat(result.challengerRoleId()).isEqualTo(200L);
            assertThat(result.roleType()).isEqualTo(ChallengerRoleType.CENTRAL_OPERATING_TEAM_MEMBER);
            assertThat(result.gisuId()).isEqualTo(6L);
        });
    }

    private ChallengerInfo challenger() {
        return ChallengerInfo.builder().challengerId(100L).memberId(1L).gisuId(6L).tracks(List.of()).build();
    }

    private MemberInfo member() {
        return MemberInfo.builder().id(1L).schoolId(30L).name("중앙 회원").build();
    }

    private GisuInfo gisu() {
        return new GisuInfo(6L, 11L, Instant.parse("2026-08-31T15:00:00Z"),
            Instant.parse("2027-02-27T15:00:00Z"), true, GisuLearningType.TRACK);
    }
}
