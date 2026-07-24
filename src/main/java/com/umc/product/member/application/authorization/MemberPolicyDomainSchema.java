package com.umc.product.member.application.authorization;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.umc.product.authorization.domain.policy.ActionSchema;
import com.umc.product.authorization.domain.policy.AttributeSchema;
import com.umc.product.authorization.domain.policy.PolicyBundleKey;
import com.umc.product.authorization.domain.policy.PolicyDomainSchema;

public final class MemberPolicyDomainSchema {

    public static final String NAMESPACE = "member";
    public static final String VERSION = "member-1.0";
    public static final String POLICY_VERSION = "1.0.0";
    public static final PolicyBundleKey BUNDLE_KEY = new PolicyBundleKey(NAMESPACE, VERSION);

    private MemberPolicyDomainSchema() {
    }

    public static PolicyDomainSchema create() {
        Set<String> required = Set.of(
            MemberPolicyAttributes.CHALLENGER.name(),
            MemberPolicyAttributes.ACTIVE_CENTRAL_CORE.name());
        return new PolicyDomainSchema(
            VERSION,
            Arrays.stream(MemberPolicyAction.values())
                .map(action -> new ActionSchema(action.id(), required, Set.of(), Set.of()))
                .toList(),
            List.of(
                new AttributeSchema(
                    MemberPolicyAttributes.CHALLENGER.name(),
                    MemberPolicyAttributes.CHALLENGER.type(),
                    Set.of()),
                new AttributeSchema(
                    MemberPolicyAttributes.ACTIVE_CENTRAL_CORE.name(),
                    MemberPolicyAttributes.ACTIVE_CENTRAL_CORE.type(),
                    Set.of())),
            List.of());
    }
}
