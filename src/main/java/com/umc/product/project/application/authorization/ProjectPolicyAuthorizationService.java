package com.umc.product.project.application.authorization;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.umc.product.authorization.application.port.in.policy.EvaluatePolicyUseCase;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.authorization.domain.exception.AuthorizationErrorCode;
import com.umc.product.authorization.domain.policy.PolicyDecision;
import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.authorization.domain.policy.PolicyValue;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationAuxiliaryFailureCode;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationAuxiliaryFailureReporter;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationClassifier;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationComparisonRequest;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationCoordinationResult;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationDecision;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationEvaluationFailure;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationEvaluationFailureCode;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationEvaluationPoint;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationEvaluationResult;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationInternalOrigin;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationResourceSnapshot;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationRolloutCoordinator;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationRolloutMode;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationRolloutObserver;
import com.umc.product.project.application.authorization.rollout.target.TargetProjectAuthorizationAdapter;

@Service
public class ProjectPolicyAuthorizationService {

    private final ProjectPolicySubjectSnapshotLoader snapshotLoader;
    private final ProjectAuthorizationRolloutCoordinator coordinator;
    private final ProjectCompiledPolicyBundle bundle;
    private final Optional<ProjectAuthorizationResourceSnapshotFactory> resourceSnapshotFactory;
    private final ProjectAuthorizationRolloutObserver rolloutObserver;
    private final ProjectAuthorizationPolicyDecisionMapper decisionMapper =
        new ProjectAuthorizationPolicyDecisionMapper();

    @Autowired
    public ProjectPolicyAuthorizationService(
        ProjectPolicySubjectSnapshotLoader snapshotLoader,
        ProjectAuthorizationRolloutCoordinator coordinator,
        ProjectPolicyBundleLoader bundleLoader,
        ProjectAuthorizationResourceSnapshotFactory resourceSnapshotFactory,
        ProjectAuthorizationRolloutObserver rolloutObserver
    ) {
        this.snapshotLoader = snapshotLoader;
        this.coordinator = coordinator;
        this.bundle = bundleLoader.compiled();
        this.resourceSnapshotFactory = Optional.of(resourceSnapshotFactory);
        this.rolloutObserver = rolloutObserver;
    }

    public ProjectPolicyAuthorizationService(
        ProjectPolicySubjectSnapshotLoader snapshotLoader,
        ProjectAuthorizationRolloutCoordinator coordinator,
        ProjectPolicyBundleLoader bundleLoader,
        ProjectAuthorizationResourceSnapshotFactory resourceSnapshotFactory
    ) {
        this(snapshotLoader, coordinator, bundleLoader, resourceSnapshotFactory,
            ProjectAuthorizationRolloutObserver.noOp());
    }

    public ProjectPolicyAuthorizationService(
        ProjectPolicySubjectSnapshotLoader snapshotLoader,
        ProjectAuthorizationRolloutCoordinator coordinator,
        ProjectPolicyBundleLoader bundleLoader
    ) {
        this.snapshotLoader = snapshotLoader;
        this.coordinator = coordinator;
        this.bundle = bundleLoader.compiled();
        this.resourceSnapshotFactory = Optional.empty();
        this.rolloutObserver = ProjectAuthorizationRolloutObserver.noOp();
    }

    public ProjectPolicyAuthorizationService(
        ProjectPolicySubjectSnapshotLoader snapshotLoader,
        EvaluatePolicyUseCase evaluatePolicyUseCase,
        ProjectPolicyBundleLoader bundleLoader
    ) {
        this(snapshotLoader, compatibilityCoordinator(evaluatePolicyUseCase, bundleLoader), bundleLoader);
    }

    public ProjectPolicySubjectSnapshot snapshot(long memberId) {
        return snapshotLoader.load(memberId);
    }

    public ProjectPolicySubjectSnapshot snapshot(SubjectAttributes subject) {
        return snapshotLoader.load(subject);
    }

    public PolicyDecision evaluate(
        long memberId,
        ProjectPolicyAction action,
        ProjectPolicyResourceContext resource
    ) {
        return evaluate(snapshotForAction(memberId, action), action, resource);
    }

    public PolicyDecision evaluateTrustedResource(
        long memberId,
        ProjectPolicyAction action,
        ProjectAuthorizationResourceSnapshot resourceSnapshot
    ) {
        return evaluate(
            snapshotForAction(memberId, action),
            action,
            resourceSnapshot,
            ProjectAuthorizationEvaluationPoint.internal(
                ProjectAuthorizationInternalOrigin.RESOURCE_PERMISSION_EVALUATOR)
        );
    }

