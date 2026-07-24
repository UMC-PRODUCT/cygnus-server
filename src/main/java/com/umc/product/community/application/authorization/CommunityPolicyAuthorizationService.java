package com.umc.product.community.application.authorization;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.umc.product.authorization.application.service.policy.CompiledPolicyRegistry;
import com.umc.product.authorization.application.service.policy.rollout.BooleanPolicyRolloutExecutor;
import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutModeResolver;
import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutObserver;
import com.umc.product.authorization.domain.AuthorizationSubjectSnapshot;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.community.application.port.in.query.GetCommentListUseCase;
import com.umc.product.community.application.port.in.query.GetPostDetailUseCase;

@Service
public class CommunityPolicyAuthorizationService {

    private final Map<CommunityPolicyAction, BooleanPolicyRolloutExecutor<CommunityAuthorizationContext>>
        rollouts;
    private final GetPostDetailUseCase getPostDetailUseCase;
    private final GetCommentListUseCase getCommentListUseCase;
    private final GetChallengerUseCase getChallengerUseCase;
    private final Clock clock;

    public CommunityPolicyAuthorizationService(
        LegacyCommunityAuthorizationAdapter legacyEvaluator,
        TargetCommunityAuthorizationAdapter targetEvaluator,
        PolicyRolloutModeResolver modeResolver,
        PolicyRolloutObserver observer,
        CompiledPolicyRegistry registry,
        GetPostDetailUseCase getPostDetailUseCase,
        GetCommentListUseCase getCommentListUseCase,
        GetChallengerUseCase getChallengerUseCase,
        Clock clock
    ) {
        EnumMap<CommunityPolicyAction, BooleanPolicyRolloutExecutor<CommunityAuthorizationContext>>
            configured = new EnumMap<>(CommunityPolicyAction.class);
        for (CommunityPolicyAction action : CommunityPolicyAction.values()) {
            configured.put(action, new BooleanPolicyRolloutExecutor<>(
                legacyEvaluator,
                targetEvaluator,
                new CommunityExpectedDifference(),
                modeResolver,
                observer,
                registry,
                CommunityPolicyDomainSchema.BUNDLE_KEY,
                action.id()));
        }
        this.rollouts = Map.copyOf(configured);
        this.getPostDetailUseCase = getPostDetailUseCase;
        this.getCommentListUseCase = getCommentListUseCase;
        this.getChallengerUseCase = getChallengerUseCase;
        this.clock = clock;
    }

    public boolean evaluatePost(
        CommunityPolicyAction action,
        SubjectAttributes subject,
        Long postId
    ) {
        Long challengerId = getPostDetailUseCase.getPostDetail(postId).authorChallengerId();
        return evaluate(action, subject, challengerId);
    }

    public boolean evaluateComment(
        CommunityPolicyAction action,
        SubjectAttributes subject,
        Long commentId
    ) {
        Long challengerId = getCommentListUseCase.getComment(commentId).challengerId();
        return evaluate(action, subject, challengerId);
    }

    private boolean evaluate(
        CommunityPolicyAction action,
        SubjectAttributes subject,
        Long authorChallengerId
    ) {
        Long authorMemberId = getChallengerUseCase.getById(authorChallengerId).memberId();
        boolean authorHasChallengerHistory =
            !getChallengerUseCase.getAllByMemberId(authorMemberId).isEmpty();
        Optional<AuthorizationSubjectSnapshot> policySubject = policySubject(subject);
        Instant evaluatedAt = policySubject
            .map(AuthorizationSubjectSnapshot::evaluatedAt)
            .orElseGet(clock::instant);
        CommunityAuthorizationContext context = new CommunityAuthorizationContext(
            action,
            subject,
            policySubject,
            authorHasChallengerHistory,
            authorMemberId,
            evaluatedAt);
        return rollouts.get(action).evaluate(context, evaluatedAt);
    }

    private Optional<AuthorizationSubjectSnapshot> policySubject(SubjectAttributes subject) {
        try {
            return Optional.of(subject.toAuthorizationSubjectSnapshot());
        } catch (IllegalStateException exception) {
            return Optional.empty();
        }
    }
}
