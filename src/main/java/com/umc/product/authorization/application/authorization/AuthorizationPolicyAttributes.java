package com.umc.product.authorization.application.authorization;

import com.umc.product.authorization.domain.policy.PolicyAttributeKey;
import com.umc.product.authorization.domain.policy.PolicyValue;
import com.umc.product.authorization.domain.policy.PolicyValueType;

public final class AuthorizationPolicyAttributes {

    public static final PolicyAttributeKey<PolicyValue.BooleanValue> AUTHENTICATED =
        new PolicyAttributeKey<>("subject.authenticated", PolicyValueType.BOOLEAN);
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> ACTIVE_CENTRAL_CORE =
        new PolicyAttributeKey<>("relation.activeCentralCore", PolicyValueType.BOOLEAN);

    private AuthorizationPolicyAttributes() {
    }
}
