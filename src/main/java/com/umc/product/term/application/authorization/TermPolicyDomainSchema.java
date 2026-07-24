package com.umc.product.term.application.authorization;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.umc.product.authorization.domain.policy.ActionSchema;
import com.umc.product.authorization.domain.policy.AttributeSchema;
import com.umc.product.authorization.domain.policy.PolicyBundleKey;
import com.umc.product.authorization.domain.policy.PolicyDomainSchema;

public final class TermPolicyDomainSchema {

    public static final String NAMESPACE = "term";
    public static final String VERSION = "term-1.0";
    public static final String POLICY_VERSION = "1.0.0";
    public static final PolicyBundleKey BUNDLE_KEY = new PolicyBundleKey(NAMESPACE, VERSION);

    private TermPolicyDomainSchema() {
    }

    public static PolicyDomainSchema create() {
        return new PolicyDomainSchema(
            VERSION,
            Arrays.stream(TermPolicyAction.values())
                .map(action -> new ActionSchema(
                    action.id(),
                    Set.of(TermPolicyAttributes.SUPER_ADMIN.name()),
                    Set.of(),
                    Set.of()))
                .toList(),
            List.of(new AttributeSchema(
                TermPolicyAttributes.SUPER_ADMIN.name(),
                TermPolicyAttributes.SUPER_ADMIN.type(),
                Set.of())),
            List.of());
    }
}
