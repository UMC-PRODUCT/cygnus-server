package com.umc.product.project.application.authorization;

import java.util.Set;

import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.authorization.domain.exception.AuthorizationErrorCode;
import com.umc.product.authorization.domain.policy.PolicyDecision;
import com.umc.product.authorization.domain.policy.PolicyValue;

public final class ProjectPolicyDecisionOutcomes {

    private ProjectPolicyDecisionOutcomes() {}

    public static boolean booleanValue(PolicyDecision decision, String key) {
        return decision.outcome(key)
            .map(value -> requireType(value, PolicyValue.BooleanValue.class))
            .map(PolicyValue.BooleanValue::value)
            .orElse(false);
    }

    public static Set<Long> longSetValue(PolicyDecision decision, String key) {
        return decision.outcome(key)
            .map(value -> requireType(value, PolicyValue.LongSetValue.class))
            .map(PolicyValue.LongSetValue::value)
            .orElseGet(Set::of);
    }

    public static String enumValue(PolicyDecision decision, String key, String defaultValue) {
        return decision.outcome(key)
            .map(value -> requireType(value, PolicyValue.EnumValue.class))
            .map(PolicyValue.EnumValue::value)
            .orElse(defaultValue);
    }

    private static <T extends PolicyValue> T requireType(PolicyValue value, Class<T> type) {
        if (!type.isInstance(value)) {
            throw new AuthorizationDomainException(AuthorizationErrorCode.POLICY_EVALUATION_FAILED);
        }
        return type.cast(value);
    }
}
