package com.umc.product.member.adapter.in.web.assembler;

import static org.assertj.core.api.Assertions.assertThat;
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

import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.member.adapter.in.web.dto.response.MemberInfoResponse;
import com.umc.product.member.application.port.in.query.GetMemberProfileUseCase;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.member.application.port.in.query.dto.MemberProfileInfo;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterInfo;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberInfoResponseAssembler")
class MemberInfoResponseAssemblerTest {

    @Mock
    GetMemberUseCase getMemberUseCase;
    @Mock
    GetMemberProfileUseCase getMemberProfileUseCase;
    @Mock
    GetChallengerUseCase getChallengerUseCase;
    @Mock
    GetGisuUseCase getGisuUseCase;
    @Mock
    GetChapterUseCase getChapterUseCase;

    @InjectMocks
    MemberInfoResponseAssembler sut;

    @Test
    @DisplayName("챌린저가 없으면 기수·지부 batch 조회 없이 회원과 프로필만 조립한다")
    void 챌린저_없는_회원을_조립한다() {
        MemberInfo member = memberInfo(null);
        MemberProfileInfo profile = MemberProfileInfo.builder().github("github").build();
        given(getMemberUseCase.getById(1L)).willReturn(member);
        given(getChallengerUseCase.getAllByMemberId(1L)).willReturn(List.of());
        given(getMemberProfileUseCase.getMemberProfileById(1L)).willReturn(profile);

        MemberInfoResponse result = sut.fromMemberId(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.profile()).isSameAs(profile);
        assertThat(result.challengerRecords()).isEmpty();
        then(getGisuUseCase).shouldHaveNoInteractions();
        then(getChapterUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("챌린저의 중복 기수 ID를 제거해 기수·학교 지부를 batch 조회하고 응답한다")
    void 챌린저_이력을_batch로_조립한다() {
        MemberInfo member = memberInfo(10L);
        ChallengerInfo first = challenger(101L, 20L);
        ChallengerInfo second = challenger(102L, 20L);
        GisuInfo gisu = new GisuInfo(20L, 12L, null, null, true);
        ChapterInfo chapter = new ChapterInfo(30L, "Seoul A");
        given(getMemberUseCase.getById(1L)).willReturn(member);
        given(getChallengerUseCase.getAllByMemberId(1L)).willReturn(List.of(first, second));
        given(getGisuUseCase.getByIds(Set.of(20L))).willReturn(List.of(gisu));
        given(getChapterUseCase.getChapterMapByGisuIdsAndSchoolIds(Set.of(20L), Set.of(10L)))
            .willReturn(Map.of(20L, Map.of(10L, chapter)));
        given(getMemberProfileUseCase.getMemberProfileById(1L))
            .willReturn(MemberProfileInfo.builder().build());

        MemberInfoResponse result = sut.fromMemberId(1L);

        assertThat(result.challengerRecords()).hasSize(2)
            .allSatisfy(record -> {
                assertThat(record.gisu()).isEqualTo(12L);
                assertThat(record.chapterId()).isEqualTo(30L);
            });
    }

    @Test
    @DisplayName("공개 응답은 email·상태·상벌점을 제거한다")
    void 공개_응답을_마스킹한다() {
        MemberInfo member = memberInfo(10L);
        ChallengerInfo challenger = challenger(101L, 20L);
        given(getMemberUseCase.getById(1L)).willReturn(member);
        given(getChallengerUseCase.getAllByMemberId(1L)).willReturn(List.of(challenger));
        given(getGisuUseCase.getByIds(Set.of(20L)))
            .willReturn(List.of(new GisuInfo(20L, 12L, null, null, true)));
        given(getChapterUseCase.getChapterMapByGisuIdsAndSchoolIds(Set.of(20L), Set.of(10L)))
            .willReturn(Map.of(20L, Map.of(10L, new ChapterInfo(30L, "Seoul A"))));
        given(getMemberProfileUseCase.getMemberProfileById(1L))
            .willReturn(MemberProfileInfo.builder().build());

        MemberInfoResponse result = sut.fromMemberIdToPublic(1L);

        assertThat(result.email()).isNull();
        assertThat(result.status()).isNull();
        assertThat(result.challengerRecords().getFirst().points()).isEmpty();
        assertThat(result.challengerRecords().getFirst().memberStatus()).isNull();
    }

    @Test
    @DisplayName("학교 ID가 없으면 지부 map에서 조회하지 않고 null을 반환한다")
    void 학교_ID_없는_지부_조회를_처리한다() {
        ChapterInfo result = ReflectionTestUtils.invokeMethod(
            sut, "getChapterInfo", Map.of(), 20L, null);

        assertThat(result).isNull();
    }

    private MemberInfo memberInfo(Long schoolId) {
        return MemberInfo.builder()
            .id(1L)
            .name("홍길동")
            .nickname("길동")
            .email("member@example.com")
            .schoolId(schoolId)
            .schoolName("테스트대학교")
            .profileImageLink("https://cdn.example/member")
            .status(com.umc.product.common.domain.enums.MemberStatus.ACTIVE)
            .roles(List.of())
            .build();
    }

    private ChallengerInfo challenger(Long challengerId, Long gisuId) {
        return ChallengerInfo.builder()
            .challengerId(challengerId)
            .memberId(1L)
            .gisuId(gisuId)
            .part(ChallengerPart.SPRINGBOOT)
            .challengerStatus(ChallengerStatus.ACTIVE)
            .challengerPoints(List.of())
            .totalPoints(0.0)
            .build();
    }
}
