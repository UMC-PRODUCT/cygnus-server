package com.umc.product.analytics.application.authorization;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.umc.product.authorization.domain.policy.ActionSchema;
import com.umc.product.authorization.domain.policy.AttributeSchema;
import com.umc.product.authorization.domain.policy.PolicyAttributeKey;
import com.umc.product.authorization.domain.policy.PolicyBundleKey;
import com.umc.product.authorization.domain.policy.PolicyDomainSchema;

public final class AnalyticsPolicyDomainSchema {

    public static final String NAMESPACE = "analytics";
    public static final String VERSION = "analytics-1.0";
    public static final String POLICY_VERSION = "1.0.0";
    public static final PolicyBundleKey BUNDLE_KEY = new PolicyBundleKey(NAMESPACE, VERSION);

    private AnalyticsPolicyDomainSchema() {
    }

    public static PolicyDomainSchema create() {
        Set<String> required = Set.of(
            AnalyticsPolicyAttributes.ACTIVE_SUPER_ADMIN.name(),
            AnalyticsPolicyAttributes.ACTIVE_CENTRAL_MEMBER.name(),
            AnalyticsPolicyAttributes.ACTIVE_CHAPTER_PRESIDENT.name(),
            AnalyticsPolicyAttributes.ACTIVE_SCHOOL_OPERATOR.name());
        return new PolicyDomainSchema(
            VERSION,
            Arrays.stream(AnalyticsPolicyAction.values())
                .map(action -> new ActionSchema(action.id(), required, Set.of(), Set.of()))
                .toList(),
            List.of(
                attribute(AnalyticsPolicyAttributes.ACTIVE_SUPER_ADMIN),
                attribute(AnalyticsPolicyAttributes.ACTIVE_CENTRAL_MEMBER),
                attribute(AnalyticsPolicyAttributes.ACTIVE_CHAPTER_PRESIDENT),
                attribute(AnalyticsPolicyAttributes.ACTIVE_SCHOOL_OPERATOR)),
            List.of());
    }

    private static AttributeSchema attribute(PolicyAttributeKey<?> key) {
        return new AttributeSchema(key.name(), key.type(), Set.of());
    }
}
