package com.umc.product.organization.application.authorization;

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
public class OrganizationPolicyAuthorizationService {

    private final Map<OrganizationPolicyAction, BooleanPolicyRolloutExecutor<OrganizationAuthorizationContext>>
        rollouts;
    private final Clock clock;

    public OrganizationPolicyAuthorizationService(
        LegacyOrganizationAuthorizationAdapter legacyEvaluator,
        TargetOrganizationAuthorizationAdapter targetEvaluator,
        PolicyRolloutModeResolver modeResolver,
        PolicyRolloutObserver observer,
        CompiledPolicyRegistry registry,
        Clock clock
    ) {
        EnumMap<OrganizationPolicyAction, BooleanPolicyRolloutExecutor<OrganizationAuthorizationContext>>
            configured = new EnumMap<>(OrganizationPolicyAction.class);
        for (OrganizationPolicyAction action : OrganizationPolicyAction.values()) {
            configured.put(action, new BooleanPolicyRolloutExecutor<>(
                legacyEvaluator,
                targetEvaluator,
                new OrganizationExpectedDifference(),
                modeResolver,
                observer,
                registry,
                OrganizationPolicyDomainSchema.BUNDLE_KEY,
                action.id()));
        }
        this.rollouts = Map.copyOf(configured);
        this.clock = clock;
    }

    public boolean evaluate(OrganizationPolicyAction action, SubjectAttributes legacySubject) {
        Optional<AuthorizationSubjectSnapshot> subject = policySubject(legacySubject);
        Instant evaluatedAt = subject
            .map(AuthorizationSubjectSnapshot::evaluatedAt)
            .orElseGet(clock::instant);
        return rollouts.get(action).evaluate(
            new OrganizationAuthorizationContext(action, legacySubject, subject, evaluatedAt),
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
