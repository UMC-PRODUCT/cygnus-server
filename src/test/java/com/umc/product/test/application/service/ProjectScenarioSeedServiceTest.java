package com.umc.product.test.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterInfo;
import com.umc.product.project.application.port.in.command.AddProjectMemberUseCase;
import com.umc.product.project.application.port.in.command.CreateDraftProjectUseCase;
import com.umc.product.project.application.port.in.command.PublishProjectUseCase;
import com.umc.product.project.application.port.in.command.SubmitProjectUseCase;
import com.umc.product.project.application.port.in.command.UpdatePartQuotasUseCase;
import com.umc.product.project.application.port.in.command.UpdateProjectUseCase;
import com.umc.product.project.application.port.in.command.UpsertProjectApplicationFormUseCase;
import com.umc.product.project.application.port.in.command.dto.CreateDraftProjectCommand;
import com.umc.product.project.application.port.in.command.dto.UpdatePartQuotasCommand.Entry;
import com.umc.product.project.application.port.in.query.dto.ApplicationFormInfo;
import com.umc.product.test.application.port.in.command.dto.SeedProjectScenariosCommand;
import com.umc.product.test.application.port.in.command.dto.SeedProjectScenariosResult;
import com.umc.product.test.application.port.in.command.dto.SeedProjectScenariosResult.CreatedProject;
import com.umc.product.test.application.port.in.command.dto.SeedProjectScenariosResult.FailedProject;
import com.umc.product.test.application.port.in.command.dto.TargetProjectStatus;

@ExtendWith(MockitoExtension.class)
class ProjectScenarioSeedServiceTest {

    private static final Long GISU_ID = 9L;
    private static final Long CHAPTER_ID = 1L;
    private static final Long SCHOOL_ID = 11L;
    private static final Long PO_MEMBER_ID = 100L;

    @Mock
    GetGisuUseCase getGisuUseCase;
    @Mock
    GetMemberUseCase getMemberUseCase;
    @Mock
    GetChallengerUseCase getChallengerUseCase;
    @Mock
    GetChapterUseCase getChapterUseCase;
    @Mock
    CreateDraftProjectUseCase createDraftProjectUseCase;
    @Mock
    UpdateProjectUseCase updateProjectUseCase;
    @Mock
    UpsertProjectApplicationFormUseCase upsertProjectApplicationFormUseCase;
    @Mock
    SubmitProjectUseCase submitProjectUseCase;
    @Mock
    UpdatePartQuotasUseCase updatePartQuotasUseCase;
    @Mock
    PublishProjectUseCase publishProjectUseCase;
    @Mock
    AddProjectMemberUseCase addProjectMemberUseCase;
    @Mock
    ScenarioPartQuotaPolicy scenarioPartQuotaPolicy;

    DummyProjectFactory dummyProjectFactory = new DummyProjectFactory();
    DummyApplicationFormFactory dummyApplicationFormFactory = new DummyApplicationFormFactory();
    ProjectScenarioSeedService sut;

    @BeforeEach
    void setUp() {
        sut = new ProjectScenarioSeedService(
            getGisuUseCase,
            getMemberUseCase,
            getChallengerUseCase,
            getChapterUseCase,
            createDraftProjectUseCase,
            updateProjectUseCase,
            upsertProjectApplicationFormUseCase,
            submitProjectUseCase,
            updatePartQuotasUseCase,
            publishProjectUseCase,
            addProjectMemberUseCase,
            dummyProjectFactory,
            dummyApplicationFormFactory,
            scenarioPartQuotaPolicy
        );
    }

    // ===== Helpers =====

    private ChallengerInfo challengerInfo(Long memberId, ChallengerPart part) {
        return challengerInfo(memberId, part, ChallengerStatus.ACTIVE);
    }

    private ChallengerInfo challengerInfo(Long memberId, ChallengerPart part, ChallengerStatus status) {
        return ChallengerInfo.builder()
            .challengerId(memberId * 10)
            .memberId(memberId)
            .gisuId(GISU_ID)
            .part(part)
            .challengerStatus(status)
            .build();
    }

    private MemberInfo memberInfo(Long memberId, Long schoolId) {
        return MemberInfo.builder()
            .id(memberId)
            .schoolId(schoolId)
            .build();
    }

