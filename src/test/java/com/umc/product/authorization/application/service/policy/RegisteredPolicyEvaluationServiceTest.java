package com.umc.product.authorization.application.service.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authorization.application.port.in.policy.EvaluatePolicyUseCase;
import com.umc.product.authorization.application.port.in.policy.PolicyEvaluationRequest;
import com.umc.product.authorization.application.port.in.policy.RegisteredPolicyEvaluationRequest;
import com.umc.product.authorization.domain.policy.CompiledPolicyBundle;
import com.umc.product.authorization.domain.policy.PolicyAttributeSet;
import com.umc.product.authorization.domain.policy.PolicyBundleKey;
import com.umc.product.authorization.domain.policy.PolicyDecision;

class RegisteredPolicyEvaluationServiceTest {

    @Test
    @DisplayName("정확한 namespace와 context schema version의 사전 컴파일 bundle로 평가한다")
    void evaluatesPrecompiledBundleSelectedByExactKey() {
        CompiledPolicyRegistry registry = mock(CompiledPolicyRegistry.class);
        EvaluatePolicyUseCase evaluator = mock(EvaluatePolicyUseCase.class);
        CompiledPolicyBundle bundle = mock(CompiledPolicyBundle.class);
        PolicyBundleKey key = new PolicyBundleKey("term", "term-1.0");
        PolicyAttributeSet attributes = PolicyAttributeSet.builder().build();
        Instant evaluatedAt = Instant.parse("2026-07-24T00:00:00Z");
        PolicyDecision decision = mock(PolicyDecision.class);
        when(registry.require(key)).thenReturn(bundle);
        when(evaluator.evaluate(new PolicyEvaluationRequest(
            bundle,
            "term:create",
            attributes,
            evaluatedAt))).thenReturn(decision);

        var result = new RegisteredPolicyEvaluationService(registry, evaluator).evaluate(
            new RegisteredPolicyEvaluationRequest(
                key,
                "term:create",
                attributes,
                evaluatedAt));

        assertThat(result).isSameAs(decision);
        verify(registry).require(key);
    }

    @Test
    @DisplayName("등록되지 않은 bundle key는 다른 namespace 정책으로 fallback하지 않는다")
    void rejectsUnknownBundleWithoutFallback() {
        CompiledPolicyRegistry registry = mock(CompiledPolicyRegistry.class);
        EvaluatePolicyUseCase evaluator = mock(EvaluatePolicyUseCase.class);
        PolicyBundleKey key = new PolicyBundleKey("term", "term-2.0");
        when(registry.require(key)).thenThrow(new IllegalStateException("missing"));

        assertThatThrownBy(() -> new RegisteredPolicyEvaluationService(registry, evaluator).evaluate(
            new RegisteredPolicyEvaluationRequest(
                key,
                "term:create",
                PolicyAttributeSet.builder().build(),
                Instant.parse("2026-07-24T00:00:00Z"))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("missing");
    }
}
