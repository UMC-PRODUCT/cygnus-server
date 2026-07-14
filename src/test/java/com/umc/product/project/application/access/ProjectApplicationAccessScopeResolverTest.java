package com.umc.product.project.application.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.SubjectPolicyFacts;
import com.umc.product.authorization.domain.policy.PolicyDecision;
import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.authorization.domain.policy.PolicyResolvedOutcome;
import com.umc.product.authorization.domain.policy.PolicyValue;
import com.umc.product.project.application.access.ProjectApplicationAccessScope.All;
import com.umc.product.project.application.access.ProjectApplicationAccessScope.AllInGisu;
import com.umc.product.project.application.access.ProjectApplicationAccessScope.ChapterScoped;
import com.umc.product.project.application.access.ProjectApplicationAccessScope.None;
import com.umc.product.project.application.access.ProjectApplicationAccessScope.OwnerOnly;
import com.umc.product.project.application.access.ProjectApplicationAccessScope.ProjectScoped;
import com.umc.product.project.application.authorization.ProjectPolicyAction;
import com.umc.product.project.application.authorization.ProjectPolicyAuthorizationService;
import com.umc.product.project.application.authorization.ProjectPolicyOutcomes;
import com.umc.product.project.application.authorization.ProjectPolicyPrincipal;
import com.umc.product.project.application.authorization.ProjectPolicyResourceContext;
import com.umc.product.project.application.authorization.ProjectPolicySubjectSnapshot;
import com.umc.product.project.application.port.out.LoadProjectMemberPort;
import com.umc.product.project.domain.Project;

@ExtendWith(MockitoExtension.class)
class ProjectApplicationAccessScopeResolverTest {

    private static final long MEMBER_ID = 10L;
    private static final long OWNER_ID = 99L;
    private static final long PROJECT_ID = 100L;
    private static final long GISU_ID = 1L;
    private static final long CHAPTER_ID = 5L;
    private static final Instant EVALUATED_AT = Instant.parse("2026-07-13T00:00:00Z");
    private static final ProjectPolicySubjectSnapshot SNAPSHOT = new ProjectPolicySubjectSnapshot(
        new ProjectPolicyPrincipal.Member(MEMBER_ID), EVALUATED_AT, List.of(), List.of(), Map.of());

    @Mock
    ProjectPolicyAuthorizationService policyAuthorizationService;

    @Mock
    LoadProjectMemberPort loadProjectMemberPort;

    @InjectMocks
    ProjectApplicationAccessScopeResolver sut;