    private ApplicationFormInfo applicationFormInfo(Long projectId, Long formId) {
        return ApplicationFormInfo.builder()
            .projectId(projectId)
            .applicationFormId(formId)
            .build();
    }

    private void givenActiveGisu() {
        given(getGisuUseCase.getActiveGisuId()).willReturn(GISU_ID);
    }

    private void givenPoIsPlanChallenger(Long poMemberId) {
        Map<Long, ChallengerInfo> map = new HashMap<>();
        map.put(poMemberId, challengerInfo(poMemberId, ChallengerPart.PLAN));
        given(getChallengerUseCase.listByMemberIdsAndGisuId(Set.of(poMemberId), GISU_ID))
            .willReturn(map);
    }

    private void givenPoMemberSchool(Long poMemberId, Long schoolId) {
        given(getMemberUseCase.getById(poMemberId)).willReturn(memberInfo(poMemberId, schoolId));
        given(getChapterUseCase.byGisuAndSchool(GISU_ID, schoolId))
            .willReturn(new ChapterInfo(CHAPTER_ID, "지부"));
    }

    private void givenCreateDraftReturns(Long projectId) {
        AtomicLong counter = new AtomicLong(projectId);
        given(createDraftProjectUseCase.create(any(CreateDraftProjectCommand.class)))
            .willAnswer(inv -> counter.getAndIncrement());
    }

    // ===== Test Cases =====

    @Nested
    @DisplayName("DRAFT 시나리오")
    class DraftScenario {

        @Test
        @DisplayName("createDraft와 updateProject만 호출되고 form/submit/publish는 호출되지 않는다")
        void calls_only_create_and_update() {
            givenActiveGisu();
            givenPoIsPlanChallenger(PO_MEMBER_ID);
            givenPoMemberSchool(PO_MEMBER_ID, SCHOOL_ID);
            givenCreateDraftReturns(500L);

            SeedProjectScenariosResult result = sut.seed(new SeedProjectScenariosCommand(
                TargetProjectStatus.DRAFT, 1, List.of(PO_MEMBER_ID)
            ));

            assertThat(result.createdProjects()).hasSize(1);
            CreatedProject created = result.createdProjects().get(0);
            assertThat(created.projectId()).isEqualTo(500L);
            assertThat(created.finalStatus()).isEqualTo(TargetProjectStatus.DRAFT);
            assertThat(created.productOwnerMemberId()).isEqualTo(PO_MEMBER_ID);
            assertThat(created.chapterId()).isEqualTo(CHAPTER_ID);
            assertThat(created.schoolId()).isEqualTo(SCHOOL_ID);
            assertThat(created.applicationFormId()).isNull();
            assertThat(created.partFills()).isNull();

            then(createDraftProjectUseCase).should().create(any());
            then(updateProjectUseCase).should().update(any());
            then(upsertProjectApplicationFormUseCase).should(never()).upsert(any());
            then(submitProjectUseCase).should(never()).submit(any());
            then(updatePartQuotasUseCase).should(never()).update(any());
            then(publishProjectUseCase).should(never()).publish(any());
            then(addProjectMemberUseCase).should(never()).add(any());
        }
    }

    @Nested
    @DisplayName("PENDING_REVIEW 시나리오")
    class PendingReviewScenario {

        @Test
        @DisplayName("DRAFT 단계 호출에 더해 upsertForm + submit이 호출되고 publish는 호출되지 않는다")
        void runs_through_submit_but_not_publish() {
            givenActiveGisu();
            givenPoIsPlanChallenger(PO_MEMBER_ID);
            givenPoMemberSchool(PO_MEMBER_ID, SCHOOL_ID);
            givenCreateDraftReturns(501L);
            given(upsertProjectApplicationFormUseCase.upsert(any()))
                .willReturn(applicationFormInfo(501L, 9001L));

            SeedProjectScenariosResult result = sut.seed(new SeedProjectScenariosCommand(
                TargetProjectStatus.PENDING_REVIEW, 1, List.of(PO_MEMBER_ID)
            ));

            assertThat(result.createdProjects()).hasSize(1);
            CreatedProject created = result.createdProjects().get(0);
            assertThat(created.finalStatus()).isEqualTo(TargetProjectStatus.PENDING_REVIEW);
            assertThat(created.applicationFormId()).isEqualTo(9001L);
            assertThat(created.partFills()).isNull();

            then(upsertProjectApplicationFormUseCase).should().upsert(any());
            then(submitProjectUseCase).should().submit(any());
            then(updatePartQuotasUseCase).should(never()).update(any());
            then(publishProjectUseCase).should(never()).publish(any());
        }
    }

