package com.umc.product.challenger.application.authorization;

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

@Service
public class ChallengerPolicyAuthorizationService {

    private final Map<ChallengerPolicyAction, BooleanPolicyRolloutExecutor<ChallengerAuthorizationContext>>
        rollouts;
    private final Clock clock;

    public ChallengerPolicyAuthorizationService(
        LegacyChallengerAuthorizationAdapter legacyEvaluator,
        TargetChallengerAuthorizationAdapter targetEvaluator,
        PolicyRolloutModeResolver modeResolver,
        PolicyRolloutObserver observer,
        CompiledPolicyRegistry registry,
        Clock clock
    ) {
        EnumMap<ChallengerPolicyAction, BooleanPolicyRolloutExecutor<ChallengerAuthorizationContext>>
            configured = new EnumMap<>(ChallengerPolicyAction.class);
        for (ChallengerPolicyAction action : ChallengerPolicyAction.values()) {
            configured.put(action, new BooleanPolicyRolloutExecutor<>(
                legacyEvaluator,
                targetEvaluator,
                new ChallengerExpectedDifference(),
                modeResolver,
                observer,
                registry,
                ChallengerPolicyDomainSchema.BUNDLE_KEY,
                action.id()));
        }
        this.rollouts = Map.copyOf(configured);
        this.clock = clock;
    }

    public boolean evaluate(
        ChallengerPolicyAction action,
        SubjectAttributes legacySubject,
        Long targetGisuId,
        Long targetSchoolId
    ) {
        Optional<AuthorizationSubjectSnapshot> subject = policySubject(legacySubject);
        Instant evaluatedAt = subject
            .map(AuthorizationSubjectSnapshot::evaluatedAt)
            .orElseGet(clock::instant);
        return rollouts.get(action).evaluate(
            new ChallengerAuthorizationContext(
                action,
                legacySubject,
                subject,
                targetGisuId,
                targetSchoolId,
                evaluatedAt),
            evaluatedAt);
    }

    public boolean evaluate(
        ChallengerPolicyAction action,
        SubjectAttributes legacySubject
    ) {
        return evaluate(action, legacySubject, null, null);
    }

    private Optional<AuthorizationSubjectSnapshot> policySubject(SubjectAttributes subject) {
        try {
            return Optional.of(subject.toAuthorizationSubjectSnapshot());
        } catch (IllegalStateException exception) {
            return Optional.empty();
        }
    }
}
