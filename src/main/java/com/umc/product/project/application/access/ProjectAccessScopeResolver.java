package com.umc.product.project.application.access;

import static com.umc.product.project.application.authorization.ProjectPolicyDecisionOutcomes.booleanValue;
import static com.umc.product.project.application.authorization.ProjectPolicyDecisionOutcomes.longSetValue;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.policy.PolicyDecision;
import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.project.application.access.ProjectAccessScope.Clauses;
import com.umc.product.project.application.access.ProjectAccessScope.None;
import com.umc.product.project.application.authorization.ProjectPolicyAction;
import com.umc.product.project.application.authorization.ProjectPolicyAuthorizationService;
import com.umc.product.project.application.authorization.ProjectPolicyOutcomes;
import com.umc.product.project.application.authorization.ProjectPolicyResourceContext;
import com.umc.product.project.application.authorization.ProjectPolicySubjectSnapshot;
import com.umc.product.project.application.port.out.LoadProjectPort;
import com.umc.product.project.domain.enums.ProjectStatus;
import com.umc.product.project.domain.exception.ProjectDomainException;
import com.umc.product.project.domain.exception.ProjectErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProjectAccessScopeResolver {

    private static final Set<ProjectStatus> PUBLIC_STATUSES = Set.of(
        ProjectStatus.IN_PROGRESS,
        ProjectStatus.COMPLETED
    );

    private final ProjectPolicyAuthorizationService policyAuthorizationService;
    private final LoadProjectPort loadProjectPort;

    public ProjectAccessScope resolveForPublicSearch(
        Long memberId,
        Long gisuId,
        Set<ProjectStatus> requestedStatuses
    ) {
        rejectDraft(requestedStatuses);
        return resolveForPublicSearch(
            policyAuthorizationService.snapshot(memberId),
            gisuId,
            requestedStatuses
        );
    }

    public ProjectAccessScope resolveForPublicSearch(
        SubjectAttributes subject,
        Long gisuId,
        Set<ProjectStatus> requestedStatuses
    ) {
        rejectDraft(requestedStatuses);
        return resolveForPublicSearch(
            policyAuthorizationService.snapshot(subject),
            gisuId,
            requestedStatuses
        );
    }

    private ProjectAccessScope resolveForPublicSearch(
        ProjectPolicySubjectSnapshot snapshot,
        Long gisuId,
        Set<ProjectStatus> requestedStatuses
    ) {
        PolicyDecision decision = policyAuthorizationService.evaluate(
            snapshot,
            ProjectPolicyAction.PROJECT_LIST_PUBLIC,
            ProjectPolicyResourceContext.builder().gisuScope(gisuId).build()
        );
        if (decision.effect() != PolicyEffect.ALLOW) {
            throw denied();
        }

        List<ScopeClause> clauses = new ArrayList<>();
        if (booleanValue(decision, ProjectPolicyOutcomes.PROJECT_PUBLIC_ONLY)) {
            clauses.add(ScopeClause.gisu(Set.of(gisuId), PUBLIC_STATUSES));
        }
        addAllClause(decision, gisuId, requestedStatuses, clauses);
        addGisuClauses(decision, requestedStatuses, clauses);
        addChapterClauses(decision, gisuId, requestedStatuses, clauses);

        boolean privileged = clauses.size() > 1
            || booleanValue(decision, ProjectPolicyOutcomes.PROJECT_ALL);
        if (!privileged && !PUBLIC_STATUSES.containsAll(requestedStatuses)) {
            throw denied();
        }
        return clauses.isEmpty() ? new None() : new Clauses(clauses);
    }

    public ProjectAccessScope resolveForManagement(
        Long memberId,
        Long gisuId,
        Set<ProjectStatus> requestedStatuses
    ) {
        ProjectPolicySubjectSnapshot snapshot = policyAuthorizationService.snapshot(memberId);
        boolean hasOwnedProject = loadProjectPort.existsByOwnerAndGisu(memberId, gisuId);
        PolicyDecision decision = policyAuthorizationService.evaluate(
            snapshot,
            ProjectPolicyAction.PROJECT_LIST_MANAGED,
            ProjectPolicyResourceContext.builder()
                .gisuScope(gisuId)
                .requesterHasOwnedProjectInResourceGisu(hasOwnedProject)
                .build()
        );
        if (decision.effect() != PolicyEffect.ALLOW) {
            return new None();
        }

        List<ScopeClause> clauses = new ArrayList<>();
        addAllClause(decision, gisuId, requestedStatuses, clauses);
        addGisuClauses(decision, requestedStatuses, clauses);
        addChapterClauses(decision, gisuId, requestedStatuses, clauses);
        addOwnerClause(decision, gisuId, requestedStatuses, clauses);
        return clauses.isEmpty() ? new None() : new Clauses(clauses);
    }

    public ProjectAccessScope resolveForOwnDraft(Long memberId, Long gisuId) {
        ProjectPolicySubjectSnapshot snapshot = policyAuthorizationService.snapshot(memberId);
        PolicyDecision decision = policyAuthorizationService.evaluate(
            snapshot,
            ProjectPolicyAction.PROJECT_LIST_OWN_DRAFTS,
            ProjectPolicyResourceContext.builder().gisuScope(gisuId).build()
        );
        Set<Long> ownerMemberIds = longSetValue(decision, ProjectPolicyOutcomes.PROJECT_OWNER_MEMBER_IDS);
        boolean includeOwnDrafts = booleanValue(decision, ProjectPolicyOutcomes.PROJECT_INCLUDE_OWN_DRAFTS);
        return decision.effect() == PolicyEffect.ALLOW
            && includeOwnDrafts
            && ownerMemberIds.contains(memberId)
                ? new ProjectAccessScope.OwnerOnly(memberId, Set.of(ProjectStatus.DRAFT))
                : new None();
    }

    private void addAllClause(
        PolicyDecision decision,
        Long requestedGisuId,
        Set<ProjectStatus> statuses,
        List<ScopeClause> clauses
    ) {
        if (booleanValue(decision, ProjectPolicyOutcomes.PROJECT_ALL)) {
            clauses.add(ScopeClause.gisu(Set.of(requestedGisuId), statuses));
        }
    }

    private void addGisuClauses(
        PolicyDecision decision,
        Set<ProjectStatus> statuses,
        List<ScopeClause> clauses
    ) {
        Set<Long> gisuIds = longSetValue(decision, ProjectPolicyOutcomes.PROJECT_GISU_IDS);
        if (!gisuIds.isEmpty()) {
            clauses.add(ScopeClause.gisu(gisuIds, statuses));
        }
    }

    private void addChapterClauses(
        PolicyDecision decision,
        Long requestedGisuId,
        Set<ProjectStatus> statuses,
        List<ScopeClause> clauses
    ) {
        Set<Long> chapterIds = longSetValue(decision, ProjectPolicyOutcomes.PROJECT_CHAPTER_IDS);
        if (!chapterIds.isEmpty()) {
            clauses.add(ScopeClause.gisu(Set.of(requestedGisuId), statuses).andChapterIds(chapterIds));
        }
    }

    private void addOwnerClause(
        PolicyDecision decision,
        Long requestedGisuId,
        Set<ProjectStatus> requestedStatuses,
        List<ScopeClause> clauses
    ) {
        Set<Long> ownerMemberIds = longSetValue(decision, ProjectPolicyOutcomes.PROJECT_OWNER_MEMBER_IDS);
        if (ownerMemberIds.isEmpty()) {
            return;
        }
        Set<ProjectStatus> ownerStatuses = EnumSet.copyOf(requestedStatuses);
        if (booleanValue(decision, ProjectPolicyOutcomes.PROJECT_INCLUDE_OWN_DRAFTS)) {
            ownerStatuses.add(ProjectStatus.DRAFT);
        }
        clauses.add(ScopeClause.gisu(Set.of(requestedGisuId), ownerStatuses)
            .andOwnerMemberIds(ownerMemberIds));
    }

    private ProjectDomainException denied() {
        return new ProjectDomainException(ProjectErrorCode.PROJECT_ACCESS_DENIED);
    }

    private void rejectDraft(Set<ProjectStatus> requestedStatuses) {
        if (requestedStatuses.contains(ProjectStatus.DRAFT)) {
            throw denied();
        }
    }
}
