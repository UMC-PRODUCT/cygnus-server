package com.umc.product.authorization.application.service.policy.rollout;

@FunctionalInterface
public interface PolicyExpectedDifference<C, D> {

    boolean matches(C context, D legacyDecision, D targetDecision);

    static <C, D> PolicyExpectedDifference<C, D> none() {
        return (context, legacy, target) -> false;
    }
}
