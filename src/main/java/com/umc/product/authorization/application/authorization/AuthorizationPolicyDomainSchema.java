package com.umc.product.authorization.application.authorization;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.umc.product.authorization.domain.policy.ActionSchema;
import com.umc.product.authorization.domain.policy.AttributeSchema;
import com.umc.product.authorization.domain.policy.PolicyBundleKey;
import com.umc.product.authorization.domain.policy.PolicyDomainSchema;

public final class AuthorizationPolicyDomainSchema {

    public static final String NAMESPACE = "authorization";
    public static final String VERSION = "authorization-1.0";
    public static final String POLICY_VERSION = "1.0.0";
    public static final PolicyBundleKey BUNDLE_KEY = new PolicyBundleKey(NAMESPACE, VERSION);

    private AuthorizationPolicyDomainSchema() {
    }

    public static PolicyDomainSchema create() {
        Set<String> required = Set.of(
            AuthorizationPolicyAttributes.AUTHENTICATED.name(),
            AuthorizationPolicyAttributes.ACTIVE_CENTRAL_CORE.name());
        return new PolicyDomainSchema(
            VERSION,
            Arrays.stream(AuthorizationPolicyAction.values())
                .map(action -> new ActionSchema(action.id(), required, Set.of(), Set.of()))
                .toList(),
            List.of(
                new AttributeSchema(
                    AuthorizationPolicyAttributes.AUTHENTICATED.name(),
                    AuthorizationPolicyAttributes.AUTHENTICATED.type(),
                    Set.of()),
                new AttributeSchema(
                    AuthorizationPolicyAttributes.ACTIVE_CENTRAL_CORE.name(),
                    AuthorizationPolicyAttributes.ACTIVE_CENTRAL_CORE.type(),
                    Set.of())),
            List.of());
    }
}
