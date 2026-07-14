package com.umc.product.authorization.application.port.in.policy;

import com.umc.product.authorization.domain.policy.CompiledPolicyBundle;

public interface CompilePolicyBundleUseCase {
    CompiledPolicyBundle compile(PolicyBundleCompilationRequest request);
}