    public PolicyDecision evaluate(
        SubjectAttributes subject,
        ProjectPolicyAction action,
        ProjectPolicyResourceContext resource
    ) {
        return evaluate(snapshot(subject), action, resource);
    }

    public PolicyDecision evaluateSystem(
        String systemId,
        ProjectPolicyAction action,
        ProjectPolicyResourceContext resource
    ) {
        return evaluate(snapshotLoader.loadSystem(systemId), action, resource);
    }

    public PolicyDecision evaluate(
        ProjectPolicySubjectSnapshot subject,
        ProjectPolicyAction action,
        ProjectPolicyResourceContext resource
    ) {
        return evaluate(
            subject,
            action,
            resource,
            ProjectAuthorizationEvaluationPoint.internal(
                ProjectAuthorizationInternalOrigin.RESOURCE_PERMISSION_EVALUATOR),
            Optional.empty()
        );
    }

    public PolicyDecision evaluate(
        ProjectPolicySubjectSnapshot subject,
        ProjectPolicyAction action,
        ProjectPolicyResourceContext resource,
        ProjectAuthorizationEvaluationPoint evaluationPoint,
        Optional<Long> targetMemberSchoolId
    ) {
        ProjectAuthorizationResourceSnapshot resourceSnapshot = resourceSnapshotFactory
            .map(factory -> factory.create(subject, resource, targetMemberSchoolId))
            .orElseGet(() -> ProjectAuthorizationResourceSnapshot.of(resource));
        return evaluate(subject, action, resourceSnapshot, evaluationPoint);
    }

    public PolicyDecision evaluate(
        ProjectPolicySubjectSnapshot subject,
        ProjectPolicyAction action,
        ProjectAuthorizationResourceSnapshot resourceSnapshot,
        ProjectAuthorizationEvaluationPoint evaluationPoint
    ) {
        try {
            ProjectAuthorizationComparisonRequest request = new ProjectAuthorizationComparisonRequest(
                subject, action, resourceSnapshot, evaluationPoint);
            long startedAt = System.nanoTime();
            ProjectAuthorizationCoordinationResult coordination = coordinator.coordinate(request);
            long elapsedNanos = Math.max(0L, System.nanoTime() - startedAt);
            observe(request, coordination, elapsedNanos);
            var result = coordination.authoritative();
            if (result instanceof ProjectAuthorizationEvaluationFailure) {
                throw new AuthorizationDomainException(AuthorizationErrorCode.POLICY_EVALUATION_FAILED);
            }
            return decisionMapper.map(request, (ProjectAuthorizationDecision) result, bundle);
        } catch (AuthorizationDomainException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new AuthorizationDomainException(AuthorizationErrorCode.POLICY_EVALUATION_FAILED);
        }
    }

    private void observe(
        ProjectAuthorizationComparisonRequest request,
        ProjectAuthorizationCoordinationResult coordination,
        long elapsedNanos
    ) {
        try {
            rolloutObserver.observe(observation(request, coordination, elapsedNanos));
        } catch (RuntimeException exception) {
            ProjectAuthorizationAuxiliaryFailureReporter.warn(
                ProjectAuthorizationAuxiliaryFailureCode.ROLLOUT_OBSERVATION_FAILED,
                request.action()
            );
        }
    }

    private ProjectAuthorizationRolloutObserver.Observation observation(
        ProjectAuthorizationComparisonRequest request,
        ProjectAuthorizationCoordinationResult coordination,
        long elapsedNanos
    ) {
        var compiled = bundle.value();
        return new ProjectAuthorizationRolloutObserver.Observation(
            request.action(),
            coordination.mode(),
            coordination.classification(),
            request.evaluationPoint(),
            bothFailed(coordination),
            failureCode(coordination),
            observedEffect(coordination.legacy()),
            coordination.target()
                .map(ProjectPolicyAuthorizationService::observedEffect)
                .orElse(ProjectAuthorizationRolloutObserver.EvaluationEffect.NOT_EVALUATED),
            targetPrivilegeExpansion(coordination),
            request.evaluatedAt(),
            compiled.schemaVersion(),
            compiled.contextSchemaVersion(),
            compiled.policyVersion(),
            compiled.policyFingerprint(),
            elapsedNanos
        );
    }

