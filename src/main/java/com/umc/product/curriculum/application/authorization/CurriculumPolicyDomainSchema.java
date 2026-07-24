package com.umc.product.curriculum.application.authorization;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.umc.product.authorization.domain.policy.ActionSchema;
import com.umc.product.authorization.domain.policy.AttributeSchema;
import com.umc.product.authorization.domain.policy.PolicyAttributeKey;
import com.umc.product.authorization.domain.policy.PolicyBundleKey;
import com.umc.product.authorization.domain.policy.PolicyDomainSchema;

public final class CurriculumPolicyDomainSchema {

    public static final String NAMESPACE = "curriculum";
    public static final String VERSION = "curriculum-1.0";
    public static final String POLICY_VERSION = "1.0.0";
    public static final PolicyBundleKey BUNDLE_KEY = new PolicyBundleKey(NAMESPACE, VERSION);

    private CurriculumPolicyDomainSchema() {
    }

    public static PolicyDomainSchema create() {
        Set<String> required = Set.of(
            CurriculumPolicyAttributes.SUPER_ADMIN.name(),
            CurriculumPolicyAttributes.ACTIVE_CENTRAL_MEMBER.name(),
            CurriculumPolicyAttributes.ACTIVE_SCHOOL_ADMIN.name());
        return new PolicyDomainSchema(
            VERSION,
            Arrays.stream(CurriculumPolicyAction.values())
                .map(action -> new ActionSchema(action.id(), required, Set.of(), Set.of()))
                .toList(),
            List.of(
                attribute(CurriculumPolicyAttributes.SUPER_ADMIN),
                attribute(CurriculumPolicyAttributes.ACTIVE_CENTRAL_MEMBER),
                attribute(CurriculumPolicyAttributes.ACTIVE_SCHOOL_ADMIN)),
            List.of());
    }

    private static AttributeSchema attribute(PolicyAttributeKey<?> key) {
        return new AttributeSchema(key.name(), key.type(), Set.of());
    }
}
