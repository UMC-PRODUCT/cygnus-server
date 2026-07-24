package com.umc.product.recruiting.application.authorization;

import com.umc.product.authorization.domain.policy.PolicyAttributeKey;
import com.umc.product.authorization.domain.policy.PolicyValue;
import com.umc.product.authorization.domain.policy.PolicyValueType;

public final class RecruitingPolicyAttributes {

    public static final PolicyAttributeKey<PolicyValue.BooleanValue> RESOURCE_SPECIFIED =
        booleanKey("resource.specified");
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> SUPER_ADMIN =
        booleanKey("relation.superAdmin");
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> ACTIVE_ANY_CENTRAL_CORE =
        booleanKey("relation.activeAnyCentralCore");
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> ACTIVE_ANY_SCHOOL_CORE =
        booleanKey("relation.activeAnySchoolCore");
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> ACTIVE_CENTRAL_CORE_IN_TARGET =
        booleanKey("relation.activeCentralCoreInTargetGisu");
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> ACTIVE_SCHOOL_CORE_FOR_TARGET =
        booleanKey("relation.activeSchoolCoreForTarget");

    private RecruitingPolicyAttributes() {
    }

    private static PolicyAttributeKey<PolicyValue.BooleanValue> booleanKey(String name) {
        return new PolicyAttributeKey<>(name, PolicyValueType.BOOLEAN);
    }
}
