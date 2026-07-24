package com.umc.product.audit.application.authorization;

import com.umc.product.authorization.domain.policy.PolicyAttributeKey;
import com.umc.product.authorization.domain.policy.PolicyValue;
import com.umc.product.authorization.domain.policy.PolicyValueType;

public final class AuditPolicyAttributes {

    public static final PolicyAttributeKey<PolicyValue.BooleanValue> ACTIVE_CENTRAL_MEMBER =
        new PolicyAttributeKey<>("relation.activeCentralMember", PolicyValueType.BOOLEAN);

    private AuditPolicyAttributes() {
    }
}
