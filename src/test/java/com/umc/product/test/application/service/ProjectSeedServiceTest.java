package com.umc.product.test.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.challenger.application.port.in.command.ManageChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterWithSchoolsInfo;
import com.umc.product.project.application.port.in.command.AddProjectMemberUseCase;
import com.umc.product.project.application.port.in.command.CreateDraftProjectUseCase;
import com.umc.product.project.application.port.in.command.dto.AddProjectMemberCommand;
import com.umc.product.test.application.port.in.command.dto.SeedProjectsCommand;
import com.umc.product.test.application.port.in.command.dto.SeedProjectsResult;

@ExtendWith(MockitoExtension.class)
class ProjectSeedServiceTest {

    @Mock
    GetMemberUseCase getMemberUseCase;
    @Mock
    GetGisuUseCase getGisuUseCase;
    @Mock
    GetChapterUseCase getChapterUseCase;
    @Mock
    GetChallengerUseCase getChallengerUseCase;
    @Mock
    ManageChallengerUseCase manageChallengerUseCase;
    @Mock
    CreateDraftProjectUseCase createDraftProjectUseCase;
    @Mock
    AddProjectMemberUseCase addProjectMemberUseCase;

    PartAssignmentPolicy partAssignmentPolicy = new PartAssignmentPolicy();
    ProjectSeedService sut;

    @BeforeEach
    void setUp() {
        sut = new ProjectSeedService(
            partAssignmentPolicy,
            getMemberUseCase,
            getGisuUseCase,
            getChapterUseCase,
            getChallengerUseCase,
            manageChallengerUseCase,
            createDraftProjectUseCase,
            addProjectMemberUseCase
        );
    }

    @Test
    @DisplayName("풀이 MIN_TOTAL(11) 미만인 school 은 시딩을 스킵한다")
    void 풀_부족_시_스킵() {
        // Given - 풀 3명 (< 11)
        ChapterWithSchoolsInfo chapter = new ChapterWithSchoolsInfo(
            1L, "서울", List.of(new ChapterWithSchoolsInfo.SchoolInfo(101L, "건국대"))
        );
        given(getChapterUseCase.getChaptersWithSchoolsByGisuId(9L)).willReturn(List.of(chapter));
        given(getMemberUseCase.listIdsBySchoolIds(Set.of(101L))).willReturn(Map.of(101L, Set.of(1L, 2L, 3L)));
        given(getChallengerUseCase.listByMemberIdsAndGisuId(Set.of(1L, 2L, 3L), 9L)).willReturn(Map.of());

        // When
        SeedProjectsResult result = sut.seed(new SeedProjectsCommand(5, 9L));

        // Then
        assertThat(result.createdProjectIds()).isEmpty();
        assertThat(result.partialProjects()).isEmpty();
        assertThat(result.skippedChapters()).isNotEmpty();
        assertThat(result.skippedChapters().get(0).reason()).contains("INSUFFICIENT_POOL");
        verify(createDraftProjectUseCase, never()).create(any());
    }

    @Test
    @DisplayName("정상 시딩 시 PO 가 PLAN 챌린저로 등록되지 않은 경우 자동 등록한다")
    void PO_PLAN_챌린저_자동_등록() {
        // Given
        Long gisuId = 9L;
        ChapterWithSchoolsInfo chapter = new ChapterWithSchoolsInfo(
            1L, "서울", List.of(new ChapterWithSchoolsInfo.SchoolInfo(101L, "건국대"))
        );
        Set<Long> pool = bigPool(13);
        given(getChapterUseCase.getChaptersWithSchoolsByGisuId(gisuId)).willReturn(List.of(chapter));
        given(getMemberUseCase.listIdsBySchoolIds(Set.of(101L))).willReturn(Map.of(101L, pool));
        given(getChallengerUseCase.listByMemberIdsAndGisuId(pool, gisuId)).willReturn(Map.of());
        given(manageChallengerUseCase.createChallenger(any())).willReturn(9000L);
        given(createDraftProjectUseCase.create(any())).willReturn(1000L);

        // When
        SeedProjectsResult result = sut.seed(new SeedProjectsCommand(1, gisuId));

        // Then
        assertThat(result.createdProjectIds()).hasSize(1);
        assertThat(result.partialProjects()).isEmpty();
        verify(manageChallengerUseCase, times(1)).createChallenger(any());
        verify(createDraftProjectUseCase, times(1)).create(any());
        verify(addProjectMemberUseCase, atLeast(11)).add(any());
    }

