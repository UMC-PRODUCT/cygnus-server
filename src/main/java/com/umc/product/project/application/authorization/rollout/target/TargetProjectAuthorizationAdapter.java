package com.umc.product.project.application.authorization.rollout.target;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.in.policy.EvaluatePolicyUseCase;
import com.umc.product.authorization.application.port.in.policy.PolicyEvaluationRequest;
import com.umc.product.authorization.domain.policy.PolicyDecision;
import com.umc.product.authorization.domain.policy.PolicyEvaluationFailure;
import com.umc.product.authorization.domain.policy.PolicyEvaluationResult;
import com.umc.product.project.application.authorization.ProjectCompiledPolicyBundle;
import com.umc.product.project.application.authorization.ProjectPolicyAction;
import com.umc.product.project.application.authorization.ProjectPolicyBundleLoader;
import com.umc.product.project.application.authorization.ProjectPolicyContextBuilder;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationComparisonRequest;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationDecision;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationEvaluationFailure;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationEvaluationFailureCode;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationEvaluationResult;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationEvaluator;

@Component
public final class TargetProjectAuthorizationAdapter implements ProjectAuthorizationEvaluator {

    private final EvaluatePolicyUseCase evaluatePolicyUseCase;
    private final ProjectCompiledPolicyBundle bundle;
    private final ProjectPolicyContextBuilder contextBuilder = new ProjectPolicyContextBuilder();

    public TargetProjectAuthorizationAdapter(
        EvaluatePolicyUseCase evaluatePolicyUseCase,
        ProjectPolicyBundleLoader bundleLoader
    ) {
        this.evaluatePolicyUseCase = evaluatePolicyUseCase;
        this.bundle = bundleLoader.compiled();
    }

    @Override
    public ProjectAuthorizationEvaluationResult evaluate(ProjectAuthorizationComparisonRequest request) {
        if (deniesApplicationRoundScopeMismatch(request)) {
            return ProjectAuthorizationDecision.denied();
        }
        if (requiresTargetMemberSchool(request)) {
            return failure(request);
        }
        try {
            PolicyEvaluationResult result = evaluatePolicyUseCase.evaluate(new PolicyEvaluationRequest(
                bundle.value(),
                request.action().id(),
                contextBuilder.build(request.action(), request.snapshot(), request.resourceSnapshot()),
                request.evaluatedAt()
            ));
            if (result instanceof PolicyEvaluationFailure) {
                return failure(request);
            }
            return ProjectAuthorizationDecision.fromPolicyDecision(
                request.action(),
                (PolicyDecision) result
            );
        } catch (RuntimeException exception) {
            return failure(request);
        }
    }

    private boolean requiresTargetMemberSchool(ProjectAuthorizationComparisonRequest request) {
        if (request.action() != ProjectPolicyAction.PROJECT_CREATE
            || request.resourceSnapshot().targetMemberSchoolId().isPresent()) {
            return false;
        }
        return request.snapshot().principal() instanceof
            com.umc.product.project.application.authorization.ProjectPolicyPrincipal.Member member
            && request.resource().creatorMemberId().filter(id -> id != member.memberId()).isPresent();
    }

    private boolean deniesApplicationRoundScopeMismatch(
        ProjectAuthorizationComparisonRequest request
    ) {
        if (!request.resourceSnapshot().applicationRoundScopeMismatch()) {
            return false;
        }
        return request.action() == ProjectPolicyAction.APPLICATION_CREATE
            || request.action() == ProjectPolicyAction.APPLICATION_SUBMIT;
    }

    private ProjectAuthorizationEvaluationFailure failure(
        ProjectAuthorizationComparisonRequest request
    ) {
        return new ProjectAuthorizationEvaluationFailure(
            ProjectAuthorizationEvaluationFailureCode.POLICY_EVALUATION_FAILED,
            request.evaluatedAt()
        );
    }
}
