package com.umc.product.schedule.application.authorization;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.umc.product.authorization.domain.policy.ActionSchema;
import com.umc.product.authorization.domain.policy.AttributeSchema;
import com.umc.product.authorization.domain.policy.PolicyAttributeKey;
import com.umc.product.authorization.domain.policy.PolicyBundleKey;
import com.umc.product.authorization.domain.policy.PolicyDomainSchema;

public final class SchedulePolicyDomainSchema {

    public static final String NAMESPACE = "schedule";
    public static final String VERSION = "schedule-1.0";
    public static final String POLICY_VERSION = "1.0.0";
    public static final PolicyBundleKey BUNDLE_KEY = new PolicyBundleKey(NAMESPACE, VERSION);

    private SchedulePolicyDomainSchema() {
    }

    public static PolicyDomainSchema create() {
        Set<String> required = Set.of(
            SchedulePolicyAttributes.SUPER_ADMIN.name(),
            SchedulePolicyAttributes.CHALLENGER_HISTORY.name(),
            SchedulePolicyAttributes.AUTHOR.name(),
            SchedulePolicyAttributes.PARTICIPANT.name(),
            SchedulePolicyAttributes.RESOURCE_SPECIFIED.name(),
            SchedulePolicyAttributes.ACTIVE_OPERATING_STAFF.name(),
            SchedulePolicyAttributes.ACTIVE_TARGET_GISU_STAFF.name());
        return new PolicyDomainSchema(
            VERSION,
            Arrays.stream(SchedulePolicyAction.values())
                .map(action -> new ActionSchema(action.id(), required, Set.of(), Set.of()))
                .toList(),
            List.of(
                attribute(SchedulePolicyAttributes.SUPER_ADMIN),
                attribute(SchedulePolicyAttributes.CHALLENGER_HISTORY),
                attribute(SchedulePolicyAttributes.AUTHOR),
                attribute(SchedulePolicyAttributes.PARTICIPANT),
                attribute(SchedulePolicyAttributes.RESOURCE_SPECIFIED),
                attribute(SchedulePolicyAttributes.ACTIVE_OPERATING_STAFF),
                attribute(SchedulePolicyAttributes.ACTIVE_TARGET_GISU_STAFF)),
            List.of());
    }

    private static AttributeSchema attribute(PolicyAttributeKey<?> key) {
        return new AttributeSchema(key.name(), key.type(), Set.of());
    }
}
