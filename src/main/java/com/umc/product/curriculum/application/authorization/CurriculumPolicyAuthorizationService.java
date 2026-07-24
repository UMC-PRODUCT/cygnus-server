package com.umc.product.curriculum.application.authorization;

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
public class CurriculumPolicyAuthorizationService {

    private final Map<CurriculumPolicyAction, BooleanPolicyRolloutExecutor<CurriculumAuthorizationContext>>
        rollouts;
    private final Clock clock;

    public CurriculumPolicyAuthorizationService(
        LegacyCurriculumAuthorizationAdapter legacyEvaluator,
        TargetCurriculumAuthorizationAdapter targetEvaluator,
        PolicyRolloutModeResolver modeResolver,
        PolicyRolloutObserver observer,
        CompiledPolicyRegistry registry,
        Clock clock
    ) {
        EnumMap<CurriculumPolicyAction, BooleanPolicyRolloutExecutor<CurriculumAuthorizationContext>>
            configured = new EnumMap<>(CurriculumPolicyAction.class);
        for (CurriculumPolicyAction action : CurriculumPolicyAction.values()) {
            configured.put(action, new BooleanPolicyRolloutExecutor<>(
                legacyEvaluator,
                targetEvaluator,
                new CurriculumExpectedDifference(),
                modeResolver,
                observer,
                registry,
                CurriculumPolicyDomainSchema.BUNDLE_KEY,
                action.id()));
        }
        this.rollouts = Map.copyOf(configured);
        this.clock = clock;
    }

    public boolean evaluate(
        CurriculumPolicyAction action,
        SubjectAttributes legacySubject
    ) {
        Optional<AuthorizationSubjectSnapshot> subject = policySubject(legacySubject);
        Instant evaluatedAt = subject
            .map(AuthorizationSubjectSnapshot::evaluatedAt)
            .orElseGet(clock::instant);
        return rollouts.get(action).evaluate(
            new CurriculumAuthorizationContext(action, legacySubject, subject, evaluatedAt),
            evaluatedAt);
    }

    private Optional<AuthorizationSubjectSnapshot> policySubject(SubjectAttributes subject) {
        try {
            return Optional.of(subject.toAuthorizationSubjectSnapshot());
        } catch (IllegalStateException exception) {
            return Optional.empty();
        }
    }
}
