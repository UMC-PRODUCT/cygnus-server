package com.umc.product.audit.application.authorization;

import java.util.List;
import java.util.Set;

import com.umc.product.authorization.domain.policy.ActionSchema;
import com.umc.product.authorization.domain.policy.AttributeSchema;
import com.umc.product.authorization.domain.policy.PolicyBundleKey;
import com.umc.product.authorization.domain.policy.PolicyDomainSchema;

public final class AuditPolicyDomainSchema {

    public static final String NAMESPACE = "audit";
    public static final String VERSION = "audit-1.0";
    public static final String POLICY_VERSION = "1.0.0";
    public static final PolicyBundleKey BUNDLE_KEY = new PolicyBundleKey(NAMESPACE, VERSION);

    private AuditPolicyDomainSchema() {
    }

    public static PolicyDomainSchema create() {
        return new PolicyDomainSchema(
            VERSION,
            List.of(new ActionSchema(
                AuditPolicyAction.LIST.id(),
                Set.of(AuditPolicyAttributes.ACTIVE_CENTRAL_MEMBER.name()),
                Set.of(),
                Set.of())),
            List.of(new AttributeSchema(
                AuditPolicyAttributes.ACTIVE_CENTRAL_MEMBER.name(),
                AuditPolicyAttributes.ACTIVE_CENTRAL_MEMBER.type(),
                Set.of())),
            List.of());
    }
}
