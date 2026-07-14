package com.umc.product.project.application.authorization;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterScopeInfo;
import com.umc.product.project.application.access.ProjectAccessScopeResolver;
import com.umc.product.project.application.access.ProjectApplicationAccessScopeResolver;
import com.umc.product.project.application.authorization.ProjectDirectActionBindings.Binding;
import com.umc.product.project.application.authorization.ProjectDirectActionBindings.Caller;
import com.umc.product.project.application.port.out.LoadProjectMemberPort;
import com.umc.product.project.application.port.out.LoadProjectPort;
import com.umc.product.project.application.service.policy.ProjectStatisticsAccessPolicy;
import com.umc.product.project.domain.Project;
import com.umc.product.project.domain.enums.ProjectStatus;

class ProjectDirectScopeActionProductionPathContractTest
    extends ProjectDirectActionProductionPathContractSupport {

    private final LoadProjectPort loadProjectPort = mock(LoadProjectPort.class);
    private final LoadProjectMemberPort loadProjectMemberPort = mock(LoadProjectMemberPort.class);

    private final ProjectAccessScopeResolver projectScopeResolver =
        new ProjectAccessScopeResolver(policyService, loadProjectPort);
    private final ProjectApplicationAccessScopeResolver applicationScopeResolver =
        new ProjectApplicationAccessScopeResolver(policyService, loadProjectMemberPort);
    private final ProjectStatisticsAccessPolicy statisticsAccessPolicy =
        new ProjectStatisticsAccessPolicy(policyService);

    private Project project;

    @BeforeEach
    void setUpProject() {
        project = mock(Project.class);
        given(project.getId()).willReturn(PROJECT_ID);
        given(project.getGisuId()).willReturn(GISU_ID);
        given(project.getChapterId()).willReturn(CHAPTER_ID);
        given(project.getStatus()).willReturn(ProjectStatus.DRAFT);
        given(project.getProductOwnerMemberId()).willReturn(MEMBER_ID);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("scopeBindings")
    @DisplayName("scope와 statistics의 direct action은 실제 resolver에서 exact policy action으로 평가된다")
    void direct_scope_action이_production_path에서_exact_action으로_평가된다(Binding binding) {
        assertExactPolicyAction(binding.action(), () -> invoke(binding.action()));
    }

    private void invoke(ProjectPolicyAction action) {
        switch (action) {
            case PROJECT_LIST_PUBLIC -> projectScopeResolver.resolveForPublicSearch(
                MEMBER_ID, GISU_ID, Set.of(ProjectStatus.IN_PROGRESS));
            case PROJECT_LIST_MANAGED -> projectScopeResolver.resolveForManagement(
                MEMBER_ID, GISU_ID, Set.of(ProjectStatus.IN_PROGRESS));
            case PROJECT_LIST_OWN_DRAFTS -> projectScopeResolver.resolveForOwnDraft(MEMBER_ID, GISU_ID);
            case APPLICATION_LIST_SELF -> applicationScopeResolver.resolveForApplicant(MEMBER_ID);
            case APPLICATION_LIST_PROJECT -> applicationScopeResolver.resolveForProjectApplicantList(
                MEMBER_ID, project);
            case APPLICATION_LIST_PROJECT_BATCH -> applicationScopeResolver.resolveForProjectApplicantLists(
                MEMBER_ID, List.of(project));
            case APPLICATION_LIST_MANAGEMENT -> applicationScopeResolver.resolveForManagement(MEMBER_ID, GISU_ID);
            case STATISTICS_PROJECT -> statisticsAccessPolicy.canReadProjectStatistics(snapshot, project, false);
            case STATISTICS_CHAPTER -> statisticsAccessPolicy.canReadChapterStatistics(
                snapshot, new ChapterScopeInfo(CHAPTER_ID, GISU_ID));
            case STATISTICS_PUBLIC_MATCHING -> statisticsAccessPolicy.canReadPublicMatchingStatistics(
                snapshot, new ChapterScopeInfo(CHAPTER_ID, GISU_ID));
            default -> throw new IllegalArgumentException("scope direct action이 아닙니다: " + action);
        }
    }

    private static Stream<Binding> scopeBindings() {
        return ProjectDirectActionBindings.valuesFor(
            Caller.PROJECT_SCOPE,
            Caller.APPLICATION_SCOPE,
            Caller.STATISTICS
        ).stream();
    }
}
