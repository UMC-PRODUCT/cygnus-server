package com.umc.product.community.application.authorization;

import com.umc.product.authorization.domain.policy.PolicyAttributeKey;
import com.umc.product.authorization.domain.policy.PolicyValue;
import com.umc.product.authorization.domain.policy.PolicyValueType;

public final class CommunityPolicyAttributes {

    public static final PolicyAttributeKey<PolicyValue.BooleanValue> AUTHENTICATED =
        booleanKey("relation.authenticated");
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> AUTHOR_HAS_CHALLENGER_HISTORY =
        booleanKey("relation.authorHasChallengerHistory");
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> IS_AUTHOR =
        booleanKey("relation.isAuthor");
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> SUPER_ADMIN =
        booleanKey("relation.superAdmin");
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> ACTIVE_CENTRAL_CORE =
        booleanKey("relation.activeCentralCore");

    private CommunityPolicyAttributes() {
    }

    private static PolicyAttributeKey<PolicyValue.BooleanValue> booleanKey(String name) {
        return new PolicyAttributeKey<>(name, PolicyValueType.BOOLEAN);
    }
}
