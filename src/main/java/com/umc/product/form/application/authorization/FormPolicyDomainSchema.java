package com.umc.product.form.application.authorization;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.umc.product.authorization.domain.policy.ActionSchema;
import com.umc.product.authorization.domain.policy.AttributeSchema;
import com.umc.product.authorization.domain.policy.PolicyBundleKey;
import com.umc.product.authorization.domain.policy.PolicyDomainSchema;
import com.umc.product.authorization.domain.policy.PolicyValueType;

public final class FormPolicyDomainSchema {

    public static final String NAMESPACE = "form";
    public static final String VERSION = "form-1.0";
    public static final String POLICY_VERSION = "1.0.0";
    public static final PolicyBundleKey BUNDLE_KEY = new PolicyBundleKey(NAMESPACE, VERSION);

    private FormPolicyDomainSchema() {
    }

    public static PolicyDomainSchema create() {
        Set<String> required = Set.of(
            FormPolicyAttributes.SUBJECT_KIND.name(),
            FormPolicyAttributes.USAGE_OWNER_AUTHORIZED.name(),
            FormPolicyAttributes.CONSUMER_AUTHORIZED.name(),
            FormPolicyAttributes.IS_RESPONDENT.name(),
            FormPolicyAttributes.CAPABILITY_BOUND_TO_RESPONSE.name());
        return new PolicyDomainSchema(
            VERSION,
            Arrays.stream(FormPolicyAction.values())
                .map(action -> new ActionSchema(action.id(), required, Set.of(), Set.of()))
                .toList(),
            List.of(
                new AttributeSchema(
                    FormPolicyAttributes.SUBJECT_KIND.name(),
                    PolicyValueType.ENUM,
                    Set.of("MEMBER", "CAPABILITY")),
                booleanAttribute(FormPolicyAttributes.USAGE_OWNER_AUTHORIZED.name()),
                booleanAttribute(FormPolicyAttributes.CONSUMER_AUTHORIZED.name()),
                booleanAttribute(FormPolicyAttributes.IS_RESPONDENT.name()),
                booleanAttribute(FormPolicyAttributes.CAPABILITY_BOUND_TO_RESPONSE.name())),
            List.of());
    }

    private static AttributeSchema booleanAttribute(String name) {
        return new AttributeSchema(name, PolicyValueType.BOOLEAN, Set.of());
    }
}
