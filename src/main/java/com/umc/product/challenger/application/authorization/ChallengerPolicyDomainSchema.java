package com.umc.product.challenger.application.authorization;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.umc.product.authorization.domain.policy.ActionSchema;
import com.umc.product.authorization.domain.policy.AttributeSchema;
import com.umc.product.authorization.domain.policy.PolicyAttributeKey;
import com.umc.product.authorization.domain.policy.PolicyBundleKey;
import com.umc.product.authorization.domain.policy.PolicyDomainSchema;

public final class ChallengerPolicyDomainSchema {

    public static final String NAMESPACE = "challenger";
    public static final String VERSION = "challenger-1.0";
    public static final String POLICY_VERSION = "1.0.0";
    public static final PolicyBundleKey BUNDLE_KEY = new PolicyBundleKey(NAMESPACE, VERSION);

    private ChallengerPolicyDomainSchema() {
    }

    public static PolicyDomainSchema create() {
        Set<String> required = Set.of(
            ChallengerPolicyAttributes.ACTIVE_CENTRAL_CORE.name(),
            ChallengerPolicyAttributes.ACTIVE_SCHOOL_CORE.name(),
            ChallengerPolicyAttributes.ACTIVE_CENTRAL_MEMBER_IN_TARGET_GISU.name(),
            ChallengerPolicyAttributes.ACTIVE_SCHOOL_CORE_FOR_TARGET.name());
        return new PolicyDomainSchema(
            VERSION,
            Arrays.stream(ChallengerPolicyAction.values())
                .map(action -> new ActionSchema(action.id(), required, Set.of(), Set.of()))
                .toList(),
            List.of(
                attribute(ChallengerPolicyAttributes.ACTIVE_CENTRAL_CORE),
                attribute(ChallengerPolicyAttributes.ACTIVE_SCHOOL_CORE),
                attribute(ChallengerPolicyAttributes.ACTIVE_CENTRAL_MEMBER_IN_TARGET_GISU),
                attribute(ChallengerPolicyAttributes.ACTIVE_SCHOOL_CORE_FOR_TARGET)),
            List.of());
    }

    private static AttributeSchema attribute(PolicyAttributeKey<?> key) {
        return new AttributeSchema(key.name(), key.type(), Set.of());
    }
}
