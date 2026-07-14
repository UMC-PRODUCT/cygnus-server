package com.umc.product.authorization.application.service.policy;

import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.attribute;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.compile;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.eqActive;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.literal;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.outcome;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.predicate;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.statement;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.statementWithActions;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authorization.domain.policy.CompiledPolicyBundle;

class PolicyFingerprintTest {

    private static final String GOLDEN_FINGERPRINT =
            "e6d409e70d19570ff1c62c50797821dc85cf0e18ba37a49d8658b27734f38b57";

    @DisplayName("canonical compiled AST는 고정된 SHA-256 fingerprint를 가진다")
    @Test
    void producesGoldenCanonicalFingerprint() {
        // given
        CompiledPolicyBundle bundle = compile(statement("allow-active", "ALLOW", eqActive(true)));

        // when
        String fingerprint = bundle.policyFingerprint();

        // then
        assertThat(fingerprint).isEqualTo(GOLDEN_FINGERPRINT);
    }

    @DisplayName("statement 순서를 바꿔도 fingerprint와 canonical AST가 같다")
    @Test
    void fingerprintIsIndependentOfStatementOrder() {
        // given
        String alpha = statement("alpha", "ALLOW", eqActive(true));
        String zeta = statement("zeta", "DENY", eqActive(false));

        // when
        CompiledPolicyBundle first = compile(zeta, alpha);
        CompiledPolicyBundle second = compile(alpha, zeta);

        // then
        assertThat(first.policyFingerprint()).isEqualTo(second.policyFingerprint());
        assertThat(first.modules().getFirst().statements())
                .extracting(policyStatement -> policyStatement.id())
                .containsExactly("alpha", "zeta");
        assertThat(second.modules().getFirst().statements()).isEqualTo(first.modules().getFirst().statements());
    }

    @DisplayName("compiled AST의 의미가 바뀌면 fingerprint도 바뀐다")
    @Test
    void fingerprintChangesForSemanticChange() {
        // when
        CompiledPolicyBundle truePolicy = compile(statement("allow-active", "ALLOW", eqActive(true)));
        CompiledPolicyBundle falsePolicy = compile(statement("allow-active", "ALLOW", eqActive(false)));

        // then
        assertThat(truePolicy.policyFingerprint()).isNotEqualTo(falsePolicy.policyFingerprint());
    }

    @DisplayName("action, outcome, set, ALL child 순서는 fingerprint에 영향을 주지 않는다")
    @Test
    void canonicalizesNestedCommutativeOrder() {
        // given
        String active = eqActive(true);
        String missingName = predicate("NOT_EXISTS", attribute("optional.name"));
        String firstCondition = all(active, missingName);
        String secondCondition = all(missingName, active);
        String firstOutcomes = "["
                + outcome("outcome.exactly", literal("STRING", "\"same\""))
                + ","
                + outcome("outcome.longs", literal("LONG_SET", "[3,1]"))
                + "]";
        String secondOutcomes = "["
                + outcome("outcome.longs", literal("LONG_SET", "[1,3]"))
                + ","
                + outcome("outcome.exactly", literal("STRING", "\"same\""))
                + "]";

        // when
        CompiledPolicyBundle first = compile(statementWithActions(
                "allow",
                "ALLOW",
                "[\"test:other\",\"test:evaluate\"]",
                firstCondition,
                firstOutcomes));
        CompiledPolicyBundle second = compile(statementWithActions(
                "allow",
                "ALLOW",
                "[\"test:evaluate\",\"test:other\"]",
                secondCondition,
                secondOutcomes));

        // then
        assertThat(first.policyFingerprint()).isEqualTo(second.policyFingerprint());
        assertThat(first.modules()).isEqualTo(second.modules());
    }

    private String all(String first, String second) {
        return """
                {"type":"ALL","conditions":[%s,%s]}
                """
                .formatted(first, second);
    }
}
