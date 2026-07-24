package com.umc.product.blog.application.authorization;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.umc.product.authorization.domain.policy.ActionSchema;
import com.umc.product.authorization.domain.policy.AttributeSchema;
import com.umc.product.authorization.domain.policy.PolicyBundleKey;
import com.umc.product.authorization.domain.policy.PolicyDomainSchema;

public final class BlogPolicyDomainSchema {

    public static final String NAMESPACE = "blog";
    public static final String VERSION = "blog-1.0";
    public static final String POLICY_VERSION = "1.0.0";
    public static final PolicyBundleKey BUNDLE_KEY = new PolicyBundleKey(NAMESPACE, VERSION);

    private BlogPolicyDomainSchema() {
    }

    public static PolicyDomainSchema create() {
        Set<String> requiredAttributes = Set.of(
            BlogPolicyAttributes.AUTHOR.name(),
            BlogPolicyAttributes.SUPER_ADMIN.name());
        List<ActionSchema> actions = Arrays.stream(BlogPolicyAction.values())
            .map(action -> new ActionSchema(
                action.id(),
                requiredAttributes,
                Set.of(),
                Set.of()))
            .toList();
        return new PolicyDomainSchema(
            VERSION,
            actions,
            List.of(
                new AttributeSchema(
                    BlogPolicyAttributes.AUTHOR.name(),
                    BlogPolicyAttributes.AUTHOR.type(),
                    Set.of()),
                new AttributeSchema(
                    BlogPolicyAttributes.SUPER_ADMIN.name(),
                    BlogPolicyAttributes.SUPER_ADMIN.type(),
                    Set.of())),
            List.of());
    }
}
