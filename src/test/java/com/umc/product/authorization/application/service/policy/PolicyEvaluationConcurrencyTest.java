package com.umc.product.authorization.application.service.policy;

import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.compile;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.eqActive;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.request;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.requiredActive;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.statement;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authorization.application.port.in.policy.PolicyEvaluationRequest;
import com.umc.product.authorization.domain.policy.CompiledPolicyBundle;
import com.umc.product.authorization.domain.policy.PolicyEvaluationResult;

class PolicyEvaluationConcurrencyTest {

    private final PolicyEvaluationService evaluator = new PolicyEvaluationService();

    @DisplayName("같은 bundle과 context와 evaluatedAt은 concurrent evaluation에서도 같은 결과를 낸다")
    @Test
    void evaluatesConcurrentlyWithDeterministicResult() throws Exception {
        // given
        CompiledPolicyBundle bundle = compile(
                statement("zeta", "ALLOW", eqActive(true)), statement("alpha", "ALLOW", eqActive(true)));
        PolicyEvaluationRequest request = request(bundle, requiredActive(true));
        PolicyEvaluationResult expected = evaluator.evaluate(request);
        List<Callable<PolicyEvaluationResult>> tasks = new ArrayList<>();
        for (int index = 0; index < 128; index++) {
            tasks.add(() -> evaluator.evaluate(request));
        }

        // when
        List<PolicyEvaluationResult> results;
        try (var executor = Executors.newFixedThreadPool(8)) {
            List<Future<PolicyEvaluationResult>> futures = executor.invokeAll(tasks);
            results = futures.stream().map(PolicyEvaluationConcurrencyTest::get).toList();
        }

        // then
        assertThat(results).containsOnly(expected);
    }

    private static PolicyEvaluationResult get(Future<PolicyEvaluationResult> future) {
        try {
            return future.get();
        } catch (Exception exception) {
            throw new AssertionError("Concurrent policy evaluation failed", exception);
        }
    }
}
