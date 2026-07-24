package com.umc.product.feedback.application.authorization;

import com.umc.product.authorization.domain.policy.PolicyAttributeKey;
import com.umc.product.authorization.domain.policy.PolicyValue;
import com.umc.product.authorization.domain.policy.PolicyValueType;

public final class FeedbackPolicyAttributes {

    public static final PolicyAttributeKey<PolicyValue.BooleanValue> ACTIVE_CENTRAL_MEMBER =
        booleanKey("relation.activeCentralMemberInTargetGisu");
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> ACTIVE_CHALLENGER =
        booleanKey("relation.activeChallengerInTargetGisu");
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> HAS_PREVIOUS_GISU =
        booleanKey("relation.hasPreviousGisu");
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> GENERATION_TEN_PLAN =
        booleanKey("relation.generationTenPlanChallenger");
    public static final PolicyAttributeKey<PolicyValue.EnumValue> RESOURCE_TARGET_TYPE =
        new PolicyAttributeKey<>("resource.feedback.targetType", PolicyValueType.ENUM);

    private FeedbackPolicyAttributes() {
    }

    private static PolicyAttributeKey<PolicyValue.BooleanValue> booleanKey(String name) {
        return new PolicyAttributeKey<>(name, PolicyValueType.BOOLEAN);
    }
}
