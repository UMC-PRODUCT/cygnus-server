package com.umc.product.authorization.application.service.policy;

import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.ACTION;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.EVALUATED_AT;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.OPTIONAL_FLAG;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.REQUIRED_ACTIVE;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.attribute;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.compile;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.eqActive;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.outcome;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.request;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.requiredActive;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.statement;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authorization.application.port.in.policy.PolicyEvaluationRequest;
import com.umc.product.authorization.domain.policy.CompiledPolicyBundle;
import com.umc.product.authorization.domain.policy.PolicyAttributeKey;
import com.umc.product.authorization.domain.policy.PolicyAttributeSet;
import com.umc.product.authorization.domain.policy.PolicyDecision;
import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.authorization.domain.policy.PolicyEvaluationFailure;
import com.umc.product.authorization.domain.policy.PolicyEvaluationFailureCode;
import com.umc.product.authorization.domain.policy.PolicyValue;
import com.umc.product.authorization.domain.policy.PolicyValueType;

class PolicyEvaluationServiceTest {

    private final PolicyEvaluationService evaluator = new PolicyEvaluationService();

    @DisplayName("ALLOW가 매칭되면 허용하고 아무 statement도 매칭되지 않으면 기본 거부한다")
    @Test
    void appliesAllowAndDefaultDeny() {
        // given
        CompiledPolicyBundle bundle = compile(statement("allow-active", "ALLOW", eqActive(true)));

        // when
        PolicyDecision allowed = (PolicyDecision) evaluator.evaluate(request(bundle, requiredActive(true)));
        PolicyDecision denied = (PolicyDecision) evaluator.evaluate(request(bundle, requiredActive(false)));

        // then
        assertThat(allowed.effect()).isEqualTo(PolicyEffect.ALLOW);
        assertThat(allowed.matchedAllowStatementIds()).containsExactly("allow-active");
        assertThat(denied.effect()).isEqualTo(PolicyEffect.DENY);
        assertThat(denied.matchedAllowStatementIds()).isEmpty();
        assertThat(denied.matchedDenyStatementIds()).isEmpty();
    }

    @DisplayName("모든 candidate statement를 평가하고 매칭 ID를 정렬한다")
    @Test
    void evaluatesAllCandidatesAndSortsMatchedIds() {
        // given
        CompiledPolicyBundle bundle = compile(
                statement("zeta", "ALLOW", eqActive(true)), statement("alpha", "ALLOW", eqActive(true)));

        // when
        PolicyDecision decision = (PolicyDecision) evaluator.evaluate(request(bundle, requiredActive(true)));

        // then
        assertThat(decision.matchedAllowStatementIds()).containsExactly("alpha", "zeta");
    }

    @DisplayName("DENY가 매칭되면 ALLOW outcome을 해석하지 않고 모두 폐기한다")
    @Test
    void denyOverridesAndSkipsOutcomeResolution() {
        // given
        String missingAttributeOutcome = "[" + outcome("outcome.attributeLongs", attribute("optional.longs")) + "]";
        CompiledPolicyBundle bundle = compile(
                statement("allow", "ALLOW", eqActive(true), missingAttributeOutcome),
                statement("deny", "DENY", eqActive(true)));

        // when
        PolicyDecision decision = (PolicyDecision) evaluator.evaluate(request(bundle, requiredActive(true)));

        // then
        assertThat(decision.effect()).isEqualTo(PolicyEffect.DENY);
        assertThat(decision.matchedAllowStatementIds()).containsExactly("allow");
        assertThat(decision.matchedDenyStatementIds()).containsExactly("deny");
        assertThat(decision.outcomes()).isEmpty();
    }

    @DisplayName("등록되지 않은 runtime action은 evaluation failure다")
    @Test
    void failsForUnregisteredAction() {
        // given
        CompiledPolicyBundle bundle = compile(statement("allow", "ALLOW", eqActive(true)));
        PolicyEvaluationRequest request =
                new PolicyEvaluationRequest(bundle, "test:unknown", requiredActive(true), EVALUATED_AT);

        // when
        PolicyEvaluationFailure failure = (PolicyEvaluationFailure) evaluator.evaluate(request);

        // then
        assertThat(failure.failureCode()).isEqualTo(PolicyEvaluationFailureCode.ACTION_NOT_REGISTERED);
        assertThat(failure.statementId()).isEmpty();
        assertThat(failure.attributeName()).isEmpty();
    }

    @DisplayName("action의 required attribute가 누락되면 evaluation failure다")
    @Test
    void failsForMissingRequiredAttribute() {
        // given
        CompiledPolicyBundle bundle = compile(statement("allow", "ALLOW", eqActive(true)));

        // when
        PolicyEvaluationFailure failure = (PolicyEvaluationFailure)
                evaluator.evaluate(request(bundle, PolicyAttributeSet.builder().build()));

        // then
        assertThat(failure.failureCode()).isEqualTo(PolicyEvaluationFailureCode.REQUIRED_ATTRIBUTE_MISSING);
        assertThat(failure.attributeName()).contains("required.active");
    }

