package com.umc.product.member.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.challenger.application.port.in.query.GetChallengerActivityPeriodUseCase;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ActivityPeriodSummary;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.common.domain.enums.MemberStatus;
import com.umc.product.member.application.port.in.query.GetMemberCredentialUseCase;
import com.umc.product.member.application.port.in.query.GetMemberProfileUseCase;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberCredentialInfo;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.member.application.port.in.query.dto.MemberProfileInfo;
import com.umc.product.member.application.port.in.query.dto.MemberSummaryV2Info;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterInfo;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberSummaryV2QueryService — BFF 조립")
class MemberSummaryV2QueryServiceTest {

    @Mock GetMemberUseCase getMemberUseCase;
    @Mock GetMemberCredentialUseCase getMemberCredentialUseCase;
    @Mock GetMemberProfileUseCase getMemberProfileUseCase;
    @Mock GetChallengerUseCase getChallengerUseCase;
    @Mock GetGisuUseCase getGisuUseCase;
    @Mock GetChapterUseCase getChapterUseCase;
    @Mock GetChallengerRoleUseCase getChallengerRoleUseCase;
    @Mock GetChallengerActivityPeriodUseCase getChallengerActivityPeriodUseCase;

    @InjectMocks MemberSummaryV2QueryService service;

    private MemberInfo member() {
        return MemberInfo.builder()
            .id(100L)
            .name("홍길동")
            .nickname("hong")
            .email("umc@hanyang.ac.kr")
            .schoolId(1L)
            .schoolName("한양대 ERICA")
            .profileImageLink(null)
            .status(MemberStatus.ACTIVE)
            .roles(List.of())
            .build();
    }

    private MemberProfileInfo profile() {
        return MemberProfileInfo.builder().build();
    }

    private ChallengerInfo challenger(Long id, Long gisuId, ChallengerStatus status) {
        return ChallengerInfo.builder()
            .challengerId(id)
            .memberId(100L)
            .gisuId(gisuId)
            .part(ChallengerPart.SPRINGBOOT)
            .challengerPoints(List.of())
            .totalPoints(0.0)
            .challengerStatus(status)
            .build();
    }

    private GisuInfo gisu(Long gisuId, Long generation) {
        Instant start = Instant.now().minus(60, ChronoUnit.DAYS);
        Instant end = Instant.now().plus(120, ChronoUnit.DAYS);
        return new GisuInfo(gisuId, generation, start, end, true);
    }

    @Test
    void 활성기수에_ACTIVE_챌린저와_운영진_기록이_있으면_그_정보가_세팅된다() {
        MemberInfo m = member();
        given(getMemberUseCase.getById(100L)).willReturn(m);
        given(getMemberProfileUseCase.getMemberProfileById(100L)).willReturn(profile());

        ChallengerInfo activeChallenger = challenger(11L, 7L, ChallengerStatus.ACTIVE);
        given(getChallengerUseCase.getAllByMemberId(100L)).willReturn(List.of(activeChallenger));

        GisuInfo activeGisu = gisu(7L, 8L);
        given(getGisuUseCase.findActiveGisu()).willReturn(Optional.of(activeGisu));
        given(getGisuUseCase.getByIds(anySet())).willReturn(List.of(activeGisu));

        given(getChapterUseCase.getChapterMapByGisuIdsAndSchoolIds(anySet(), anySet()))
            .willReturn(Map.of(7L, Map.of(1L, new ChapterInfo(200L, "서울지부"))));

        given(getChallengerRoleUseCase.getAllRoleTypesByChallengerIds(anySet()))
            .willReturn(Map.of(11L, List.of(ChallengerRoleType.SCHOOL_PRESIDENT)));

        given(getChallengerActivityPeriodUseCase.calculateActivityPeriod(any(), any()))
            .willReturn(new ActivityPeriodSummary(45L, List.of()));

        MemberSummaryV2Info info = service.getSummaryByMemberId(100L);

        assertThat(info.totalActivityDays()).isEqualTo(45L);
        assertThat(info.currentGisuMembership()).isNotNull();
        assertThat(info.currentGisuMembership().gisuId()).isEqualTo(7L);
        assertThat(info.currentGisuMembership().challenger()).isNotNull();
        assertThat(info.currentGisuMembership().challenger().challengerId()).isEqualTo(11L);
        assertThat(info.currentGisuMembership().isAdmin()).isTrue();
        assertThat(info.currentGisuMembership().roleTypes())
            .containsExactly(ChallengerRoleType.SCHOOL_PRESIDENT);
        assertThat(info.challengerHistory()).hasSize(1);
    }

