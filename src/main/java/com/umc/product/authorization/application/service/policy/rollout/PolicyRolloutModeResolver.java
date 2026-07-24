package com.umc.product.authorization.application.service.policy.rollout;

import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutKey;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutMode;

@FunctionalInterface
public interface PolicyRolloutModeResolver {

    PolicyRolloutMode resolve(PolicyRolloutKey key);
}
