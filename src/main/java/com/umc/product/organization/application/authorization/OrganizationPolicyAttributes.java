package com.umc.product.organization.application.authorization;

import com.umc.product.authorization.domain.policy.PolicyAttributeKey;
import com.umc.product.authorization.domain.policy.PolicyValue;
import com.umc.product.authorization.domain.policy.PolicyValueType;

public final class OrganizationPolicyAttributes {

    public static final PolicyAttributeKey<PolicyValue.BooleanValue> ACTIVE_CENTRAL_CORE =
        booleanKey("relation.activeCentralCore");
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> ACTIVE_SCHOOL_ADMIN =
        booleanKey("relation.activeSchoolAdminForSubjectSchool");
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> ACTIVE_SCHOOL_CORE =
        booleanKey("relation.activeSchoolCoreForSubjectSchool");

    private OrganizationPolicyAttributes() {
    }

    private static PolicyAttributeKey<PolicyValue.BooleanValue> booleanKey(String name) {
        return new PolicyAttributeKey<>(name, PolicyValueType.BOOLEAN);
    }
}
