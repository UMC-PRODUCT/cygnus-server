package com.umc.product.analytics.application.authorization;

import com.umc.product.authorization.domain.policy.PolicyAttributeKey;
import com.umc.product.authorization.domain.policy.PolicyValue;
import com.umc.product.authorization.domain.policy.PolicyValueType;

public final class AnalyticsPolicyAttributes {

    public static final PolicyAttributeKey<PolicyValue.BooleanValue> ACTIVE_SUPER_ADMIN =
        new PolicyAttributeKey<>("relation.activeSuperAdmin", PolicyValueType.BOOLEAN);
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> ACTIVE_CENTRAL_MEMBER =
        new PolicyAttributeKey<>("relation.activeCentralMember", PolicyValueType.BOOLEAN);
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> ACTIVE_CHAPTER_PRESIDENT =
        new PolicyAttributeKey<>("relation.activeChapterPresident", PolicyValueType.BOOLEAN);
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> ACTIVE_SCHOOL_OPERATOR =
        new PolicyAttributeKey<>("relation.activeSchoolOperator", PolicyValueType.BOOLEAN);

    private AnalyticsPolicyAttributes() {
    }
}