    @Test
    @DisplayName("같은 호출 안에서 한 멤버가 두 프로젝트에 중복 배정되지 않는다")
    void 멤버_중복_배정_차단() {
        // Given
        Long gisuId = 9L;
        ChapterWithSchoolsInfo chapter = new ChapterWithSchoolsInfo(
            1L, "서울", List.of(new ChapterWithSchoolsInfo.SchoolInfo(101L, "건국대"))
        );
        Set<Long> pool = bigPool(30);
        given(getChapterUseCase.getChaptersWithSchoolsByGisuId(gisuId)).willReturn(List.of(chapter));
        given(getMemberUseCase.listIdsBySchoolIds(Set.of(101L))).willReturn(Map.of(101L, pool));
        given(getChallengerUseCase.listByMemberIdsAndGisuId(pool, gisuId)).willReturn(Map.of());
        given(manageChallengerUseCase.createChallenger(any())).willReturn(9000L, 9001L);
        AtomicLong projectIdSeq = new AtomicLong(1000L);
        given(createDraftProjectUseCase.create(any())).willAnswer(inv -> projectIdSeq.getAndIncrement());

        // When
        SeedProjectsResult result = sut.seed(new SeedProjectsCommand(2, gisuId));

        // Then
        assertThat(result.createdProjectIds()).hasSize(2);
        ArgumentCaptor<AddProjectMemberCommand> captor = ArgumentCaptor.forClass(AddProjectMemberCommand.class);
        verify(addProjectMemberUseCase, atLeast(22)).add(captor.capture());
        Set<Long> usedMemberIds = new HashSet<>();
        for (AddProjectMemberCommand cmd : captor.getAllValues()) {
            assertThat(usedMemberIds.add(cmd.memberId()))
                .as("memberId %d already used in another project", cmd.memberId())
                .isTrue();
        }
    }

    @Test
    @DisplayName("addProjectMember 가 중간에 실패하면 partialProjects 로 보고한다")
    void 부분_실패_시_partial_보고() {
        // Given - 프로젝트 1개 생성하되 첫 add 이후 나머지 모두 실패
        Long gisuId = 9L;
        ChapterWithSchoolsInfo chapter = new ChapterWithSchoolsInfo(
            1L, "서울", List.of(new ChapterWithSchoolsInfo.SchoolInfo(101L, "건국대"))
        );
        Set<Long> pool = bigPool(13);
        given(getChapterUseCase.getChaptersWithSchoolsByGisuId(gisuId)).willReturn(List.of(chapter));
        given(getMemberUseCase.listIdsBySchoolIds(Set.of(101L))).willReturn(Map.of(101L, pool));
        given(getChallengerUseCase.listByMemberIdsAndGisuId(pool, gisuId)).willReturn(Map.of());
        given(manageChallengerUseCase.createChallenger(any())).willReturn(9000L);
        given(createDraftProjectUseCase.create(any())).willReturn(2000L);
        given(addProjectMemberUseCase.add(any()))
            .willReturn(1L)
            .willThrow(new RuntimeException("add boom"));

        // When
        SeedProjectsResult result = sut.seed(new SeedProjectsCommand(1, gisuId));

        // Then
        assertThat(result.createdProjectIds()).isEmpty();
        assertThat(result.partialProjects()).hasSize(1);
        SeedProjectsResult.PartialProject partial = result.partialProjects().get(0);
        assertThat(partial.projectId()).isEqualTo(2000L);
        assertThat(partial.addedMemberCount()).isEqualTo(1);
        assertThat(partial.expectedMemberCount()).isBetween(11, 13);
        assertThat(partial.reason()).contains("add boom");
    }

