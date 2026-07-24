package com.umc.product.term.application.authorization;

import com.umc.product.authorization.domain.policy.PolicyAttributeKey;
import com.umc.product.authorization.domain.policy.PolicyValue;
import com.umc.product.authorization.domain.policy.PolicyValueType;

public final class TermPolicyAttributes {

    public static final PolicyAttributeKey<PolicyValue.BooleanValue> SUPER_ADMIN =
        new PolicyAttributeKey<>("relation.superAdmin", PolicyValueType.BOOLEAN);

    private TermPolicyAttributes() {
    }
}