    private static ProjectAuthorizationRolloutObserver.EvaluationEffect observedEffect(
        ProjectAuthorizationEvaluationResult result
    ) {
        if (result instanceof ProjectAuthorizationEvaluationFailure) {
            return ProjectAuthorizationRolloutObserver.EvaluationEffect.FAILURE;
        }
        return result.allows()
            ? ProjectAuthorizationRolloutObserver.EvaluationEffect.ALLOW
            : ProjectAuthorizationRolloutObserver.EvaluationEffect.DENY;
    }

    private static ProjectAuthorizationRolloutObserver.TargetPrivilegeExpansion targetPrivilegeExpansion(
        ProjectAuthorizationCoordinationResult coordination
    ) {
        if (coordination.target().isEmpty()) {
            return ProjectAuthorizationRolloutObserver.TargetPrivilegeExpansion.NOT_EVALUATED;
        }
        ProjectAuthorizationEvaluationResult target = coordination.target().orElseThrow();
        if (coordination.legacy() instanceof ProjectAuthorizationEvaluationFailure
            || target instanceof ProjectAuthorizationEvaluationFailure) {
            return ProjectAuthorizationRolloutObserver.TargetPrivilegeExpansion.INDETERMINATE;
        }
        return target.allows() && !coordination.legacy().allows()
            ? ProjectAuthorizationRolloutObserver.TargetPrivilegeExpansion.YES
            : ProjectAuthorizationRolloutObserver.TargetPrivilegeExpansion.NO;
    }

    private static boolean bothFailed(ProjectAuthorizationCoordinationResult coordination) {
        return coordination.legacy() instanceof ProjectAuthorizationEvaluationFailure
            && coordination.target()
                .filter(ProjectAuthorizationEvaluationFailure.class::isInstance)
                .isPresent();
    }

    private static Optional<ProjectAuthorizationEvaluationFailureCode> failureCode(
        ProjectAuthorizationCoordinationResult coordination
    ) {
        Optional<ProjectAuthorizationEvaluationFailureCode> targetFailure = coordination.target()
            .filter(ProjectAuthorizationEvaluationFailure.class::isInstance)
            .map(ProjectAuthorizationEvaluationFailure.class::cast)
            .map(ProjectAuthorizationEvaluationFailure::failureCode);
        if (targetFailure.isPresent()) {
            return targetFailure;
        }
        if (coordination.legacy() instanceof ProjectAuthorizationEvaluationFailure legacyFailure) {
            return Optional.of(legacyFailure.failureCode());
        }
        return Optional.empty();
    }

    public boolean allowsAny(
        SubjectAttributes subject,
        List<ProjectPolicyAction> actions,
        ProjectPolicyResourceContext resource
    ) {
        ProjectPolicySubjectSnapshot snapshot = snapshot(subject);
        return actions.stream().anyMatch(action -> allows(snapshot, action, resource));
    }

    public boolean allows(
        ProjectPolicySubjectSnapshot subject,
        ProjectPolicyAction action,
        ProjectPolicyResourceContext resource
    ) {
        return evaluate(subject, action, resource).effect() == PolicyEffect.ALLOW;
    }

    public boolean forceDecision(PolicyDecision decision) {
        return decision.outcome(ProjectPolicyOutcomes.APPLICATION_FORCE_DECISION)
            .filter(PolicyValue.BooleanValue.class::isInstance)
            .map(PolicyValue.BooleanValue.class::cast)
            .map(PolicyValue.BooleanValue::value)
            .orElse(false);
    }

    private ProjectPolicySubjectSnapshot snapshotForAction(long memberId, ProjectPolicyAction action) {
        return switch (action) {
            case MATCHING_LIST -> snapshotLoader.loadAuthenticatedMember(memberId);
            case MATCHING_CREATE, MATCHING_UPDATE, MATCHING_DELETE, MATCHING_HUMAN_AUTO_DECIDE ->
                snapshotLoader.loadMatchingManager(memberId);
            default -> snapshotLoader.load(memberId);
        };
    }

    private static ProjectAuthorizationRolloutCoordinator compatibilityCoordinator(
        EvaluatePolicyUseCase evaluatePolicyUseCase,
        ProjectPolicyBundleLoader bundleLoader
    ) {
        TargetProjectAuthorizationAdapter target = new TargetProjectAuthorizationAdapter(
            evaluatePolicyUseCase, bundleLoader);
        return new ProjectAuthorizationRolloutCoordinator(
            request -> ProjectAuthorizationDecision.denied(),
            target,
            new ProjectAuthorizationClassifier((request, legacy, current) -> java.util.Optional.empty()),
            action -> ProjectAuthorizationRolloutMode.ENFORCE
        );
    }
}
