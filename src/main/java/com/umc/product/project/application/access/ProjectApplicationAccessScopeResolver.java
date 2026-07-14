package com.umc.product.project.application.access;

import static com.umc.product.project.application.authorization.ProjectPolicyDecisionOutcomes.booleanValue;
import static com.umc.product.project.application.authorization.ProjectPolicyDecisionOutcomes.longSetValue;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.policy.PolicyDecision;
import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.project.application.access.ProjectApplicationAccessScope.All;
import com.umc.product.project.application.access.ProjectApplicationAccessScope.AllInGisu;
import com.umc.product.project.application.access.ProjectApplicationAccessScope.ChapterScoped;
import com.umc.product.project.application.access.ProjectApplicationAccessScope.None;
import com.umc.product.project.application.access.ProjectApplicationAccessScope.OwnerOnly;
import com.umc.product.project.application.access.ProjectApplicationAccessScope.ProjectScoped;
import com.umc.product.project.application.authorization.ProjectPolicyAction;
import com.umc.product.project.application.authorization.ProjectPolicyAuthorizationService;
import com.umc.product.project.application.authorization.ProjectPolicyOutcomes;
import com.umc.product.project.application.authorization.ProjectPolicyResourceContext;
import com.umc.product.project.application.authorization.ProjectPolicySubjectSnapshot;
import com.umc.product.project.application.port.out.LoadProjectMemberPort;
import com.umc.product.project.domain.Project;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProjectApplicationAccessScopeResolver {

    private final ProjectPolicyAuthorizationService policyAuthorizationService;
    private final LoadProjectMemberPort loadProjectMemberPort;

    public ProjectApplicationAccessScope resolveForApplicant(Long memberId) {
        ProjectPolicySubjectSnapshot snapshot = policyAuthorizationService.snapshot(memberId);
        PolicyDecision decision = policyAuthorizationService.evaluate(
            snapshot,
            ProjectPolicyAction.APPLICATION_LIST_SELF,
            ProjectPolicyResourceContext.builder().build()
        );
        Set<Long> ownerMemberIds = longSetValue(decision, ProjectPolicyOutcomes.APPLICATION_OWNER_MEMBER_IDS);
        return decision.effect() == PolicyEffect.ALLOW && ownerMemberIds.contains(memberId)
            ? new OwnerOnly(memberId)
            : new None();
    }

    public ProjectApplicationAccessScope resolveForProjectApplicantList(Long memberId, Project project) {
        ProjectPolicySubjectSnapshot snapshot = policyAuthorizationService.snapshot(memberId);
        boolean activePlanMember = loadProjectMemberPort.isActivePlanMember(project.getId(), memberId);
        return resolveProject(snapshot, project, activePlanMember, ProjectPolicyAction.APPLICATION_LIST_PROJECT);
    }

    public ProjectApplicationAccessScope resolveForProjectApplicantList(
        SubjectAttributes subject,
        Project project
    ) {
        ProjectPolicySubjectSnapshot snapshot = policyAuthorizationService.snapshot(subject);
        boolean activePlanMember = loadProjectMemberPort.isActivePlanMember(project.getId(), subject.memberId());
        return resolveProject(snapshot, project, activePlanMember, ProjectPolicyAction.APPLICATION_LIST_PROJECT);
    }

    public Map<Long, ProjectApplicationAccessScope> resolveForProjectApplicantLists(
        Long memberId,
        Collection<Project> projects
    ) {
        if (projects == null || projects.isEmpty()) {
            return Map.of();
        }
        return resolveForProjectApplicantLists(
            memberId,
            policyAuthorizationService.snapshot(memberId),
            projects
        );
    }

    public Map<Long, ProjectApplicationAccessScope> resolveForProjectApplicantLists(
        SubjectAttributes subject,
        Collection<Project> projects
    ) {
        if (projects == null || projects.isEmpty()) {
            return Map.of();
        }
        return resolveForProjectApplicantLists(
            subject.memberId(),
            policyAuthorizationService.snapshot(subject),
            projects
        );
    }

    private Map<Long, ProjectApplicationAccessScope> resolveForProjectApplicantLists(
        Long memberId,
        ProjectPolicySubjectSnapshot snapshot,
        Collection<Project> projects
    ) {
        Set<Long> projectIds = projects.stream().map(Project::getId).collect(java.util.stream.Collectors.toSet());
        Set<Long> activePlanProjectIds = Set.copyOf(
            loadProjectMemberPort.listProjectIdsByActivePlanMember(projectIds, memberId));

        Map<Long, ProjectApplicationAccessScope> result = new LinkedHashMap<>();
        for (Project project : projects) {
            result.put(project.getId(), resolveProject(
                snapshot,
                project,
                activePlanProjectIds.contains(project.getId()),
                ProjectPolicyAction.APPLICATION_LIST_PROJECT_BATCH
            ));
        }
        return Map.copyOf(result);
    }

    public ProjectApplicationAccessScope resolveForManagement(Long memberId, Long gisuId) {
        ProjectPolicySubjectSnapshot snapshot = policyAuthorizationService.snapshot(memberId);
        PolicyDecision decision = policyAuthorizationService.evaluate(
            snapshot,
            ProjectPolicyAction.APPLICATION_LIST_MANAGEMENT,
            ProjectPolicyResourceContext.builder().gisuScope(gisuId).build()
        );
        if (decision.effect() != PolicyEffect.ALLOW) {
            return new None();
        }
        if (booleanValue(decision, ProjectPolicyOutcomes.APPLICATION_ALL)) {
            return new All();
        }
        if (longSetValue(decision, ProjectPolicyOutcomes.APPLICATION_GISU_IDS).contains(gisuId)) {
            return new AllInGisu(gisuId);
        }
        List<Long> chapterIds = longSetValue(decision, ProjectPolicyOutcomes.APPLICATION_CHAPTER_IDS).stream()
            .sorted()
            .toList();
        return chapterIds.isEmpty() ? new None() : new ChapterScoped(chapterIds, gisuId);
    }

    private ProjectApplicationAccessScope resolveProject(
        ProjectPolicySubjectSnapshot snapshot,
        Project project,
        boolean activePlanMember,
        ProjectPolicyAction action
    ) {
        PolicyDecision decision = policyAuthorizationService.evaluate(
            snapshot,
            action,
            ProjectPolicyResourceContext.builder()
                .project(project.getId(), project.getGisuId(), project.getChapterId(), project.getStatus())
                .productOwnerMemberId(project.getProductOwnerMemberId())
                .activePlanMember(activePlanMember)
                .build()
        );
        Set<Long> projectIds = longSetValue(decision, ProjectPolicyOutcomes.APPLICATION_PROJECT_IDS);
        if (decision.effect() != PolicyEffect.ALLOW || !projectIds.contains(project.getId())) {
            return new None();
        }
        return new ProjectScoped(
            project.getId(),
            booleanValue(decision, ProjectPolicyOutcomes.APPLICATION_INCLUDE_ONGOING_ROUNDS)
        );
    }
}
