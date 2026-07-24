package com.umc.product.feedback.application.authorization;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.umc.product.authorization.application.port.in.policy.LoadPolicySubjectSnapshotUseCase;
import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.authorization.application.service.policy.CompiledPolicyRegistry;
import com.umc.product.authorization.application.service.policy.rollout.PolicyDecisionRolloutExecutor;
import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutModeResolver;
import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutObserver;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.feedback.domain.enums.UserFeedbackTargetType;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;

@Service
public class FeedbackPolicyAuthorizationService {

    private final PolicyDecisionRolloutExecutor<
        FeedbackAuthorizationContext,
        FeedbackAuthorizationDecision> resolveRollout;
    private final PolicyDecisionRolloutExecutor<
        FeedbackAuthorizationContext,
        FeedbackAuthorizationDecision> submitRollout;
    private final GetGisuUseCase getGisuUseCase;
    private final GetChallengerUseCase getChallengerUseCase;
    private final GetChallengerRoleUseCase getChallengerRoleUseCase;
    private final LoadPolicySubjectSnapshotUseCase loadPolicySubjectSnapshotUseCase;

    public FeedbackPolicyAuthorizationService(
        LegacyFeedbackAuthorizationAdapter legacyEvaluator,
        TargetFeedbackAuthorizationAdapter targetEvaluator,
        PolicyRolloutModeResolver modeResolver,
        PolicyRolloutObserver observer,
        CompiledPolicyRegistry registry,
        GetGisuUseCase getGisuUseCase,
        GetChallengerUseCase getChallengerUseCase,
        GetChallengerRoleUseCase getChallengerRoleUseCase,
        LoadPolicySubjectSnapshotUseCase loadPolicySubjectSnapshotUseCase
    ) {
        this.resolveRollout = rollout(
            FeedbackPolicyAction.TEMPLATE_RESOLVE,
            legacyEvaluator,
            targetEvaluator,
            modeResolver,
            observer,
            registry);
        this.submitRollout = rollout(
            FeedbackPolicyAction.RESPONSE_SUBMIT,
            legacyEvaluator,
            targetEvaluator,
            modeResolver,
            observer,
            registry);
        this.getGisuUseCase = getGisuUseCase;
        this.getChallengerUseCase = getChallengerUseCase;
        this.getChallengerRoleUseCase = getChallengerRoleUseCase;
        this.loadPolicySubjectSnapshotUseCase = loadPolicySubjectSnapshotUseCase;
    }

    public Optional<UserFeedbackTargetType> resolveTargetType(long memberId) {
        Optional<GisuInfo> activeGisu = getGisuUseCase.findActiveGisu();
        if (activeGisu.isEmpty()) {
            return Optional.empty();
        }
        FeedbackAuthorizationContext context = context(
            FeedbackPolicyAction.TEMPLATE_RESOLVE,
            memberId,
            activeGisu.get(),
            null);
        return resolveRollout.evaluate(context, context.evaluatedAt()).targetType();
    }

    public boolean canSubmit(long memberId, UserFeedbackTargetType targetType) {
        GisuInfo activeGisu = getGisuUseCase.findActiveGisu().orElse(null);
        FeedbackAuthorizationContext context = context(
            FeedbackPolicyAction.RESPONSE_SUBMIT,
            memberId,
            activeGisu,
            targetType);
        return submitRollout.evaluate(context, context.evaluatedAt()).allowed();
    }

    private FeedbackAuthorizationContext context(
        FeedbackPolicyAction action,
        long memberId,
        GisuInfo targetGisu,
        UserFeedbackTargetType resourceTargetType
    ) {
        var subject = loadPolicySubjectSnapshotUseCase.loadMemberPolicySubject(memberId);
        Long gisuId = targetGisu == null ? null : targetGisu.gisuId();
        return new FeedbackAuthorizationContext(
            action,
            memberId,
            gisuId,
            targetGisu == null ? null : targetGisu.generation(),
            Optional.ofNullable(resourceTargetType),
            gisuId != null && getChallengerRoleUseCase.isCentralMemberInGisu(memberId, gisuId),
            getChallengerUseCase.getAllByMemberId(memberId),
            subject,
            subject.evaluatedAt());
    }

    private PolicyDecisionRolloutExecutor<
        FeedbackAuthorizationContext,
        FeedbackAuthorizationDecision> rollout(
        FeedbackPolicyAction action,
        LegacyFeedbackAuthorizationAdapter legacyEvaluator,
        TargetFeedbackAuthorizationAdapter targetEvaluator,
        PolicyRolloutModeResolver modeResolver,
        PolicyRolloutObserver observer,
        CompiledPolicyRegistry registry
    ) {
        return new PolicyDecisionRolloutExecutor<>(
            legacyEvaluator,
            targetEvaluator,
            new FeedbackExpectedDifference(),
            modeResolver,
            observer,
            registry,
            FeedbackPolicyDomainSchema.BUNDLE_KEY,
            action.id(),
            decision -> decision.allowed()
                ? PolicyRolloutObserver.EvaluationEffect.ALLOW
                : PolicyRolloutObserver.EvaluationEffect.DENY,
            FeedbackAuthorizationDecision.deny());
    }
}
