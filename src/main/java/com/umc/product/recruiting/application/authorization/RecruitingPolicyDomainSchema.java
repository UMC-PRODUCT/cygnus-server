package com.umc.product.recruiting.application.authorization;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.umc.product.authorization.domain.policy.ActionSchema;
import com.umc.product.authorization.domain.policy.AttributeSchema;
import com.umc.product.authorization.domain.policy.PolicyAttributeKey;
import com.umc.product.authorization.domain.policy.PolicyBundleKey;
import com.umc.product.authorization.domain.policy.PolicyDomainSchema;

public final class RecruitingPolicyDomainSchema {

    public static final String NAMESPACE = "recruiting";
    public static final String VERSION = "recruiting-1.0";
    public static final String POLICY_VERSION = "1.0.0";
    public static final PolicyBundleKey BUNDLE_KEY = new PolicyBundleKey(NAMESPACE, VERSION);

    private RecruitingPolicyDomainSchema() {
    }

    public static PolicyDomainSchema create() {
        Set<String> required = Set.of(
            RecruitingPolicyAttributes.RESOURCE_SPECIFIED.name(),
            RecruitingPolicyAttributes.SUPER_ADMIN.name(),
            RecruitingPolicyAttributes.ACTIVE_ANY_CENTRAL_CORE.name(),
            RecruitingPolicyAttributes.ACTIVE_ANY_SCHOOL_CORE.name(),
            RecruitingPolicyAttributes.ACTIVE_CENTRAL_CORE_IN_TARGET.name(),
            RecruitingPolicyAttributes.ACTIVE_SCHOOL_CORE_FOR_TARGET.name());
        return new PolicyDomainSchema(
            VERSION,
            Arrays.stream(RecruitingPolicyAction.values())
                .map(action -> new ActionSchema(action.id(), required, Set.of(), Set.of()))
                .toList(),
            List.of(
                attribute(RecruitingPolicyAttributes.RESOURCE_SPECIFIED),
                attribute(RecruitingPolicyAttributes.SUPER_ADMIN),
                attribute(RecruitingPolicyAttributes.ACTIVE_ANY_CENTRAL_CORE),
                attribute(RecruitingPolicyAttributes.ACTIVE_ANY_SCHOOL_CORE),
                attribute(RecruitingPolicyAttributes.ACTIVE_CENTRAL_CORE_IN_TARGET),
                attribute(RecruitingPolicyAttributes.ACTIVE_SCHOOL_CORE_FOR_TARGET)),
            List.of());
    }

    private static AttributeSchema attribute(PolicyAttributeKey<?> key) {
        return new AttributeSchema(key.name(), key.type(), Set.of());
    }
}
