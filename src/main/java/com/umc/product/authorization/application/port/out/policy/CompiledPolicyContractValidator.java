package com.umc.product.authorization.application.port.out.policy;

import com.umc.product.authorization.domain.policy.CompiledPolicyBundle;

@FunctionalInterface
public interface CompiledPolicyContractValidator {

    CompiledPolicyContractValidator NO_OP = bundle -> {
    };

    void validate(CompiledPolicyBundle bundle);
}
