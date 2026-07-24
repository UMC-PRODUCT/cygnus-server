package com.umc.product.member.application.authorization;

import com.umc.product.authorization.domain.policy.PolicyAttributeKey;
import com.umc.product.authorization.domain.policy.PolicyValue;
import com.umc.product.authorization.domain.policy.PolicyValueType;

public final class MemberPolicyAttributes {

    public static final PolicyAttributeKey<PolicyValue.BooleanValue> CHALLENGER =
        new PolicyAttributeKey<>("relation.challenger", PolicyValueType.BOOLEAN);
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> ACTIVE_CENTRAL_CORE =
        new PolicyAttributeKey<>("relation.activeCentralCore", PolicyValueType.BOOLEAN);

    private MemberPolicyAttributes() {
    }
}
