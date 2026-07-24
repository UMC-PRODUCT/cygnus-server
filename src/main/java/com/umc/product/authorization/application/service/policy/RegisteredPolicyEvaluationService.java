package com.umc.product.authorization.application.service.policy;

import org.springframework.stereotype.Service;

import com.umc.product.authorization.application.port.in.policy.EvaluatePolicyUseCase;
import com.umc.product.authorization.application.port.in.policy.EvaluateRegisteredPolicyUseCase;
import com.umc.product.authorization.application.port.in.policy.PolicyEvaluationRequest;
import com.umc.product.authorization.application.port.in.policy.RegisteredPolicyEvaluationRequest;
import com.umc.product.authorization.domain.policy.CompiledPolicyBundle;
import com.umc.product.authorization.domain.policy.PolicyEvaluationResult;

@Service
public class RegisteredPolicyEvaluationService implements EvaluateRegisteredPolicyUseCase {

    private final CompiledPolicyRegistry registry;
    private final EvaluatePolicyUseCase evaluator;

    public RegisteredPolicyEvaluationService(
        CompiledPolicyRegistry registry,
        EvaluatePolicyUseCase evaluator
    ) {
        this.registry = registry;
        this.evaluator = evaluator;
    }

    @Override
    public PolicyEvaluationResult evaluate(RegisteredPolicyEvaluationRequest request) {
        CompiledPolicyBundle bundle = registry.require(request.bundleKey());
        return evaluator.evaluate(new PolicyEvaluationRequest(
            bundle,
            request.actionId(),
            request.attributes(),
            request.evaluatedAt()));
    }
}