    @Nested
    @DisplayName("IN_PROGRESS 시나리오")
    class InProgressScenario {

        @Test
        @DisplayName("전체 단계가 호출되고 PartFill이 quota 범위 내에서 채워진다")
        void runs_all_steps_and_fills_members_within_quota() {
            givenActiveGisu();
            givenPoIsPlanChallenger(PO_MEMBER_ID);
            givenPoMemberSchool(PO_MEMBER_ID, SCHOOL_ID);
            givenCreateDraftReturns(502L);
            given(upsertProjectApplicationFormUseCase.upsert(any()))
                .willReturn(applicationFormInfo(502L, 9002L));

            List<Entry> quotas = List.of(
                Entry.builder().part(ChallengerPart.DESIGN).quota(2L).build(),
                Entry.builder().part(ChallengerPart.WEB).quota(4L).build(),
                Entry.builder().part(ChallengerPart.NODEJS).quota(3L).build()
            );
            given(scenarioPartQuotaPolicy.pickQuotas()).willReturn(quotas);

            List<ChallengerInfo> sameSchoolChallengers = List.of(
                challengerInfo(201L, ChallengerPart.DESIGN),
                challengerInfo(202L, ChallengerPart.DESIGN),
                challengerInfo(203L, ChallengerPart.WEB),
                challengerInfo(204L, ChallengerPart.WEB),
                challengerInfo(205L, ChallengerPart.WEB),
                challengerInfo(206L, ChallengerPart.WEB),
                challengerInfo(207L, ChallengerPart.NODEJS),
                challengerInfo(208L, ChallengerPart.NODEJS),
                challengerInfo(209L, ChallengerPart.NODEJS),
                challengerInfo(PO_MEMBER_ID, ChallengerPart.PLAN)
            );
            given(getChallengerUseCase.getAllByGisuId(GISU_ID)).willReturn(sameSchoolChallengers);

            Map<Long, Long> memberSchoolMap = new HashMap<>();
            for (long id = 201L; id <= 209L; id++) {
                memberSchoolMap.put(id, SCHOOL_ID);
            }
            given(getMemberUseCase.findAllSchoolIdsByIds(any())).willReturn(memberSchoolMap);

            SeedProjectScenariosResult result = sut.seed(new SeedProjectScenariosCommand(
                TargetProjectStatus.IN_PROGRESS, 1, List.of(PO_MEMBER_ID)
            ));

            assertThat(result.createdProjects()).hasSize(1);
            CreatedProject created = result.createdProjects().get(0);
            assertThat(created.finalStatus()).isEqualTo(TargetProjectStatus.IN_PROGRESS);
            assertThat(created.partFills()).hasSize(3);
            assertThat(created.partFills().get(0).part()).isEqualTo(ChallengerPart.DESIGN);
            assertThat(created.partFills().get(0).quota()).isEqualTo(2L);
            assertThat(created.partFills().get(0).filled()).isBetween(0L, 2L);
            assertThat(created.partFills().get(1).part()).isEqualTo(ChallengerPart.WEB);
            assertThat(created.partFills().get(1).filled()).isBetween(0L, 4L);
            assertThat(created.partFills().get(2).part()).isEqualTo(ChallengerPart.NODEJS);
            assertThat(created.partFills().get(2).filled()).isBetween(0L, 3L);

            then(updatePartQuotasUseCase).should().update(any());
            then(publishProjectUseCase).should().publish(any());
        }

