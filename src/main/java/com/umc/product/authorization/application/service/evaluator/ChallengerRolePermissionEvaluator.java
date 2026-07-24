package com.umc.product.authorization.application.service.evaluator;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.authorization.AuthorizationExpectedDifference;
import com.umc.product.authorization.application.authorization.AuthorizationPolicyAction;
import com.umc.product.authorization.application.authorization.AuthorizationPolicyContext;
import com.umc.product.authorization.application.authorization.AuthorizationPolicyDomainSchema;
import com.umc.product.authorization.application.authorization.LegacyAuthorizationPolicyAdapter;
import com.umc.product.authorization.application.authorization.TargetAuthorizationPolicyAdapter;
import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.application.service.policy.CompiledPolicyRegistry;
import com.umc.product.authorization.application.service.policy.rollout.BooleanPolicyRolloutExecutor;
import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutModeResolver;
import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutObserver;
import com.umc.product.authorization.domain.AuthorizationSubjectSnapshot;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;

@Component
public class ChallengerRolePermissionEvaluator implements ResourcePermissionEvaluator {

    private final Map<AuthorizationPolicyAction, BooleanPolicyRolloutExecutor<AuthorizationPolicyContext>> rollouts;
    private final Clock clock;

    public ChallengerRolePermissionEvaluator(
        LegacyAuthorizationPolicyAdapter legacyEvaluator,
        TargetAuthorizationPolicyAdapter targetEvaluator,
        PolicyRolloutModeResolver modeResolver,
        PolicyRolloutObserver observer,
        CompiledPolicyRegistry registry,
        Clock clock
    ) {
        EnumMap<AuthorizationPolicyAction, BooleanPolicyRolloutExecutor<AuthorizationPolicyContext>> configured =
            new EnumMap<>(AuthorizationPolicyAction.class);
        for (AuthorizationPolicyAction action : AuthorizationPolicyAction.values()) {
            configured.put(action, new BooleanPolicyRolloutExecutor<>(
                legacyEvaluator,
                targetEvaluator,
                new AuthorizationExpectedDifference(),
                modeResolver,
                observer,
                registry,
                AuthorizationPolicyDomainSchema.BUNDLE_KEY,
                action.id()));
        }
        this.rollouts = Map.copyOf(configured);
        this.clock = clock;
    }

    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.CHALLENGER_ROLE;
    }

    @Override
    public boolean evaluate(
        SubjectAttributes subjectAttributes,
        ResourcePermission resourcePermission
    ) {
        AuthorizationPolicyAction action = AuthorizationPolicyAction.from(
            resourcePermission.permission());
        Optional<AuthorizationSubjectSnapshot> subject = policySubject(subjectAttributes);
        Instant evaluatedAt = subject
            .map(AuthorizationSubjectSnapshot::evaluatedAt)
            .orElseGet(clock::instant);
        return rollouts.get(action).evaluate(
            new AuthorizationPolicyContext(action, subjectAttributes, subject, evaluatedAt),
            evaluatedAt);
    }

    private Optional<AuthorizationSubjectSnapshot> policySubject(
        SubjectAttributes subjectAttributes
    ) {
        try {
            return Optional.of(subjectAttributes.toAuthorizationSubjectSnapshot());
        } catch (IllegalStateException exception) {
            return Optional.empty();
        }
    }
}
