package com.umc.product.authorization.application.service.policy.rollout;

import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutEvaluation;

@FunctionalInterface
public interface PolicyRolloutEvaluator<C, D> {

    PolicyRolloutEvaluation<D> evaluate(C context);
}