    @Test
    @DisplayName("createDraft 자체 실패는 failedCount 로 분류된다")
    void 프로젝트_생성_실패_failed_분류() {
        // Given
        Long gisuId = 9L;
        ChapterWithSchoolsInfo chapter = new ChapterWithSchoolsInfo(
            1L, "서울", List.of(new ChapterWithSchoolsInfo.SchoolInfo(101L, "건국대"))
        );
        Set<Long> pool = bigPool(13);
        given(getChapterUseCase.getChaptersWithSchoolsByGisuId(gisuId)).willReturn(List.of(chapter));
        given(getMemberUseCase.listIdsBySchoolIds(Set.of(101L))).willReturn(Map.of(101L, pool));
        given(getChallengerUseCase.listByMemberIdsAndGisuId(pool, gisuId)).willReturn(Map.of());
        given(manageChallengerUseCase.createChallenger(any())).willReturn(9000L);
        given(createDraftProjectUseCase.create(any()))
            .willThrow(new RuntimeException("create boom"));

        // When
        SeedProjectsResult result = sut.seed(new SeedProjectsCommand(1, gisuId));

        // Then
        assertThat(result.createdProjectIds()).isEmpty();
        assertThat(result.partialProjects()).isEmpty();
        assertThat(result.failedCount()).isGreaterThanOrEqualTo(1);
        verify(addProjectMemberUseCase, never()).add(any());
    }

    @Test
    @DisplayName("school 풀과 후보 챌린저는 batch 로 한 번만 조회하고 캐시를 재사용한다")
    void 프로젝트_시딩_batch_preload_캐시() {
        // Given
        Long gisuId = 9L;
        Set<Long> schoolOnePool = bigPoolRange(1, 13);
        Set<Long> schoolTwoPool = bigPoolRange(101, 113);
        Set<Long> allMemberIds = new HashSet<>();
        allMemberIds.addAll(schoolOnePool);
        allMemberIds.addAll(schoolTwoPool);
        ChapterWithSchoolsInfo chapter = new ChapterWithSchoolsInfo(
            1L, "서울", List.of(
            new ChapterWithSchoolsInfo.SchoolInfo(101L, "건국대"),
            new ChapterWithSchoolsInfo.SchoolInfo(102L, "숭실대")
        ));
        given(getChapterUseCase.getChaptersWithSchoolsByGisuId(gisuId)).willReturn(List.of(chapter));
        given(getMemberUseCase.listIdsBySchoolIds(Set.of(101L, 102L)))
            .willReturn(Map.of(101L, schoolOnePool, 102L, schoolTwoPool));
        given(getChallengerUseCase.listByMemberIdsAndGisuId(allMemberIds, gisuId)).willReturn(Map.of());
        given(manageChallengerUseCase.createChallenger(any())).willReturn(9000L, 9001L);
        AtomicLong projectIds = new AtomicLong(1000L);
        given(createDraftProjectUseCase.create(any())).willAnswer(inv -> projectIds.getAndIncrement());

        // When
        SeedProjectsResult result = sut.seed(new SeedProjectsCommand(2, gisuId));

        // Then
        assertThat(result.createdProjectIds()).hasSize(2);
        verify(getMemberUseCase, times(1)).listIdsBySchoolIds(Set.of(101L, 102L));
        verify(getMemberUseCase, never()).listIdsBySchoolId(anyLong());
        verify(getChallengerUseCase, times(1)).listByMemberIdsAndGisuId(allMemberIds, gisuId);
        verify(getChallengerUseCase, never()).findByMemberIdAndGisuId(anyLong(), eq(gisuId));
    }

