package com.umc.product.challenger.application.authorization;

import com.umc.product.authorization.domain.policy.PolicyAttributeKey;
import com.umc.product.authorization.domain.policy.PolicyValue;
import com.umc.product.authorization.domain.policy.PolicyValueType;

public final class ChallengerPolicyAttributes {

    public static final PolicyAttributeKey<PolicyValue.BooleanValue> ACTIVE_CENTRAL_CORE =
        booleanKey("relation.activeCentralCore");
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> ACTIVE_SCHOOL_CORE =
        booleanKey("relation.activeSchoolCore");
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> ACTIVE_CENTRAL_MEMBER_IN_TARGET_GISU =
        booleanKey("relation.activeCentralMemberInTargetGisu");
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> ACTIVE_SCHOOL_CORE_FOR_TARGET =
        booleanKey("relation.activeSchoolCoreForTarget");

    private ChallengerPolicyAttributes() {
    }

    private static PolicyAttributeKey<PolicyValue.BooleanValue> booleanKey(String name) {
        return new PolicyAttributeKey<>(name, PolicyValueType.BOOLEAN);
    }
}
