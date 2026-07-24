package com.umc.product.notice.application.authorization;

import com.umc.product.authorization.domain.policy.PolicyAttributeKey;
import com.umc.product.authorization.domain.policy.PolicyValue;
import com.umc.product.authorization.domain.policy.PolicyValueType;

public final class NoticePolicyAttributes {

    public static final PolicyAttributeKey<PolicyValue.BooleanValue> SUPER_ADMIN =
        booleanKey("relation.superAdmin");
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> ACTIVE_CENTRAL_CORE =
        booleanKey("relation.activeCentralCore");
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> TARGET_CHALLENGER =
        booleanKey("relation.isTargetChallenger");
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> ACTIVE_ROLE_CAN_READ_TARGET =
        booleanKey("relation.activeRoleCanReadTarget");
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> AUTHOR =
        booleanKey("relation.isNoticeAuthor");
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> ACTIVE_MANAGER_FOR_TARGET =
        booleanKey("relation.activeManagerForTarget");
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> ACTIVE_CREATOR_FOR_TARGET =
        booleanKey("relation.activeCreatorForTarget");

    private NoticePolicyAttributes() {
    }

    private static PolicyAttributeKey<PolicyValue.BooleanValue> booleanKey(String name) {
        return new PolicyAttributeKey<>(name, PolicyValueType.BOOLEAN);
    }
}
