package com.umc.product.blog.application.authorization;

import com.umc.product.authorization.domain.policy.PolicyAttributeKey;
import com.umc.product.authorization.domain.policy.PolicyValue;
import com.umc.product.authorization.domain.policy.PolicyValueType;

public final class BlogPolicyAttributes {

    public static final PolicyAttributeKey<PolicyValue.BooleanValue> AUTHOR =
        new PolicyAttributeKey<>("relation.isAuthor", PolicyValueType.BOOLEAN);
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> SUPER_ADMIN =
        new PolicyAttributeKey<>("relation.superAdmin", PolicyValueType.BOOLEAN);

    private BlogPolicyAttributes() {
    }
}
