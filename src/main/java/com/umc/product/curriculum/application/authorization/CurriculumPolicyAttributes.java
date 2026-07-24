package com.umc.product.curriculum.application.authorization;

import com.umc.product.authorization.domain.policy.PolicyAttributeKey;
import com.umc.product.authorization.domain.policy.PolicyValue;
import com.umc.product.authorization.domain.policy.PolicyValueType;

public final class CurriculumPolicyAttributes {

    public static final PolicyAttributeKey<PolicyValue.BooleanValue> SUPER_ADMIN =
        booleanKey("relation.superAdmin");
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> ACTIVE_CENTRAL_MEMBER =
        booleanKey("relation.activeCentralMember");
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> ACTIVE_SCHOOL_ADMIN =
        booleanKey("relation.activeSchoolAdmin");

    private CurriculumPolicyAttributes() {
    }

    private static PolicyAttributeKey<PolicyValue.BooleanValue> booleanKey(String name) {
        return new PolicyAttributeKey<>(name, PolicyValueType.BOOLEAN);
    }
}
