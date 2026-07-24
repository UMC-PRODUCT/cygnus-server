package com.umc.product.notification.application.service.evaluator;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.application.service.policy.CompiledPolicyRegistry;
import com.umc.product.authorization.application.service.policy.rollout.BooleanPolicyRolloutExecutor;
import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutModeResolver;
import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutObserver;
import com.umc.product.authorization.domain.AuthorizationSubjectSnapshot;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.notification.application.authorization.LegacyNotificationAuthorizationAdapter;
import com.umc.product.notification.application.authorization.NotificationAuthorizationContext;
import com.umc.product.notification.application.authorization.NotificationExpectedDifference;
import com.umc.product.notification.application.authorization.NotificationPolicyAction;
import com.umc.product.notification.application.authorization.NotificationPolicyDomainSchema;
import com.umc.product.notification.application.authorization.TargetNotificationAuthorizationAdapter;

@Component
public class FcmPermissionEvaluator implements ResourcePermissionEvaluator {

    private final BooleanPolicyRolloutExecutor<NotificationAuthorizationContext> sendRollout;
    private final BooleanPolicyRolloutExecutor<NotificationAuthorizationContext> deleteRollout;
    private final Clock clock;

    public FcmPermissionEvaluator(
        LegacyNotificationAuthorizationAdapter legacyEvaluator,
        TargetNotificationAuthorizationAdapter targetEvaluator,
        PolicyRolloutModeResolver modeResolver,
        PolicyRolloutObserver observer,
        CompiledPolicyRegistry registry,
        Clock clock
    ) {
        this.sendRollout = new BooleanPolicyRolloutExecutor<>(
            legacyEvaluator,
            targetEvaluator,
            new NotificationExpectedDifference(),
            modeResolver,
            observer,
            registry,
            NotificationPolicyDomainSchema.BUNDLE_KEY,
            NotificationPolicyAction.SEND_FCM.id());
        this.deleteRollout = new BooleanPolicyRolloutExecutor<>(
            legacyEvaluator,
            targetEvaluator,
            new NotificationExpectedDifference(),
            modeResolver,
            observer,
            registry,
            NotificationPolicyDomainSchema.BUNDLE_KEY,
            NotificationPolicyAction.DELETE_TOKEN.id());
        this.clock = clock;
    }

    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.FCM;
    }

    @Override
    public boolean evaluate(SubjectAttributes subjectAttributes, ResourcePermission resourcePermission) {
        return switch (resourcePermission.permission()) {
            case WRITE -> evaluate(subjectAttributes, NotificationPolicyAction.SEND_FCM);
            case DELETE -> evaluate(subjectAttributes, NotificationPolicyAction.DELETE_TOKEN);
            default -> false;
        };
    }

    private boolean evaluate(
        SubjectAttributes subjectAttributes,
        NotificationPolicyAction action
    ) {
        Optional<AuthorizationSubjectSnapshot> subject = policySubject(subjectAttributes);
        Instant evaluatedAt = subject
            .map(AuthorizationSubjectSnapshot::evaluatedAt)
            .orElseGet(clock::instant);
        BooleanPolicyRolloutExecutor<NotificationAuthorizationContext> rollout =
            action == NotificationPolicyAction.SEND_FCM ? sendRollout : deleteRollout;
        return rollout.evaluate(
            new NotificationAuthorizationContext(
                action,
                Optional.of(subjectAttributes),
                subject,
                false,
                evaluatedAt),
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
