package com.umc.product.authorization.application.service.policy;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import com.umc.product.authorization.application.port.in.policy.PolicyEvaluationRequest;
import com.umc.product.authorization.domain.policy.ActionSchema;
import com.umc.product.authorization.domain.policy.AttributeSchema;
import com.umc.product.authorization.domain.policy.CompiledPolicyBundle;
import com.umc.product.authorization.domain.policy.CompiledPolicyModule;
import com.umc.product.authorization.domain.policy.CompiledPolicyOutcome;
import com.umc.product.authorization.domain.policy.CompiledPolicyStatement;
import com.umc.product.authorization.domain.policy.OutcomeMergeStrategy;
import com.umc.product.authorization.domain.policy.OutcomeSchema;
import com.umc.product.authorization.domain.policy.PolicyAttributeKey;
import com.umc.product.authorization.domain.policy.PolicyAttributeSet;
import com.umc.product.authorization.domain.policy.PolicyCombiningAlgorithm;
import com.umc.product.authorization.domain.policy.PolicyCondition;
import com.umc.product.authorization.domain.policy.PolicyDecision;
import com.umc.product.authorization.domain.policy.PolicyDomainSchema;
import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.authorization.domain.policy.PolicyEvaluationFailure;
import com.umc.product.authorization.domain.policy.PolicyEvaluationFailureCode;
import com.umc.product.authorization.domain.policy.PolicyOperand;
import com.umc.product.authorization.domain.policy.PolicyOperator;
import com.umc.product.authorization.domain.policy.PolicyValue;
import com.umc.product.authorization.domain.policy.PolicyValueType;

public final class PolicyEvaluationManualDriver {

    private static final String ACTION = "manual:evaluate";
    private static final String ACTIVE = "subject.active";
    private static final String OUTCOME = "manual.force";
    private static final Instant EVALUATED_AT = Instant.parse("2026-07-13T03:00:00Z");
    private static final PolicyAttributeKey<PolicyValue.BooleanValue> ACTIVE_KEY =
            new PolicyAttributeKey<>(ACTIVE, PolicyValueType.BOOLEAN);

    private PolicyEvaluationManualDriver() {}

    public static void main(String[] args) {
        PolicyEvaluationService evaluator = new PolicyEvaluationService();
        CompiledPolicyBundle bundle = bundle();

        PolicyDecision allowed = (PolicyDecision) evaluator.evaluate(request(bundle, attributes(true)));
        PolicyDecision denied = (PolicyDecision) evaluator.evaluate(request(bundle, attributes(false)));
        PolicyEvaluationFailure missing = (PolicyEvaluationFailure)
                evaluator.evaluate(request(bundle, PolicyAttributeSet.builder().build()));

        require(allowed.effect() == PolicyEffect.ALLOW, "allow decision mismatch");
        require(allowed.matchedAllowStatementIds().equals(List.of("manual.allow")), "matched ID mismatch");
        require(
                allowed.outcome(OUTCOME).equals(java.util.Optional.of(new PolicyValue.BooleanValue(true))),
                "outcome mismatch");
        require(denied.effect() == PolicyEffect.DENY, "default deny mismatch");
        require(
                missing.failureCode() == PolicyEvaluationFailureCode.REQUIRED_ATTRIBUTE_MISSING,
                "required failure mismatch");
        require(allowed.evaluatedAt().equals(EVALUATED_AT), "evaluatedAt mismatch");

        System.out.println("MANUAL_POLICY_EVALUATION allow=ALLOW matched=manual.allow outcome=true "
                + "default=DENY missing=REQUIRED_ATTRIBUTE_MISSING evaluatedAt="
                + EVALUATED_AT
                + " fingerprintPrefix=aaaaaaaa");
    }

    private static PolicyEvaluationRequest request(
            CompiledPolicyBundle bundle, PolicyAttributeSet attributes) {
        return new PolicyEvaluationRequest(bundle, ACTION, attributes, EVALUATED_AT);
    }

    private static PolicyAttributeSet attributes(boolean active) {
        return PolicyAttributeSet.builder()
                .put(ACTIVE_KEY, new PolicyValue.BooleanValue(active))
                .build();
    }

    private static CompiledPolicyBundle bundle() {
        PolicyDomainSchema schema = new PolicyDomainSchema(
                "manual-1.0",
                List.of(new ActionSchema(ACTION, Set.of(ACTIVE), Set.of(), Set.of(OUTCOME))),
                List.of(new AttributeSchema(ACTIVE, PolicyValueType.BOOLEAN, Set.of())),
                List.of(new OutcomeSchema(
                        OUTCOME,
                        PolicyValueType.BOOLEAN,
                        OutcomeMergeStrategy.BOOLEAN_OR,
                        List.of())));
        PolicyCondition condition = new PolicyCondition.Predicate(
                PolicyOperator.EQ,
                List.of(
                        new PolicyOperand.Attribute(ACTIVE, PolicyValueType.BOOLEAN),
                        new PolicyOperand.Literal(new PolicyValue.BooleanValue(true))));
        CompiledPolicyStatement statement = new CompiledPolicyStatement(
                "manual.allow",
                List.of(ACTION),
                PolicyEffect.ALLOW,
                condition,
                List.of(new CompiledPolicyOutcome(
                        OUTCOME, new PolicyOperand.Literal(new PolicyValue.BooleanValue(true)))));
        return new CompiledPolicyBundle(
                "1.0",
                "manual-1.0",
                "manual",
                "1.0.0",
                PolicyEffect.DENY,
                PolicyCombiningAlgorithm.DENY_OVERRIDES,
                schema,
                "a".repeat(64),
                List.of(new CompiledPolicyModule("manual", "manual.policy.json", List.of(statement))));
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