    @Test
    void 활성기수가_없으면_currentGisuMembership은_null이다() {
        MemberInfo m = member();
        given(getMemberUseCase.getById(100L)).willReturn(m);
        given(getMemberProfileUseCase.getMemberProfileById(100L)).willReturn(profile());

        ChallengerInfo prevChallenger = challenger(5L, 6L, ChallengerStatus.GRADUATED);
        given(getChallengerUseCase.getAllByMemberId(100L)).willReturn(List.of(prevChallenger));

        given(getGisuUseCase.findActiveGisu()).willReturn(Optional.empty());

        Instant start = Instant.now().minus(300, ChronoUnit.DAYS);
        Instant end = Instant.now().minus(120, ChronoUnit.DAYS);
        GisuInfo oldGisu = new GisuInfo(6L, 7L, start, end, false);
        given(getGisuUseCase.getByIds(anySet())).willReturn(List.of(oldGisu));

        given(getChapterUseCase.getChapterMapByGisuIdsAndSchoolIds(anySet(), anySet()))
            .willReturn(Map.of(6L, Map.of(1L, new ChapterInfo(200L, "서울지부"))));

        given(getChallengerRoleUseCase.getAllRoleTypesByChallengerIds(anySet())).willReturn(Map.of());

        given(getChallengerActivityPeriodUseCase.calculateActivityPeriod(any(), any()))
            .willReturn(new ActivityPeriodSummary(180L, List.of()));

        MemberSummaryV2Info info = service.getSummaryByMemberId(100L);

        assertThat(info.currentGisuMembership()).isNull();
        assertThat(info.totalActivityDays()).isEqualTo(180L);
        assertThat(info.challengerHistory()).hasSize(1);
    }

    @Test
    void 활성기수_챌린저가_EXPELLED면_challenger는_null이고_isAdmin은_운영진_기록을_따른다() {
        MemberInfo m = member();
        given(getMemberUseCase.getById(100L)).willReturn(m);
        given(getMemberProfileUseCase.getMemberProfileById(100L)).willReturn(profile());

        ChallengerInfo expelled = challenger(11L, 7L, ChallengerStatus.EXPELLED);
        given(getChallengerUseCase.getAllByMemberId(100L)).willReturn(List.of(expelled));

        GisuInfo activeGisu = gisu(7L, 8L);
        given(getGisuUseCase.findActiveGisu()).willReturn(Optional.of(activeGisu));
        given(getGisuUseCase.getByIds(anySet())).willReturn(List.of(activeGisu));

        given(getChapterUseCase.getChapterMapByGisuIdsAndSchoolIds(anySet(), anySet()))
            .willReturn(Map.of(7L, Map.of(1L, new ChapterInfo(200L, "서울지부"))));

        // 활성 기수에 운영진 기록이 없음
        given(getChallengerRoleUseCase.getAllRoleTypesByChallengerIds(anySet())).willReturn(Map.of());

        given(getChallengerActivityPeriodUseCase.calculateActivityPeriod(any(), any()))
            .willReturn(new ActivityPeriodSummary(0L, List.of()));

        MemberSummaryV2Info info = service.getSummaryByMemberId(100L);

        assertThat(info.currentGisuMembership()).isNotNull();
        assertThat(info.currentGisuMembership().challenger()).isNull();
        assertThat(info.currentGisuMembership().isAdmin()).isFalse();
        assertThat(info.totalActivityDays()).isZero();
    }

    @Test
    void 챌린저_이력이_없는_신규_회원도_정상_응답된다() {
        MemberInfo m = member();
        given(getMemberUseCase.getById(100L)).willReturn(m);
        given(getMemberProfileUseCase.getMemberProfileById(100L)).willReturn(profile());

        given(getChallengerUseCase.getAllByMemberId(100L)).willReturn(List.of());
        given(getGisuUseCase.findActiveGisu()).willReturn(Optional.of(gisu(7L, 8L)));
        given(getChallengerActivityPeriodUseCase.calculateActivityPeriod(any(), any()))
            .willReturn(new ActivityPeriodSummary(0L, List.of()));

        MemberSummaryV2Info info = service.getSummaryByMemberId(100L);

        assertThat(info.challengerHistory()).isEmpty();
        assertThat(info.totalActivityDays()).isZero();
        // 활성 기수가 있어도, 본인 챌린저/운영진 기록이 없으면 isAdmin=false + challenger=null
        assertThat(info.currentGisuMembership()).isNotNull();
        assertThat(info.currentGisuMembership().challenger()).isNull();
        assertThat(info.currentGisuMembership().isAdmin()).isFalse();
        assertThat(info.currentGisuMembership().roleTypes()).isEmpty();
    }

