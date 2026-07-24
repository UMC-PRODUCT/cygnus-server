package com.umc.product.maintenance.application.authorization;

import java.util.List;
import java.util.Set;

import com.umc.product.authorization.domain.policy.ActionSchema;
import com.umc.product.authorization.domain.policy.AttributeSchema;
import com.umc.product.authorization.domain.policy.PolicyAttributeKey;
import com.umc.product.authorization.domain.policy.PolicyBundleKey;
import com.umc.product.authorization.domain.policy.PolicyDomainSchema;
import com.umc.product.authorization.domain.policy.PolicyValue;
import com.umc.product.authorization.domain.policy.PolicyValueType;

public final class MaintenancePolicyDomainSchema {

    public static final String NAMESPACE = "maintenance";
    public static final String VERSION = "maintenance-1.0";
    public static final String POLICY_VERSION = "1.0.0";
    public static final PolicyBundleKey BUNDLE_KEY = new PolicyBundleKey(NAMESPACE, VERSION);
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> SUPER_ADMIN =
        new PolicyAttributeKey<>("relation.superAdmin", PolicyValueType.BOOLEAN);

    private MaintenancePolicyDomainSchema() {
    }

    public static PolicyDomainSchema create() {
        return new PolicyDomainSchema(
            VERSION,
            List.of(new ActionSchema(
                MaintenancePolicyAction.BYPASS.id(),
                Set.of(SUPER_ADMIN.name()),
                Set.of(),
                Set.of())),
            List.of(new AttributeSchema(SUPER_ADMIN.name(), SUPER_ADMIN.type(), Set.of())),
            List.of());
    }
}