    @ParameterizedTest(name = "{0}")
    @MethodSource("projectListAllowCases")
    @DisplayName("target policy가 허용한 역할은 프로젝트 지원서 scope를 받는다")
    void projectListMapsAllowedPolicyOutcome(
        String scenario,
        boolean activePlanMember,
        boolean includeOngoingRounds
    ) {
        // given
        Project project = project(PROJECT_ID, OWNER_ID);
        givenProjectDecision(activePlanMember, allowProject(PROJECT_ID, includeOngoingRounds));

        // when
        ProjectApplicationAccessScope scope = sut.resolveForProjectApplicantList(MEMBER_ID, project);

        // then
        assertThat(scope).as(scenario).isEqualTo(new ProjectScoped(PROJECT_ID, includeOngoingRounds));
        ArgumentCaptor<ProjectPolicyResourceContext> context =
            ArgumentCaptor.forClass(ProjectPolicyResourceContext.class);
        verify(policyAuthorizationService).evaluate(
            eq(SNAPSHOT), eq(ProjectPolicyAction.APPLICATION_LIST_PROJECT), context.capture());
        assertThat(context.getValue().activePlanMember()).isEqualTo(activePlanMember);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("projectListDenyCases")
    @DisplayName("target policy가 거부한 역할은 프로젝트 지원서 scope가 없다")
    void projectListMapsDeniedPolicyDecisionToNone(String scenario) {
        // given
        Project project = project(PROJECT_ID, OWNER_ID);
        givenProjectDecision(false, deny());

        // when
        ProjectApplicationAccessScope scope = sut.resolveForProjectApplicantList(MEMBER_ID, project);

        // then
        assertThat(scope).as(scenario).isEqualTo(new None());
    }

    @Test
    @DisplayName("ALLOW여도 현재 프로젝트 ID outcome이 없으면 scope가 없다")
    void projectListRequiresCurrentProjectIdOutcome() {
        // given
        Project project = project(PROJECT_ID, OWNER_ID);
        givenProjectDecision(false, allow(List.of()));

        // when
        ProjectApplicationAccessScope scope = sut.resolveForProjectApplicantList(MEMBER_ID, project);

        // then
        assertThat(scope).isEqualTo(new None());
    }

    @Test
    @DisplayName("batch는 subject snapshot과 활성 PLAN 멤버 조회를 각각 한 번만 수행한다")
    void batchLoadsSnapshotAndPlanMembershipOnce() {
        // given
        Project ownerProject = project(PROJECT_ID, MEMBER_ID);
        Project subPmProject = project(101L, OWNER_ID);
        Set<Long> projectIds = Set.of(PROJECT_ID, 101L);
        given(policyAuthorizationService.snapshot(MEMBER_ID)).willReturn(SNAPSHOT);
        given(loadProjectMemberPort.listProjectIdsByActivePlanMember(projectIds, MEMBER_ID))
            .willReturn(List.of(101L));
        given(policyAuthorizationService.evaluate(
            eq(SNAPSHOT), eq(ProjectPolicyAction.APPLICATION_LIST_PROJECT_BATCH), any()))
            .willReturn(allowProject(PROJECT_ID, false), allowProject(101L, false));

        // when
        Map<Long, ProjectApplicationAccessScope> scopes =
            sut.resolveForProjectApplicantLists(MEMBER_ID, List.of(ownerProject, subPmProject));

        // then
        assertThat(scopes).containsExactlyInAnyOrderEntriesOf(Map.of(
            PROJECT_ID, new ProjectScoped(PROJECT_ID),
            101L, new ProjectScoped(101L)
        ));
        verify(policyAuthorizationService, times(1)).snapshot(MEMBER_ID);
        verify(loadProjectMemberPort, times(1)).listProjectIdsByActivePlanMember(projectIds, MEMBER_ID);
        verify(loadProjectMemberPort, never()).isActivePlanMember(any(), any());
        ArgumentCaptor<ProjectPolicyResourceContext> contexts =
            ArgumentCaptor.forClass(ProjectPolicyResourceContext.class);
        verify(policyAuthorizationService, times(2)).evaluate(
            eq(SNAPSHOT), eq(ProjectPolicyAction.APPLICATION_LIST_PROJECT_BATCH), contexts.capture());
        assertThat(contexts.getAllValues()).extracting(ProjectPolicyResourceContext::activePlanMember)
            .containsExactly(false, true);
    }

    @Test
    @DisplayName("batch는 전달받은 subject의 snapshot을 재사용한다")
    void batchReusesProvidedSubjectSnapshot() {
        SubjectAttributes subject = new SubjectPolicyFacts(
            EVALUATED_AT, List.of(), List.of(), Map.of()
        ).toSubjectAttributes(MEMBER_ID, 1L);
        Project project = project(PROJECT_ID, OWNER_ID);
        given(policyAuthorizationService.snapshot(subject)).willReturn(SNAPSHOT);
        given(loadProjectMemberPort.listProjectIdsByActivePlanMember(Set.of(PROJECT_ID), MEMBER_ID))
            .willReturn(List.of());
        given(policyAuthorizationService.evaluate(
            eq(SNAPSHOT), eq(ProjectPolicyAction.APPLICATION_LIST_PROJECT_BATCH), any()))
            .willReturn(allowProject(PROJECT_ID, false));

        Map<Long, ProjectApplicationAccessScope> result =
            sut.resolveForProjectApplicantLists(subject, List.of(project));

        assertThat(result).containsEntry(PROJECT_ID, new ProjectScoped(PROJECT_ID));
        verify(policyAuthorizationService).snapshot(subject);
        verify(policyAuthorizationService, never()).snapshot(MEMBER_ID);
    }

    @Test
    @DisplayName("본인 지원서 정책 outcome은 OwnerOnly로 변환한다")
    void selfListMapsOwnerOutcome() {
        // given
        given(policyAuthorizationService.snapshot(MEMBER_ID)).willReturn(SNAPSHOT);
        given(policyAuthorizationService.evaluate(
            eq(SNAPSHOT), eq(ProjectPolicyAction.APPLICATION_LIST_SELF), any()))
            .willReturn(allow(List.of(outcome(
                ProjectPolicyOutcomes.APPLICATION_OWNER_MEMBER_IDS,
                new PolicyValue.LongSetValue(Set.of(MEMBER_ID))))));

        // when
        ProjectApplicationAccessScope scope = sut.resolveForApplicant(MEMBER_ID);

        // then
        assertThat(scope).isEqualTo(new OwnerOnly(MEMBER_ID));
    }

    @Test
    @DisplayName("관리 정책의 all outcome은 전체 scope로 변환한다")
    void managementMapsAllOutcome() {
        // given
        givenManagementDecision(allow(List.of(outcome(
            ProjectPolicyOutcomes.APPLICATION_ALL, new PolicyValue.BooleanValue(true)))));

        // when
        ProjectApplicationAccessScope scope = sut.resolveForManagement(MEMBER_ID, GISU_ID);

        // then
        assertThat(scope).isEqualTo(new All());
    }

    @Test
    @DisplayName("관리 정책의 gisu outcome은 해당 기수 scope로 변환한다")
    void managementMapsGisuOutcome() {
        // given
        givenManagementDecision(allow(List.of(outcome(
            ProjectPolicyOutcomes.APPLICATION_GISU_IDS,
            new PolicyValue.LongSetValue(Set.of(GISU_ID))))));

        // when
        ProjectApplicationAccessScope scope = sut.resolveForManagement(MEMBER_ID, GISU_ID);

        // then
        assertThat(scope).isEqualTo(new AllInGisu(GISU_ID));
    }

    @Test
    @DisplayName("관리 정책의 chapter outcome은 정렬된 지부 scope로 변환한다")
    void managementMapsChapterOutcome() {
        // given
        givenManagementDecision(allow(List.of(outcome(
            ProjectPolicyOutcomes.APPLICATION_CHAPTER_IDS,
            new PolicyValue.LongSetValue(Set.of(8L, CHAPTER_ID))))));

        // when
        ProjectApplicationAccessScope scope = sut.resolveForManagement(MEMBER_ID, GISU_ID);

        // then
        assertThat(scope).isEqualTo(new ChapterScoped(List.of(CHAPTER_ID, 8L), GISU_ID));
    }

    @Test
    @DisplayName("관리 정책이 거부되면 scope가 없다")
    void managementMapsDenyToNone() {
        // given
        givenManagementDecision(deny());

        // when
        ProjectApplicationAccessScope scope = sut.resolveForManagement(MEMBER_ID, GISU_ID);

        // then
        assertThat(scope).isEqualTo(new None());
    }

    private void givenProjectDecision(boolean activePlanMember, PolicyDecision decision) {
        given(policyAuthorizationService.snapshot(MEMBER_ID)).willReturn(SNAPSHOT);
        given(loadProjectMemberPort.isActivePlanMember(PROJECT_ID, MEMBER_ID)).willReturn(activePlanMember);
        given(policyAuthorizationService.evaluate(
            eq(SNAPSHOT), eq(ProjectPolicyAction.APPLICATION_LIST_PROJECT), any()))
            .willReturn(decision);
    }

    private void givenManagementDecision(PolicyDecision decision) {
        given(policyAuthorizationService.snapshot(MEMBER_ID)).willReturn(SNAPSHOT);
        given(policyAuthorizationService.evaluate(
            eq(SNAPSHOT), eq(ProjectPolicyAction.APPLICATION_LIST_MANAGEMENT), any()))
            .willReturn(decision);
    }

    private static Stream<Arguments> projectListAllowCases() {
        return Stream.of(
            Arguments.of("PO", false, false),
            Arguments.of("활성 PLAN Sub-PM", true, false),
            Arguments.of("활성 SUPER_ADMIN", false, true),
            Arguments.of("같은 기수 중앙 운영진", false, true),
            Arguments.of("같은 지부 지부장", false, false)
        );
    }

    private static Stream<Arguments> projectListDenyCases() {
        return Stream.of(
            Arguments.of("학교 운영진"),
            Arguments.of("만료된 운영진"),
            Arguments.of("다른 기수 중앙 운영진"),
            Arguments.of("다른 지부 지부장"),
            Arguments.of("매칭 statement 없음")
        );
    }

    private static Project project(long id, long ownerId) {
        Project project = Project.createDraft(GISU_ID, CHAPTER_ID, ownerId, 7L, ownerId);
        ReflectionTestUtils.setField(project, "id", id);
        return project;
    }

    private static PolicyDecision allowProject(long projectId, boolean includeOngoingRounds) {
        return allow(List.of(
            outcome(ProjectPolicyOutcomes.APPLICATION_PROJECT_IDS,
                new PolicyValue.LongSetValue(Set.of(projectId))),
            outcome(ProjectPolicyOutcomes.APPLICATION_INCLUDE_ONGOING_ROUNDS,
                new PolicyValue.BooleanValue(includeOngoingRounds))
        ));
    }

    private static PolicyDecision allow(List<PolicyResolvedOutcome> outcomes) {
        return decision(PolicyEffect.ALLOW, outcomes);
    }

    private static PolicyDecision deny() {
        return decision(PolicyEffect.DENY, List.of());
    }

    private static PolicyDecision decision(PolicyEffect effect, List<PolicyResolvedOutcome> outcomes) {
        return new PolicyDecision(
            effect,
            effect == PolicyEffect.ALLOW ? List.of("test.allow") : List.of(),
            effect == PolicyEffect.DENY ? List.of("test.deny") : List.of(),
            outcomes,
            EVALUATED_AT,
            "1.0",
            "project-1.0",
            "test",
            "a".repeat(64)
        );
    }

    private static PolicyResolvedOutcome outcome(String key, PolicyValue value) {
        return new PolicyResolvedOutcome(key, value);
    }
}
