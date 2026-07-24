package com.umc.product.form.application.authorization;

import com.umc.product.authorization.domain.policy.PolicyAttributeKey;
import com.umc.product.authorization.domain.policy.PolicyValue;
import com.umc.product.authorization.domain.policy.PolicyValueType;

public final class FormPolicyAttributes {

    public static final PolicyAttributeKey<PolicyValue.EnumValue> SUBJECT_KIND =
        new PolicyAttributeKey<>("subject.kind", PolicyValueType.ENUM);
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> USAGE_OWNER_AUTHORIZED =
        booleanKey("relation.usageOwnerAuthorized");
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> CONSUMER_AUTHORIZED =
        booleanKey("relation.consumerAuthorized");
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> IS_RESPONDENT =
        booleanKey("relation.isRespondent");
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> CAPABILITY_BOUND_TO_RESPONSE =
        booleanKey("relation.capabilityBoundToResponse");

    private FormPolicyAttributes() {
    }

    private static PolicyAttributeKey<PolicyValue.BooleanValue> booleanKey(String name) {
        return new PolicyAttributeKey<>(name, PolicyValueType.BOOLEAN);
    }
}