    @DisplayName("runtime attribute 타입이 schema와 다르면 evaluation failure다")
    @Test
    void failsForRuntimeAttributeTypeMismatch() {
        // given
        CompiledPolicyBundle bundle = compile(statement("allow", "ALLOW", eqActive(true)));
        PolicyAttributeKey<PolicyValue.LongValue> wrongKey =
                new PolicyAttributeKey<>("required.active", PolicyValueType.LONG);
        PolicyAttributeSet attributes =
                PolicyAttributeSet.builder().put(wrongKey, new PolicyValue.LongValue(1L)).build();

        // when
        PolicyEvaluationFailure failure = (PolicyEvaluationFailure) evaluator.evaluate(request(bundle, attributes));

        // then
        assertThat(failure.failureCode()).isEqualTo(PolicyEvaluationFailureCode.ATTRIBUTE_TYPE_MISMATCH);
        assertThat(failure.attributeName()).contains("required.active");
    }

    @DisplayName("등록되지 않은 runtime attribute는 evaluation failure다")
    @Test
    void failsForUnregisteredRuntimeAttribute() {
        // given
        CompiledPolicyBundle bundle = compile(statement("allow", "ALLOW", eqActive(true)));
        PolicyAttributeKey<PolicyValue.BooleanValue> unknown =
                new PolicyAttributeKey<>("runtime.unknown", PolicyValueType.BOOLEAN);
        PolicyAttributeSet attributes = PolicyAttributeSet.builder()
                .put(REQUIRED_ACTIVE, new PolicyValue.BooleanValue(true))
                .put(unknown, new PolicyValue.BooleanValue(true))
                .build();

        // when
        PolicyEvaluationFailure failure = (PolicyEvaluationFailure) evaluator.evaluate(request(bundle, attributes));

        // then
        assertThat(failure.failureCode()).isEqualTo(PolicyEvaluationFailureCode.ATTRIBUTE_NOT_REGISTERED);
        assertThat(failure.attributeName()).contains("runtime.unknown");
    }

    @DisplayName("action schema에 등록된 추가 optional attribute는 statement가 참조하지 않아도 허용한다")
    @Test
    void acceptsRegisteredExtraOptionalAttribute() {
        // given
        CompiledPolicyBundle bundle = compile(statement("allow", "ALLOW", eqActive(true)));
        PolicyAttributeSet attributes = PolicyAttributeSet.builder()
                .put(REQUIRED_ACTIVE, new PolicyValue.BooleanValue(true))
                .put(OPTIONAL_FLAG, new PolicyValue.BooleanValue(false))
                .build();

        // when
        PolicyDecision decision = (PolicyDecision) evaluator.evaluate(request(bundle, attributes));

        // then
        assertThat(decision.effect()).isEqualTo(PolicyEffect.ALLOW);
    }

    @DisplayName("decision과 failure는 같은 evaluatedAt과 정책 버전 식별자를 반환한다")
    @Test
    void returnsEvaluationMetadataForDecisionAndFailure() {
        // given
        CompiledPolicyBundle bundle = compile(statement("allow", "ALLOW", eqActive(true)));

        // when
        PolicyDecision decision = (PolicyDecision) evaluator.evaluate(request(bundle, requiredActive(true)));
        PolicyEvaluationFailure failure = (PolicyEvaluationFailure) evaluator.evaluate(
                new PolicyEvaluationRequest(bundle, "test:unknown", requiredActive(true), EVALUATED_AT));

        // then
        assertMetadata(decision, bundle);
        assertThat(failure.evaluatedAt()).isEqualTo(EVALUATED_AT);
        assertThat(failure.schemaVersion()).isEqualTo(bundle.schemaVersion());
        assertThat(failure.contextSchemaVersion()).isEqualTo(bundle.contextSchemaVersion());
        assertThat(failure.policyVersion()).isEqualTo(bundle.policyVersion());
        assertThat(failure.policyFingerprint()).isEqualTo(bundle.policyFingerprint());
        assertThat(failure.outcomeKey()).isEqualTo(Optional.empty());
    }

    private void assertMetadata(PolicyDecision decision, CompiledPolicyBundle bundle) {
        assertThat(decision.evaluatedAt()).isEqualTo(EVALUATED_AT);
        assertThat(decision.schemaVersion()).isEqualTo(bundle.schemaVersion());
        assertThat(decision.contextSchemaVersion()).isEqualTo(bundle.contextSchemaVersion());
        assertThat(decision.policyVersion()).isEqualTo(bundle.policyVersion());
        assertThat(decision.policyFingerprint()).isEqualTo(bundle.policyFingerprint());
        assertThat(ACTION).isEqualTo("test:evaluate");
    }
}
