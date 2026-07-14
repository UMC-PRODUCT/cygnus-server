package com.umc.product.project.application.authorization.rollout.legacy;

import java.util.List;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.project.application.authorization.ProjectPolicyPrincipal;
import com.umc.product.project.application.authorization.ProjectPolicyResourceContext;
import com.umc.product.project.application.authorization.ProjectPolicySubjectSnapshot;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationApplicationScope;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationComparisonRequest;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationDecision;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationEvaluationFailure;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationEvaluationFailureCode;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationEvaluationPoint;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationEvaluationResult;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationEvaluator;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationFormView;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationProjectScope;
import com.umc.product.project.domain.enums.ProjectStatus;

@Component
public final class LegacyProjectAuthorizationAdapter implements ProjectAuthorizationEvaluator {

    private static final String GRAPHQL_FORM_SURFACE = "graphql:Project.applicationForm";

    private final LegacyProjectAuthorizationRules resourceRules = new LegacyProjectAuthorizationRules();
    private final LegacyProjectAuthorizationScopeRules scopeRules = new LegacyProjectAuthorizationScopeRules();
    private final LegacyProjectOperationalAuthorizationRules operationalRules =
        new LegacyProjectOperationalAuthorizationRules();

    @Override
    public ProjectAuthorizationEvaluationResult evaluate(ProjectAuthorizationComparisonRequest request) {
        try {
            return evaluateAction(request);
        } catch (RuntimeException exception) {
            return new ProjectAuthorizationEvaluationFailure(
                ProjectAuthorizationEvaluationFailureCode.LEGACY_ADAPTER_FAILED,
                request.evaluatedAt()
            );
        }
    }

    private ProjectAuthorizationEvaluationResult evaluateAction(
        ProjectAuthorizationComparisonRequest request
    ) {
        ProjectPolicySubjectSnapshot subject = request.snapshot();
        ProjectPolicyResourceContext resource = request.resource();
        return switch (request.action()) {
            case PROJECT_CREATE -> projectCreate(request);
            case PROJECT_UPDATE,
                PROJECT_SUBMIT,
                PROJECT_TRANSFER_OWNERSHIP,
                PROJECT_MEMBER_ADD,
                PROJECT_MEMBER_REMOVE,
                PROJECT_MEMBER_STATUS_UPDATE,
                FORM_UPDATE -> decision(resourceRules.canEditProject(subject, resource));
            case PROJECT_PUBLISH,
                PROJECT_QUOTA_UPDATE,
                PROJECT_ABORT -> decision(resourceRules.canManageProject(subject, resource));
            case PROJECT_DELETE -> decision(resourceRules.canDeleteProject(subject, resource));
            case PROJECT_READ,
                PROJECT_MEMBER_LIST -> decision(resourceRules.canReadProject(subject, resource));
            case PROJECT_MEMBER_BATCH -> resource.projectStatus().isPresent()
                ? decision(resourceRules.canReadProject(subject, resource))
                : decision(isMember(subject));
            case PROJECT_LIST_PUBLIC -> projectScope(scopeRules.publicProjects(subject, resource));
            case PROJECT_LIST_MANAGED -> projectScope(scopeRules.managedProjects(subject, resource));
            case PROJECT_LIST_OWN_DRAFTS -> projectScope(scopeRules.ownDrafts(subject));
            case APPLICATION_CREATE -> decision(resourceRules.canCreateApplication(subject, resource));
            case APPLICATION_UPDATE,
                APPLICATION_SUBMIT -> decision(resourceRules.canEditApplication(subject, resource));
            case APPLICATION_CANCEL -> decision(resourceRules.canCancelApplication(subject, resource));
            case APPLICATION_DECIDE -> applicationDecision(subject, resource);
            case APPLICATION_READ -> decision(resourceRules.canReadApplication(subject, resource));
            case APPLICATION_LIST_SELF -> applicationScope(scopeRules.ownApplications(subject));
            case APPLICATION_LIST_PROJECT,
                APPLICATION_LIST_PROJECT_BATCH ->
                applicationScope(scopeRules.projectApplications(subject, resource));
            case APPLICATION_LIST_MANAGEMENT ->
                applicationScope(scopeRules.managedApplications(subject, resource));
            case FORM_READ -> formRead(request);
            case MATCHING_LIST -> decision(isMember(subject));
            case MATCHING_CREATE,
                MATCHING_UPDATE,
                MATCHING_DELETE,
                MATCHING_HUMAN_AUTO_DECIDE ->
                decision(operationalRules.canManageMatchingRound(subject, resource));
            case MATCHING_SYSTEM_AUTO_DECIDE -> decision(operationalRules.canRunScheduler(subject));
            case STATISTICS_PROJECT ->
                decision(operationalRules.canReadProjectStatistics(subject, request.resourceSnapshot()));
            case STATISTICS_CHAPTER ->
                decision(operationalRules.canReadChapterStatistics(subject, request.resourceSnapshot()));
            case STATISTICS_PUBLIC_MATCHING -> decision(true);
            case CAPABILITY_LIST -> decision(isMember(subject));
        };
    }