    @Test
    void 로컬_자격증명이_있는_회원은_hasLocalCredential이_true이다() {
        MemberInfo m = member();
        given(getMemberUseCase.getById(100L)).willReturn(m);
        given(getMemberCredentialUseCase.findCredentialByMemberId(100L))
            .willReturn(Optional.of(new MemberCredentialInfo(100L, "{bcrypt}encoded")));
        given(getMemberProfileUseCase.getMemberProfileById(100L)).willReturn(profile());

        given(getChallengerUseCase.getAllByMemberId(100L)).willReturn(List.of());
        given(getGisuUseCase.findActiveGisu()).willReturn(Optional.empty());
        given(getChallengerActivityPeriodUseCase.calculateActivityPeriod(any(), any()))
            .willReturn(new ActivityPeriodSummary(0L, List.of()));

        MemberSummaryV2Info info = service.getSummaryByMemberId(100L);

        assertThat(info.hasLocalCredential()).isTrue();
    }

    @Test
    void 로컬_자격증명이_없는_회원은_hasLocalCredential이_false이다() {
        MemberInfo m = member();
        given(getMemberUseCase.getById(100L)).willReturn(m);
        given(getMemberCredentialUseCase.findCredentialByMemberId(100L)).willReturn(Optional.empty());
        given(getMemberProfileUseCase.getMemberProfileById(100L)).willReturn(profile());

        given(getChallengerUseCase.getAllByMemberId(100L)).willReturn(List.of());
        given(getGisuUseCase.findActiveGisu()).willReturn(Optional.empty());
        given(getChallengerActivityPeriodUseCase.calculateActivityPeriod(any(), any()))
            .willReturn(new ActivityPeriodSummary(0L, List.of()));

        MemberSummaryV2Info info = service.getSummaryByMemberId(100L);

        assertThat(info.hasLocalCredential()).isFalse();
    }

    @Test
    void 학교가_없고_일부_기수_정보가_누락되어도_이력을_안전하게_정렬한다() {
        MemberInfo schoolless = MemberInfo.builder()
            .id(100L)
            .name("홍길동")
            .nickname("hong")
            .email("member@example.com")
            .schoolId(null)
            .status(MemberStatus.ACTIVE)
            .roles(List.of())
            .build();
        ChallengerInfo known = challenger(11L, 7L, ChallengerStatus.GRADUATED);
        ChallengerInfo unknown = challenger(12L, 999L, ChallengerStatus.GRADUATED);
        GisuInfo knownGisu = gisu(7L, 8L);
        given(getMemberUseCase.getById(100L)).willReturn(schoolless);
        given(getMemberCredentialUseCase.findCredentialByMemberId(100L)).willReturn(Optional.empty());
        given(getMemberProfileUseCase.getMemberProfileById(100L)).willReturn(profile());
        given(getChallengerUseCase.getAllByMemberId(100L)).willReturn(List.of(unknown, known));
        given(getGisuUseCase.findActiveGisu()).willReturn(Optional.empty());
        given(getGisuUseCase.getByIds(anySet())).willReturn(List.of(knownGisu));
        given(getChallengerRoleUseCase.getAllRoleTypesByChallengerIds(anySet())).willReturn(Map.of());
        given(getChallengerActivityPeriodUseCase.calculateActivityPeriod(any(), any()))
            .willReturn(new ActivityPeriodSummary(0L, List.of()));

        MemberSummaryV2Info result = service.getSummaryByMemberId(100L);

        assertThat(result.challengerHistory()).extracting(MemberSummaryV2Info.ChallengerHistoryItem::challengerId)
            .containsExactly(11L, 12L);
        assertThat(result.challengerHistory().get(1).generation()).isNull();
        assertThat(result.challengerHistory().get(1).chapterId()).isNull();
    }
}
