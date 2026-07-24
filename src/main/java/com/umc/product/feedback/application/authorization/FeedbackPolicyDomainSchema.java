package com.umc.product.feedback.application.authorization;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.umc.product.authorization.domain.policy.ActionSchema;
import com.umc.product.authorization.domain.policy.AttributeSchema;
import com.umc.product.authorization.domain.policy.OutcomeMergeStrategy;
import com.umc.product.authorization.domain.policy.OutcomeSchema;
import com.umc.product.authorization.domain.policy.PolicyBundleKey;
import com.umc.product.authorization.domain.policy.PolicyDomainSchema;
import com.umc.product.authorization.domain.policy.PolicyValueType;
import com.umc.product.feedback.domain.enums.UserFeedbackTargetType;

public final class FeedbackPolicyDomainSchema {

    public static final String NAMESPACE = "feedback";
    public static final String VERSION = "feedback-1.0";
    public static final String POLICY_VERSION = "1.0.0";
    public static final PolicyBundleKey BUNDLE_KEY = new PolicyBundleKey(NAMESPACE, VERSION);

    private static final Set<String> TARGET_SYMBOLS = Arrays.stream(UserFeedbackTargetType.values())
        .map(Enum::name)
        .collect(Collectors.toUnmodifiableSet());
    private static final Set<String> COMMON_REQUIRED = Set.of(
        FeedbackPolicyAttributes.ACTIVE_CENTRAL_MEMBER.name(),
        FeedbackPolicyAttributes.ACTIVE_CHALLENGER.name(),
        FeedbackPolicyAttributes.HAS_PREVIOUS_GISU.name(),
        FeedbackPolicyAttributes.GENERATION_TEN_PLAN.name());

    private FeedbackPolicyDomainSchema() {
    }

    public static PolicyDomainSchema create() {
        return new PolicyDomainSchema(
            VERSION,
            List.of(
                new ActionSchema(
                    FeedbackPolicyAction.TEMPLATE_RESOLVE.id(),
                    COMMON_REQUIRED,
                    Set.of(FeedbackPolicyAttributes.RESOURCE_TARGET_TYPE.name()),
                    Set.of(FeedbackPolicyOutcomes.TARGET_TYPE)),
                new ActionSchema(
                    FeedbackPolicyAction.RESPONSE_SUBMIT.id(),
                    union(COMMON_REQUIRED, FeedbackPolicyAttributes.RESOURCE_TARGET_TYPE.name()),
                    Set.of(),
                    Set.of())),
            List.of(
                booleanAttribute(FeedbackPolicyAttributes.ACTIVE_CENTRAL_MEMBER.name()),
                booleanAttribute(FeedbackPolicyAttributes.ACTIVE_CHALLENGER.name()),
                booleanAttribute(FeedbackPolicyAttributes.HAS_PREVIOUS_GISU.name()),
                booleanAttribute(FeedbackPolicyAttributes.GENERATION_TEN_PLAN.name()),
                new AttributeSchema(
                    FeedbackPolicyAttributes.RESOURCE_TARGET_TYPE.name(),
                    PolicyValueType.ENUM,
                    TARGET_SYMBOLS)),
            List.of(new OutcomeSchema(
                FeedbackPolicyOutcomes.TARGET_TYPE,
                PolicyValueType.ENUM,
                OutcomeMergeStrategy.DOMINANCE,
                List.of("ADMIN", "EXPERIENCED_CHALLENGER", "NEW_CHALLENGER"))));
    }

    private static AttributeSchema booleanAttribute(String name) {
        return new AttributeSchema(name, PolicyValueType.BOOLEAN, Set.of());
    }

    private static Set<String> union(Set<String> values, String additional) {
        java.util.HashSet<String> result = new java.util.HashSet<>(values);
        result.add(additional);
        return Set.copyOf(result);
    }
}
