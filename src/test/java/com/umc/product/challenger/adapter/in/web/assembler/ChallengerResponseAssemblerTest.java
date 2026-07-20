package com.umc.product.challenger.adapter.in.web.assembler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.challenger.adapter.in.web.dto.response.ChallengerInfoResponse;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.common.domain.enums.MemberStatus;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterInfo;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChallengerResponseAssembler")
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
    ChallengerResponseAssembler sut;

    @Test
    @DisplayName("챌린저 ID로 회원·기수·학교 지부를 조회해 단건 응답을 조립한다")
    void 챌린저_ID로_조립한다() {
        ChallengerInfo challenger = challenger(100L, 20L);
        MemberInfo member = member(10L);
        GisuInfo gisu = gisu();
        ChapterInfo chapter = chapter();
        given(getChallengerUseCase.getById(100L)).willReturn(challenger);
        given(getMemberUseCase.getById(1L)).willReturn(member);
        given(getGisuUseCase.getById(20L)).willReturn(gisu);
        given(getChapterUseCase.byGisuAndSchool(20L, 10L)).willReturn(chapter);

        ChallengerInfoResponse result = sut.fromChallengerId(100L);

        assertThat(result.challengerId()).isEqualTo(100L);
        assertThat(result.gisu()).isEqualTo(12L);
        assertThat(result.chapterName()).isEqualTo("Seoul A");
    }

    @Test
    @DisplayName("챌린저 이력이 없으면 기수·지부 batch 조회를 생략한다")
    void 빈_이력을_조립한다() {
        given(getChallengerUseCase.getAllByMemberId(1L)).willReturn(List.of());
        given(getMemberUseCase.getById(1L)).willReturn(member(10L));

        List<ChallengerInfoResponse> result = sut.fromMemberId(1L);

        assertThat(result).isEmpty();
        then(getGisuUseCase).shouldHaveNoInteractions();
        then(getChapterUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("중복 기수는 제거하고 회원 학교와 조합해 한 번씩 batch 조회한다")
    void 중복_기수를_batch로_조립한다() {
        given(getChallengerUseCase.getAllByMemberId(1L))
            .willReturn(List.of(challenger(100L, 20L), challenger(101L, 20L)));
        given(getMemberUseCase.getById(1L)).willReturn(member(10L));
        given(getGisuUseCase.getByIds(Set.of(20L))).willReturn(List.of(gisu()));
        given(getChapterUseCase.getChapterMapByGisuIdsAndSchoolIds(Set.of(20L), Set.of(10L)))
            .willReturn(Map.of(20L, Map.of(10L, chapter())));

        List<ChallengerInfoResponse> result = sut.fromMemberId(1L);

        assertThat(result).hasSize(2)
            .allSatisfy(response -> assertThat(response.chapterId()).isEqualTo(30L));
    }

    @Test
    @DisplayName("학교가 없는 회원은 지부 batch 조회를 생략한다")
    void 학교가_없으면_지부_조회를_생략한다() {
        given(getChallengerUseCase.getAllByMemberId(1L)).willReturn(List.of());
        given(getMemberUseCase.getById(1L)).willReturn(member(null));

        assertThat(sut.fromMemberId(1L)).isEmpty();
        then(getChapterUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("학교 ID가 없으면 지부 map 조회 결과도 null이다")
    void 학교_ID_없는_지부_조회() {
        ChapterInfo result = ReflectionTestUtils.invokeMethod(
            sut, "getChapterInfo", Map.of(), 20L, null);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("기수 또는 학교에 매칭되는 지부가 없으면 null이다")
    void 매칭_지부가_없는_조회() {
        Map<Long, Map<Long, ChapterInfo>> chapters = Map.of(20L, Map.of(10L, chapter()));

        ChapterInfo unknownGisu = ReflectionTestUtils.invokeMethod(
            sut, "getChapterInfo", chapters, 99L, 10L);
        ChapterInfo unknownSchool = ReflectionTestUtils.invokeMethod(
            sut, "getChapterInfo", chapters, 20L, 99L);

        assertThat(unknownGisu).isNull();
        assertThat(unknownSchool).isNull();
    }

    @Test
    @DisplayName("기수 ID가 null인 손상된 이력은 batch 조회 대상에서 제외되지만 조립 시 실패한다")
    void null_기수_ID를_거부한다() {
        given(getChallengerUseCase.getAllByMemberId(1L)).willReturn(List.of(challenger(100L, null)));
        given(getMemberUseCase.getById(1L)).willReturn(member(10L));

        assertThatThrownBy(() -> sut.fromMemberId(1L))
            .isInstanceOf(NullPointerException.class);
        then(getGisuUseCase).shouldHaveNoInteractions();
    }

    private ChallengerInfo challenger(Long challengerId, Long gisuId) {
        return ChallengerInfo.builder()
            .challengerId(challengerId)
            .memberId(1L)
            .gisuId(gisuId)
            .part(ChallengerPart.SPRINGBOOT)
            .challengerPoints(List.of())
            .totalPoints(0.0)
            .challengerStatus(ChallengerStatus.ACTIVE)
            .build();
    }

    private MemberInfo member(Long schoolId) {
        return MemberInfo.builder()
            .id(1L)
            .name("홍길동")
            .nickname("길동")
            .email("member@example.com")
            .schoolId(schoolId)
            .schoolName("테스트대학교")
            .profileImageLink("https://cdn/profile")
            .status(MemberStatus.ACTIVE)
            .roles(List.of())
            .build();
    }

    private GisuInfo gisu() {
        return new GisuInfo(20L, 12L, null, null, true);
    }

    private ChapterInfo chapter() {
        return new ChapterInfo(30L, "Seoul A");
    }
}