        @Test
        @DisplayName("PO 본인은 멤버 충원에서 제외된다")
        void po_is_excluded_from_member_pool() {
            givenActiveGisu();
            givenPoIsPlanChallenger(PO_MEMBER_ID);
            givenPoMemberSchool(PO_MEMBER_ID, SCHOOL_ID);
            givenCreateDraftReturns(503L);
            given(upsertProjectApplicationFormUseCase.upsert(any()))
                .willReturn(applicationFormInfo(503L, 9003L));
            given(scenarioPartQuotaPolicy.pickQuotas()).willReturn(List.of(
                Entry.builder().part(ChallengerPart.DESIGN).quota(2L).build(),
                Entry.builder().part(ChallengerPart.WEB).quota(4L).build(),
                Entry.builder().part(ChallengerPart.NODEJS).quota(3L).build()
            ));
            given(getChallengerUseCase.getAllByGisuId(GISU_ID))
                .willReturn(List.of(challengerInfo(PO_MEMBER_ID, ChallengerPart.PLAN)));

            sut.seed(new SeedProjectScenariosCommand(
                TargetProjectStatus.IN_PROGRESS, 1, List.of(PO_MEMBER_ID)
            ));

            // PO 만 챌린저로 등록된 상태 → fillMembers 의 pre-filter 단계에서 모든 풀이 비어 addMember 미호출
            then(addProjectMemberUseCase).should(never()).add(any());
        }

        @Test
        @DisplayName("멤버 풀이 비어있어도 partFills는 quota entry 수만큼 0으로 채워서 반환한다")
        void empty_pool_returns_zero_filled_entries() {
            givenActiveGisu();
            givenPoIsPlanChallenger(PO_MEMBER_ID);
            givenPoMemberSchool(PO_MEMBER_ID, SCHOOL_ID);
            givenCreateDraftReturns(504L);
            given(upsertProjectApplicationFormUseCase.upsert(any()))
                .willReturn(applicationFormInfo(504L, 9004L));
            given(scenarioPartQuotaPolicy.pickQuotas()).willReturn(List.of(
                Entry.builder().part(ChallengerPart.DESIGN).quota(2L).build(),
                Entry.builder().part(ChallengerPart.WEB).quota(4L).build(),
                Entry.builder().part(ChallengerPart.NODEJS).quota(3L).build()
            ));
            given(getChallengerUseCase.getAllByGisuId(GISU_ID)).willReturn(List.of());

            SeedProjectScenariosResult result = sut.seed(new SeedProjectScenariosCommand(
                TargetProjectStatus.IN_PROGRESS, 1, List.of(PO_MEMBER_ID)
            ));

            CreatedProject created = result.createdProjects().get(0);
            assertThat(created.partFills()).hasSize(3);
            assertThat(created.partFills()).allMatch(f -> f.filled() == 0L);
            assertThat(created.partFills().get(0).quota()).isEqualTo(2L);
            assertThat(created.partFills().get(1).quota()).isEqualTo(4L);
            assertThat(created.partFills().get(2).quota()).isEqualTo(3L);
        }

        @Test
        @DisplayName("비활성·이미 선택된 멤버를 제외하고 개별 추가 실패를 해당 파트에 격리한다")
        void 멤버_필터와_추가_실패_격리() {
            givenActiveGisu();
            givenPoIsPlanChallenger(PO_MEMBER_ID);
            givenPoMemberSchool(PO_MEMBER_ID, SCHOOL_ID);
            givenCreateDraftReturns(505L);
            given(upsertProjectApplicationFormUseCase.upsert(any()))
                .willReturn(applicationFormInfo(505L, 9005L));
            given(scenarioPartQuotaPolicy.pickQuotas()).willReturn(List.of(
                Entry.builder().part(ChallengerPart.WEB).quota(1L).build(),
                Entry.builder().part(ChallengerPart.WEB).quota(1L).build()
            ));
            given(getChallengerUseCase.getAllByGisuId(GISU_ID)).willReturn(List.of(
                challengerInfo(201L, ChallengerPart.WEB),
                challengerInfo(202L, ChallengerPart.WEB),
                challengerInfo(203L, ChallengerPart.WEB, ChallengerStatus.GRADUATED)
            ));
            given(getMemberUseCase.findAllSchoolIdsByIds(Set.of(201L, 202L)))
                .willReturn(Map.of(201L, SCHOOL_ID, 202L, SCHOOL_ID));
            given(addProjectMemberUseCase.add(any()))
                .willReturn(1L)
                .willThrow(new RuntimeException("add boom"));
            sut = spy(sut);
            doReturn(1L, 1L).when(sut).nextFillCount(1L);

            SeedProjectScenariosResult result = sut.seed(new SeedProjectScenariosCommand(
                TargetProjectStatus.IN_PROGRESS, 1, List.of(PO_MEMBER_ID)
            ));

            assertThat(result.createdProjects().get(0).partFills())
                .extracting(SeedProjectScenariosResult.PartFill::filled)
                .containsExactly(1L, 0L);
        }