    private ProjectAuthorizationEvaluationResult projectCreate(
        ProjectAuthorizationComparisonRequest request
    ) {
        ProjectPolicySubjectSnapshot subject = request.snapshot();
        if (!resourceRules.canEnterProjectCreate(subject)) {
            return ProjectAuthorizationDecision.denied();
        }
        Long targetMemberId = request.resource().creatorMemberId()
            .or(() -> request.resource().productOwnerMemberId())
            .orElse(null);
        if (targetMemberId == null || targetMemberId.equals(resourceRules.memberId(subject))) {
            return ProjectAuthorizationDecision.allowed();
        }
        if (request.resourceSnapshot().targetMemberSchoolId().isEmpty()) {
            return new ProjectAuthorizationEvaluationFailure(
                ProjectAuthorizationEvaluationFailureCode.LEGACY_ADAPTER_FAILED,
                request.evaluatedAt()
            );
        }
        boolean allowed = resourceRules.canAssignProjectOwner(
            subject,
            request.resource(),
            request.resourceSnapshot().targetMemberSchoolId().orElseThrow()
        );
        return decision(allowed);
    }

    private ProjectAuthorizationDecision applicationDecision(
        ProjectPolicySubjectSnapshot subject,
        ProjectPolicyResourceContext resource
    ) {
        boolean allowed = resourceRules.canDecideApplication(subject, resource);
        Long memberId = resourceRules.memberId(subject);
        boolean capabilityAuthorized = memberId != null
            && resource.projectStatus().filter(status -> status == ProjectStatus.IN_PROGRESS).isPresent()
            && resource.productOwnerMemberId().filter(memberId::equals).isPresent();
        return new ProjectAuthorizationDecision(
            effect(allowed),
            ProjectAuthorizationProjectScope.none(),
            ProjectAuthorizationApplicationScope.none(),
            ProjectAuthorizationFormView.NONE,
            allowed && resourceRules.isSuperAdmin(subject),
            capabilityAuthorized,
            List.of()
        );
    }

    private ProjectAuthorizationDecision formRead(ProjectAuthorizationComparisonRequest request) {
        boolean projectReadable = resourceRules.canReadProject(request.snapshot(), request.resource());
        boolean full = projectReadable && resourceRules.canViewFullForm(
            request.snapshot(), request.resource(), isGraphQlBatch(request.evaluationPoint()));
        boolean applicant = projectReadable
            && !full
            && resourceRules.canViewApplicantForm(request.snapshot(), request.resource());
        ProjectAuthorizationFormView view = full
            ? ProjectAuthorizationFormView.FULL
            : applicant ? ProjectAuthorizationFormView.APPLICANT : ProjectAuthorizationFormView.NONE;
        return new ProjectAuthorizationDecision(
            effect(full || applicant),
            ProjectAuthorizationProjectScope.none(),
            ProjectAuthorizationApplicationScope.none(),
            view,
            false,
            full || applicant,
            List.of()
        );
    }

    private ProjectAuthorizationDecision projectScope(ProjectAuthorizationProjectScope scope) {
        boolean allowed = scope.all()
            || scope.publicOnly()
            || !scope.gisuIds().isEmpty()
            || !scope.chapterIds().isEmpty()
            || !scope.ownerMemberIds().isEmpty();
        return new ProjectAuthorizationDecision(
            effect(allowed), scope, ProjectAuthorizationApplicationScope.none(),
            ProjectAuthorizationFormView.NONE, false, allowed, List.of());
    }

    private ProjectAuthorizationDecision applicationScope(ProjectAuthorizationApplicationScope scope) {
        boolean allowed = scope.all()
            || !scope.gisuIds().isEmpty()
            || !scope.chapterIds().isEmpty()
            || !scope.projectIds().isEmpty()
            || !scope.ownerMemberIds().isEmpty();
        return new ProjectAuthorizationDecision(
            effect(allowed), ProjectAuthorizationProjectScope.none(), scope,
            ProjectAuthorizationFormView.NONE, false, allowed, List.of());
    }

    private ProjectAuthorizationDecision decision(boolean allowed) {
        return allowed ? ProjectAuthorizationDecision.allowed() : ProjectAuthorizationDecision.denied();
    }

    private PolicyEffect effect(boolean allowed) {
        return allowed ? PolicyEffect.ALLOW : PolicyEffect.DENY;
    }

    private boolean isMember(ProjectPolicySubjectSnapshot subject) {
        return subject.principal() instanceof ProjectPolicyPrincipal.Member;
    }

    private boolean isGraphQlBatch(ProjectAuthorizationEvaluationPoint point) {
        return point instanceof ProjectAuthorizationEvaluationPoint.Surface surface
            && surface.identity().id().equals(GRAPHQL_FORM_SURFACE);
    }
}
