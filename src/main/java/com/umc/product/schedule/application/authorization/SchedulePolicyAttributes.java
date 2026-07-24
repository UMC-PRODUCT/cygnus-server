package com.umc.product.schedule.application.authorization;

import com.umc.product.authorization.domain.policy.PolicyAttributeKey;
import com.umc.product.authorization.domain.policy.PolicyValue;
import com.umc.product.authorization.domain.policy.PolicyValueType;

public final class SchedulePolicyAttributes {

    public static final PolicyAttributeKey<PolicyValue.BooleanValue> SUPER_ADMIN =
        booleanKey("relation.superAdmin");
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> CHALLENGER_HISTORY =
        booleanKey("relation.challengerHistory");
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> AUTHOR =
        booleanKey("relation.isAuthor");
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> PARTICIPANT =
        booleanKey("relation.isParticipant");
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> RESOURCE_SPECIFIED =
        booleanKey("resource.specified");
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> ACTIVE_OPERATING_STAFF =
        booleanKey("relation.activeOperatingStaff");
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> ACTIVE_TARGET_GISU_STAFF =
        booleanKey("relation.activeOperatingStaffInTargetGisu");

    private SchedulePolicyAttributes() {
    }

    private static PolicyAttributeKey<PolicyValue.BooleanValue> booleanKey(String name) {
        return new PolicyAttributeKey<>(name, PolicyValueType.BOOLEAN);
    }
}
