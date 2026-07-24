package com.umc.product.notice.application.authorization;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.umc.product.authorization.domain.policy.ActionSchema;
import com.umc.product.authorization.domain.policy.AttributeSchema;
import com.umc.product.authorization.domain.policy.PolicyAttributeKey;
import com.umc.product.authorization.domain.policy.PolicyBundleKey;
import com.umc.product.authorization.domain.policy.PolicyDomainSchema;

public final class NoticePolicyDomainSchema {

    public static final String NAMESPACE = "notice";
    public static final String VERSION = "notice-1.0";
    public static final String POLICY_VERSION = "1.0.0";
    public static final PolicyBundleKey BUNDLE_KEY = new PolicyBundleKey(NAMESPACE, VERSION);

    private NoticePolicyDomainSchema() {
    }

    public static PolicyDomainSchema create() {
        Set<String> required = Set.of(
            NoticePolicyAttributes.SUPER_ADMIN.name(),
            NoticePolicyAttributes.ACTIVE_CENTRAL_CORE.name(),
            NoticePolicyAttributes.TARGET_CHALLENGER.name(),
            NoticePolicyAttributes.ACTIVE_ROLE_CAN_READ_TARGET.name(),
            NoticePolicyAttributes.AUTHOR.name(),
            NoticePolicyAttributes.ACTIVE_MANAGER_FOR_TARGET.name(),
            NoticePolicyAttributes.ACTIVE_CREATOR_FOR_TARGET.name());
        return new PolicyDomainSchema(
            VERSION,
            Arrays.stream(NoticePolicyAction.values())
                .map(action -> new ActionSchema(action.id(), required, Set.of(), Set.of()))
                .toList(),
            List.of(
                attribute(NoticePolicyAttributes.SUPER_ADMIN),
                attribute(NoticePolicyAttributes.ACTIVE_CENTRAL_CORE),
                attribute(NoticePolicyAttributes.TARGET_CHALLENGER),
                attribute(NoticePolicyAttributes.ACTIVE_ROLE_CAN_READ_TARGET),
                attribute(NoticePolicyAttributes.AUTHOR),
                attribute(NoticePolicyAttributes.ACTIVE_MANAGER_FOR_TARGET),
                attribute(NoticePolicyAttributes.ACTIVE_CREATOR_FOR_TARGET)),
            List.of());
    }

    private static AttributeSchema attribute(PolicyAttributeKey<?> key) {
        return new AttributeSchema(key.name(), key.type(), Set.of());
    }
}