        @Test
        @DisplayName("무작위 충원 수는 0부터 quota까지의 범위다")
        void 무작위_충원_범위() {
            assertThat(sut.nextFillCount(3L)).isBetween(0L, 3L);
        }
    }

    @Nested
    @DisplayName("PO 입력 검증")
    class PoValidation {

        @Test
        @DisplayName("productOwnerMemberIds size가 projectCount와 다르면 IllegalArgumentException")
        void size_mismatch_throws() {
            givenActiveGisu();

            assertThatThrownBy(() -> sut.seed(new SeedProjectScenariosCommand(
                TargetProjectStatus.DRAFT, 3, List.of(100L, 101L)
            ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size");
        }

        @Test
        @DisplayName("입력된 PO가 PLAN 챌린저가 아니면 IllegalArgumentException")
        void non_plan_po_throws() {
            givenActiveGisu();
            Map<Long, ChallengerInfo> map = new HashMap<>();
            map.put(100L, challengerInfo(100L, ChallengerPart.WEB));
            given(getChallengerUseCase.listByMemberIdsAndGisuId(Set.of(100L), GISU_ID))
                .willReturn(map);

            assertThatThrownBy(() -> sut.seed(new SeedProjectScenariosCommand(
                TargetProjectStatus.DRAFT, 1, List.of(100L)
            ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PLAN");
        }

        @Test
        @DisplayName("입력된 PO가 챌린저가 아니면 IllegalArgumentException")
        void missing_challenger_throws() {
            givenActiveGisu();
            given(getChallengerUseCase.listByMemberIdsAndGisuId(Set.of(100L), GISU_ID))
                .willReturn(Map.of());

            assertThatThrownBy(() -> sut.seed(new SeedProjectScenariosCommand(
                TargetProjectStatus.DRAFT, 1, List.of(100L)
            ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not a challenger");
        }
    }

    @Nested
    @DisplayName("랜덤 PO 픽")
    class RandomPoPick {

        @Test
        @DisplayName("PO 입력이 null이고 PLAN 풀이 부족하면 IllegalArgumentException")
        void insufficient_pool_throws() {
            givenActiveGisu();
            given(getChallengerUseCase.getAllByGisuId(GISU_ID))
                .willReturn(List.of(challengerInfo(100L, ChallengerPart.PLAN)));

            assertThatThrownBy(() -> sut.seed(new SeedProjectScenariosCommand(
                TargetProjectStatus.DRAFT, 3, null
            ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pool size");
        }

        @Test
        @DisplayName("PO 입력이 null이면 PLAN ACTIVE 챌린저 풀에서 N명 픽한다")
        void picks_random_pos_from_active_plan_pool() {
            givenActiveGisu();
            given(getChallengerUseCase.getAllByGisuId(GISU_ID)).willReturn(List.of(
                challengerInfo(100L, ChallengerPart.PLAN),
                challengerInfo(101L, ChallengerPart.PLAN),
                challengerInfo(102L, ChallengerPart.PLAN),
                challengerInfo(103L, ChallengerPart.WEB),
                challengerInfo(104L, ChallengerPart.PLAN, ChallengerStatus.GRADUATED)
            ));
            given(getMemberUseCase.getById(anyLong())).willAnswer(inv ->
                memberInfo(inv.getArgument(0), SCHOOL_ID));
            given(getChapterUseCase.byGisuAndSchool(GISU_ID, SCHOOL_ID))
                .willReturn(new ChapterInfo(CHAPTER_ID, "지부"));
            givenCreateDraftReturns(600L);

            SeedProjectScenariosResult result = sut.seed(new SeedProjectScenariosCommand(
                TargetProjectStatus.DRAFT, 2, null
            ));

            assertThat(result.createdProjects()).hasSize(2);
            assertThat(result.createdProjects())
                .allMatch(p -> List.of(100L, 101L, 102L).contains(p.productOwnerMemberId()));
        }
    }

    @Nested
    @DisplayName("단계별 실패 격리")
    class FailureIsolation {

        @Test
        @DisplayName("프로젝트 정보 갱신 실패는 UPDATE 단계로 기록된다")
        void update_failure_recorded() {
            givenActiveGisu();
            givenPoIsPlanChallenger(PO_MEMBER_ID);
            givenCreateDraftReturns(698L);
            willThrow(new RuntimeException("update boom"))
                .given(updateProjectUseCase).update(any());

            SeedProjectScenariosResult result = sut.seed(new SeedProjectScenariosCommand(
                TargetProjectStatus.DRAFT, 1, List.of(PO_MEMBER_ID)
            ));

            assertThat(result.failedProjects()).singleElement()
                .satisfies(failure -> assertThat(failure.failedStep()).isEqualTo("UPDATE"));
        }

        @Test
        @DisplayName("지원 폼 생성 실패는 FORM 단계로 기록된다")
        void form_failure_recorded() {
            givenActiveGisu();
            givenPoIsPlanChallenger(PO_MEMBER_ID);
            givenPoMemberSchool(PO_MEMBER_ID, SCHOOL_ID);
            givenCreateDraftReturns(699L);
            given(upsertProjectApplicationFormUseCase.upsert(any()))
                .willThrow(new RuntimeException("form boom"));

            SeedProjectScenariosResult result = sut.seed(new SeedProjectScenariosCommand(
                TargetProjectStatus.PENDING_REVIEW, 1, List.of(PO_MEMBER_ID)
            ));

            assertThat(result.failedProjects()).singleElement()
                .satisfies(failure -> assertThat(failure.failedStep()).isEqualTo("FORM"));
        }

        @Test
        @DisplayName("파트 quota 갱신 실패는 QUOTA 단계로 기록된다")
        void quota_failure_recorded() {
            givenActiveGisu();
            givenPoIsPlanChallenger(PO_MEMBER_ID);
            givenPoMemberSchool(PO_MEMBER_ID, SCHOOL_ID);
            givenCreateDraftReturns(699L);
            given(upsertProjectApplicationFormUseCase.upsert(any()))
                .willReturn(applicationFormInfo(699L, 9699L));
            given(scenarioPartQuotaPolicy.pickQuotas()).willReturn(List.of(
                Entry.builder().part(ChallengerPart.WEB).quota(1L).build()
            ));
            willThrow(new RuntimeException("quota boom"))
                .given(updatePartQuotasUseCase).update(any());

            SeedProjectScenariosResult result = sut.seed(new SeedProjectScenariosCommand(
                TargetProjectStatus.IN_PROGRESS, 1, List.of(PO_MEMBER_ID)
            ));

            assertThat(result.failedProjects()).singleElement()
                .satisfies(failure -> assertThat(failure.failedStep()).isEqualTo("QUOTA"));
        }

        @Test
        @DisplayName("submit 실패 시 reachedStatus=DRAFT, failedStep=SUBMIT으로 failedProjects에 기록된다")
        void submit_failure_recorded_with_draft_reached() {
            givenActiveGisu();
            givenPoIsPlanChallenger(PO_MEMBER_ID);
            givenPoMemberSchool(PO_MEMBER_ID, SCHOOL_ID);
            givenCreateDraftReturns(700L);
            given(upsertProjectApplicationFormUseCase.upsert(any()))
                .willReturn(applicationFormInfo(700L, 9700L));
            willThrow(new RuntimeException("submit boom"))
                .given(submitProjectUseCase).submit(any());

            SeedProjectScenariosResult result = sut.seed(new SeedProjectScenariosCommand(
                TargetProjectStatus.PENDING_REVIEW, 1, List.of(PO_MEMBER_ID)
            ));

            assertThat(result.createdProjects()).isEmpty();
            assertThat(result.failedProjects()).hasSize(1);
            FailedProject failed = result.failedProjects().get(0);
            assertThat(failed.projectId()).isEqualTo(700L);
            assertThat(failed.reachedStatus()).isEqualTo(TargetProjectStatus.DRAFT);
            assertThat(failed.intendedStatus()).isEqualTo(TargetProjectStatus.PENDING_REVIEW);
            assertThat(failed.failedStep()).isEqualTo("SUBMIT");
            assertThat(failed.reason()).contains("submit boom");
        }

        @Test
        @DisplayName("publish 실패 시 reachedStatus=PENDING_REVIEW, failedStep=PUBLISH로 기록된다")
        void publish_failure_recorded_with_pending_review_reached() {
            givenActiveGisu();
            givenPoIsPlanChallenger(PO_MEMBER_ID);
            givenPoMemberSchool(PO_MEMBER_ID, SCHOOL_ID);
            givenCreateDraftReturns(701L);
            given(upsertProjectApplicationFormUseCase.upsert(any()))
                .willReturn(applicationFormInfo(701L, 9701L));
            given(scenarioPartQuotaPolicy.pickQuotas()).willReturn(List.of(
                Entry.builder().part(ChallengerPart.DESIGN).quota(1L).build()
            ));
            willThrow(new RuntimeException("publish boom"))
                .given(publishProjectUseCase).publish(any());

            SeedProjectScenariosResult result = sut.seed(new SeedProjectScenariosCommand(
                TargetProjectStatus.IN_PROGRESS, 1, List.of(PO_MEMBER_ID)
            ));

            assertThat(result.createdProjects()).isEmpty();
            assertThat(result.failedProjects()).hasSize(1);
            FailedProject failed = result.failedProjects().get(0);
            assertThat(failed.projectId()).isEqualTo(701L);
            assertThat(failed.reachedStatus()).isEqualTo(TargetProjectStatus.PENDING_REVIEW);
            assertThat(failed.intendedStatus()).isEqualTo(TargetProjectStatus.IN_PROGRESS);
            assertThat(failed.failedStep()).isEqualTo("PUBLISH");
            assertThat(failed.reason()).contains("publish boom");
        }

        @Test
        @DisplayName("한 프로젝트가 실패해도 다른 프로젝트의 시딩은 계속 진행된다")
        void one_failure_does_not_block_others() {
            Long poA = 100L;
            Long poB = 101L;
            givenActiveGisu();
            Map<Long, ChallengerInfo> map = new HashMap<>();
            map.put(poA, challengerInfo(poA, ChallengerPart.PLAN));
            map.put(poB, challengerInfo(poB, ChallengerPart.PLAN));
            given(getChallengerUseCase.listByMemberIdsAndGisuId(Set.of(poA, poB), GISU_ID))
                .willReturn(map);
            given(getMemberUseCase.getById(poB)).willReturn(memberInfo(poB, SCHOOL_ID));
            given(getChapterUseCase.byGisuAndSchool(GISU_ID, SCHOOL_ID))
                .willReturn(new ChapterInfo(CHAPTER_ID, "지부"));

            AtomicLong idSeq = new AtomicLong(800L);
            given(createDraftProjectUseCase.create(any())).willAnswer(inv -> {
                CreateDraftProjectCommand cmd = inv.getArgument(0);
                if (cmd.productOwnerMemberId().equals(poA)) {
                    throw new RuntimeException("draft boom A");
                }
                return idSeq.getAndIncrement();
            });

            SeedProjectScenariosResult result = sut.seed(new SeedProjectScenariosCommand(
                TargetProjectStatus.DRAFT, 2, List.of(poA, poB)
            ));

            assertThat(result.createdProjects()).hasSize(1);
            assertThat(result.createdProjects().get(0).productOwnerMemberId()).isEqualTo(poB);
            assertThat(result.failedProjects()).hasSize(1);
            assertThat(result.failedProjects().get(0).failedStep()).isEqualTo("CREATE_DRAFT");
            assertThat(result.failedProjects().get(0).projectId()).isNull();
            assertThat(result.failedProjects().get(0).reachedStatus()).isNull();
        }
    }
}
