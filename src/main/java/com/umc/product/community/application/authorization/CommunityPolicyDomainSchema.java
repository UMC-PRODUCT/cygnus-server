package com.umc.product.community.application.authorization;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.umc.product.authorization.domain.policy.ActionSchema;
import com.umc.product.authorization.domain.policy.AttributeSchema;
import com.umc.product.authorization.domain.policy.PolicyAttributeKey;
import com.umc.product.authorization.domain.policy.PolicyBundleKey;
import com.umc.product.authorization.domain.policy.PolicyDomainSchema;

public final class CommunityPolicyDomainSchema {

    public static final String NAMESPACE = "community";
    public static final String VERSION = "community-1.0";
    public static final String POLICY_VERSION = "1.0.0";
    public static final PolicyBundleKey BUNDLE_KEY = new PolicyBundleKey(NAMESPACE, VERSION);

    private CommunityPolicyDomainSchema() {
    }

    public static PolicyDomainSchema create() {
        Set<String> required = Set.of(
            CommunityPolicyAttributes.AUTHENTICATED.name(),
            CommunityPolicyAttributes.AUTHOR_HAS_CHALLENGER_HISTORY.name(),
            CommunityPolicyAttributes.IS_AUTHOR.name(),
            CommunityPolicyAttributes.SUPER_ADMIN.name(),
            CommunityPolicyAttributes.ACTIVE_CENTRAL_CORE.name());
        return new PolicyDomainSchema(
            VERSION,
            Arrays.stream(CommunityPolicyAction.values())
                .map(action -> new ActionSchema(action.id(), required, Set.of(), Set.of()))
                .toList(),
            List.of(
                attribute(CommunityPolicyAttributes.AUTHENTICATED),
                attribute(CommunityPolicyAttributes.AUTHOR_HAS_CHALLENGER_HISTORY),
                attribute(CommunityPolicyAttributes.IS_AUTHOR),
                attribute(CommunityPolicyAttributes.SUPER_ADMIN),
                attribute(CommunityPolicyAttributes.ACTIVE_CENTRAL_CORE)),
            List.of());
    }

    private static AttributeSchema attribute(PolicyAttributeKey<?> key) {
        return new AttributeSchema(key.name(), key.type(), Set.of());
    }
}
