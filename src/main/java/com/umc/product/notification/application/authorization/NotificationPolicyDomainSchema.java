package com.umc.product.notification.application.authorization;

import java.util.List;
import java.util.Set;

import com.umc.product.authorization.domain.policy.ActionSchema;
import com.umc.product.authorization.domain.policy.AttributeSchema;
import com.umc.product.authorization.domain.policy.PolicyBundleKey;
import com.umc.product.authorization.domain.policy.PolicyDomainSchema;

public final class NotificationPolicyDomainSchema {

    public static final String NAMESPACE = "notification";
    public static final String VERSION = "notification-1.0";
    public static final String POLICY_VERSION = "1.0.0";
    public static final PolicyBundleKey BUNDLE_KEY = new PolicyBundleKey(NAMESPACE, VERSION);

    private NotificationPolicyDomainSchema() {
    }

    public static PolicyDomainSchema create() {
        return new PolicyDomainSchema(
            VERSION,
            List.of(
                new ActionSchema(
                    NotificationPolicyAction.SEND_FCM.id(),
                    Set.of(NotificationPolicyAttributes.ACTIVE_CENTRAL_CORE.name()),
                    Set.of(),
                    Set.of()),
                new ActionSchema(
                    NotificationPolicyAction.DELETE_TOKEN.id(),
                    Set.of(),
                    Set.of(
                        NotificationPolicyAttributes.ACTIVE_CENTRAL_CORE.name(),
                        NotificationPolicyAttributes.TOKEN_OWNER.name()),
                    Set.of())),
            List.of(
                new AttributeSchema(
                    NotificationPolicyAttributes.ACTIVE_CENTRAL_CORE.name(),
                    NotificationPolicyAttributes.ACTIVE_CENTRAL_CORE.type(),
                    Set.of()),
                new AttributeSchema(
                    NotificationPolicyAttributes.TOKEN_OWNER.name(),
                    NotificationPolicyAttributes.TOKEN_OWNER.type(),
                    Set.of())),
            List.of());
    }
}
