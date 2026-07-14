package com.umc.product.authorization.domain.policy;

public enum OutcomeMergeStrategy {
    BOOLEAN_OR,
    SET_UNION,
    DOMINANCE,
    EXACTLY_ONE
}
