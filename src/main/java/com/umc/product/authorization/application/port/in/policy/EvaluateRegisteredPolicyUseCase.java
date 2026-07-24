package com.umc.product.authorization.application.port.in.policy;

import com.umc.product.authorization.domain.policy.PolicyEvaluationResult;

public interface EvaluateRegisteredPolicyUseCase {

    PolicyEvaluationResult evaluate(RegisteredPolicyEvaluationRequest request);
}