    @Test
    @DisplayName("gisuId 가 null 이면 활성 기수를 사용한다")
    void gisuId_null_시_활성_기수_사용() {
        // Given
        given(getGisuUseCase.getActiveGisuId()).willReturn(10L);
        given(getChapterUseCase.getChaptersWithSchoolsByGisuId(10L)).willReturn(List.of());

        // When
        SeedProjectsResult result = sut.seed(new SeedProjectsCommand(1, null));

        // Then
        assertThat(result.createdProjectIds()).isEmpty();
        verify(getGisuUseCase, times(1)).getActiveGisuId();
    }

    @Test
    @DisplayName("모든 후보가 다른 파트 챌린저이면 PO 후보 없음으로 스킵한다")
    void PO_후보_없음_스킵() {
        Long gisuId = 9L;
        Set<Long> pool = bigPool(13);
        given(getChapterUseCase.getChaptersWithSchoolsByGisuId(gisuId)).willReturn(List.of(
            new ChapterWithSchoolsInfo(1L, "서울", List.of(
                new ChapterWithSchoolsInfo.SchoolInfo(101L, "건국대")
            ))
        ));
        given(getMemberUseCase.listIdsBySchoolIds(Set.of(101L))).willReturn(Map.of(101L, pool));
        Map<Long, ChallengerInfo> challengers = new java.util.HashMap<>();
        pool.forEach(memberId -> challengers.put(memberId, challenger(memberId, ChallengerPart.WEB)));
        given(getChallengerUseCase.listByMemberIdsAndGisuId(pool, gisuId)).willReturn(challengers);

        SeedProjectsResult result = sut.seed(new SeedProjectsCommand(1, gisuId));

        assertThat(result.skippedChapters()).singleElement()
            .satisfies(skipped -> assertThat(skipped.reason()).contains("NO_PO_CANDIDATE"));
        verify(createDraftProjectUseCase, never()).create(any());
    }

    @Test
    @DisplayName("기존 PLAN 챌린저를 PO로 재사용하면 챌린저를 중복 생성하지 않는다")
    void 기존_PLAN_PO_재사용() {
        Long gisuId = 9L;
        Set<Long> pool = bigPool(13);
        given(getChapterUseCase.getChaptersWithSchoolsByGisuId(gisuId)).willReturn(List.of(
            new ChapterWithSchoolsInfo(1L, "서울", List.of(
                new ChapterWithSchoolsInfo.SchoolInfo(101L, "건국대")
            ))
        ));
        given(getMemberUseCase.listIdsBySchoolIds(Set.of(101L))).willReturn(Map.of(101L, pool));
        Map<Long, ChallengerInfo> challengers = new java.util.HashMap<>();
        pool.forEach(memberId -> challengers.put(memberId, challenger(memberId, ChallengerPart.PLAN)));
        given(getChallengerUseCase.listByMemberIdsAndGisuId(pool, gisuId)).willReturn(challengers);
        given(createDraftProjectUseCase.create(any())).willReturn(1000L);

        SeedProjectsResult result = sut.seed(new SeedProjectsCommand(1, gisuId));

        assertThat(result.createdProjectIds()).containsExactly(1000L);
        verify(manageChallengerUseCase, never()).createChallenger(any());
    }

    private static Set<Long> bigPool(int size) {
        Set<Long> pool = new HashSet<>();
        for (long i = 1; i <= size; i++) {
            pool.add(i);
        }
        return pool;
    }

    private static Set<Long> bigPoolRange(long startInclusive, long endInclusive) {
        Set<Long> pool = new HashSet<>();
        for (long i = startInclusive; i <= endInclusive; i++) {
            pool.add(i);
        }
        return pool;
    }

    private static ChallengerInfo challenger(Long memberId, ChallengerPart part) {
        return ChallengerInfo.builder().memberId(memberId).part(part).build();
    }
}
