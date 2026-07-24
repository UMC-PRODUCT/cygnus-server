package com.umc.product.member.application.authorization;

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
public class MemberPolicyAuthorizationService {

    private final Map<MemberPolicyAction, BooleanPolicyRolloutExecutor<MemberAuthorizationContext>> rollouts;
    private final Clock clock;

    public MemberPolicyAuthorizationService(
        LegacyMemberAuthorizationAdapter legacyEvaluator,
        TargetMemberAuthorizationAdapter targetEvaluator,
        PolicyRolloutModeResolver modeResolver,
        PolicyRolloutObserver observer,
        CompiledPolicyRegistry registry,
        Clock clock
    ) {
        EnumMap<MemberPolicyAction, BooleanPolicyRolloutExecutor<MemberAuthorizationContext>> configured =
            new EnumMap<>(MemberPolicyAction.class);
        for (MemberPolicyAction action : MemberPolicyAction.values()) {
            configured.put(action, new BooleanPolicyRolloutExecutor<>(
                legacyEvaluator,
                targetEvaluator,
                new MemberExpectedDifference(),
                modeResolver,
                observer,
                registry,
                MemberPolicyDomainSchema.BUNDLE_KEY,
                action.id()));
        }
        this.rollouts = Map.copyOf(configured);
        this.clock = clock;
    }

    public boolean evaluate(MemberPolicyAction action, SubjectAttributes legacySubject) {
        Optional<AuthorizationSubjectSnapshot> subject = policySubject(legacySubject);
        Instant evaluatedAt = subject
            .map(AuthorizationSubjectSnapshot::evaluatedAt)
            .orElseGet(clock::instant);
        return rollouts.get(action).evaluate(
            new MemberAuthorizationContext(action, legacySubject, subject, evaluatedAt),
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
