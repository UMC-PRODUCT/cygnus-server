package com.umc.product.organization.application.authorization;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.umc.product.authorization.domain.policy.ActionSchema;
import com.umc.product.authorization.domain.policy.AttributeSchema;
import com.umc.product.authorization.domain.policy.PolicyBundleKey;
import com.umc.product.authorization.domain.policy.PolicyDomainSchema;

public final class OrganizationPolicyDomainSchema {

    public static final String NAMESPACE = "organization";
    public static final String VERSION = "organization-1.0";
    public static final String POLICY_VERSION = "1.0.0";
    public static final PolicyBundleKey BUNDLE_KEY = new PolicyBundleKey(NAMESPACE, VERSION);

    private OrganizationPolicyDomainSchema() {
    }

    public static PolicyDomainSchema create() {
        Set<String> required = Set.of(
            OrganizationPolicyAttributes.ACTIVE_CENTRAL_CORE.name(),
            OrganizationPolicyAttributes.ACTIVE_SCHOOL_ADMIN.name(),
            OrganizationPolicyAttributes.ACTIVE_SCHOOL_CORE.name());
        return new PolicyDomainSchema(
            VERSION,
            Arrays.stream(OrganizationPolicyAction.values())
                .map(action -> new ActionSchema(action.id(), required, Set.of(), Set.of()))
                .toList(),
            List.of(
                attribute(OrganizationPolicyAttributes.ACTIVE_CENTRAL_CORE),
                attribute(OrganizationPolicyAttributes.ACTIVE_SCHOOL_ADMIN),
                attribute(OrganizationPolicyAttributes.ACTIVE_SCHOOL_CORE)),
            List.of());
    }

    private static AttributeSchema attribute(
        com.umc.product.authorization.domain.policy.PolicyAttributeKey<?> key
    ) {
        return new AttributeSchema(key.name(), key.type(), Set.of());
    }
}
