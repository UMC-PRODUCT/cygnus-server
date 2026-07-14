package com.umc.product.project.application.authorization;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authorization.application.port.in.policy.PolicyEvaluationRequest;
import com.umc.product.authorization.application.service.policy.PolicyEvaluationService;
import com.umc.product.authorization.application.service.policy.PolicySemanticCompiler;
import com.umc.product.authorization.domain.policy.CompiledPolicyBundle;
import com.umc.product.authorization.domain.policy.CompiledPolicyModule;
import com.umc.product.authorization.domain.policy.CompiledPolicyOutcome;
import com.umc.product.authorization.domain.policy.CompiledPolicyStatement;
import com.umc.product.authorization.domain.policy.PolicyAttributeSet;
import com.umc.product.authorization.domain.policy.PolicyCondition;
import com.umc.product.authorization.domain.policy.PolicyDecision;
import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.authorization.domain.policy.PolicyOperand;
import com.umc.product.authorization.domain.policy.PolicyValue;

class ProjectTargetStatementCoverageTest {

    private static final Instant EVALUATED_AT = Instant.parse("2026-07-13T00:00:00Z");
    private static final int EXPECTED_STATEMENTS = 89;
    private static final int EXPECTED_ACTION_BINDINGS = 130;
    private static final int EXPECTED_LEAVES = 219;
    private static final int EXPECTED_ANY_NODES = 1;
    private static final int EXPECTED_LEAF_ACTION_MUTATIONS = 328;

    private final PolicyEvaluationService evaluator = new PolicyEvaluationService();

    @Test
    @DisplayName("Target 89개 statement의 모든 action과 219개 condition leaf를 수용·반증한다")
    void verifiesEveryStatementActionAndConditionLeaf() {
        CompiledPolicyBundle target =
            new ProjectPolicyBundleLoader(new PolicySemanticCompiler()).compiled().value();
        ProjectPolicyStatementWitnessFactory witnesses =
            new ProjectPolicyStatementWitnessFactory(target.domainSchema());
        List<StatementInModule> statements = target.modules().stream()
            .flatMap(module -> module.statements().stream().map(statement -> new StatementInModule(module, statement)))
            .toList();
        int actionBindings = 0;
        int leafCount = 0;
        int anyNodeCount = 0;
        int verifiedMutations = 0;
        int unverifiedLeaves = 0;
        List<String> unverifiedLeafIds = new java.util.ArrayList<>();

        for (StatementInModule entry : statements) {
            CompiledPolicyStatement statement = entry.statement();
            List<PolicyCondition.Predicate> leaves = witnesses.leaves(statement.condition());
            leafCount += leaves.size();
            anyNodeCount += witnesses.anyNodeCount(statement.condition());
            assertThat(statement.effect()).as(statement.id()).isEqualTo(PolicyEffect.ALLOW);

            for (String actionId : statement.actions()) {
                actionBindings++;
                PolicyAttributeSet matching = witnesses.matching(actionId, statement.condition()).orElseThrow();
                assertAllows(singleStatementBundle(target, entry), actionId, matching, statement.id());
                assertAttributeOutcomesAreNonEmptySets(statement, matching);

                for (PolicyCondition.Predicate leaf : leaves) {
                    var mutation = witnesses.mutation(actionId, statement.condition(), leaf);
                    if (mutation.isEmpty()) {
                        unverifiedLeaves++;
                        unverifiedLeafIds.add(statement.id() + " / " + actionId + " / " + leaf);
                        continue;
                    }
                    assertAllows(singleStatementBundle(target, entry), actionId,
                        mutation.orElseThrow().matching(), statement.id());
                    assertDefaultDenies(singleStatementBundle(target, entry), actionId,
                        mutation.orElseThrow().mutated(), statement.id());
                    verifiedMutations++;
                }
            }
        }

        assertThat(statements).hasSize(EXPECTED_STATEMENTS);
        assertThat(actionBindings).isEqualTo(EXPECTED_ACTION_BINDINGS);
        assertThat(leafCount).isEqualTo(EXPECTED_LEAVES);
        assertThat(anyNodeCount).isEqualTo(EXPECTED_ANY_NODES);
        assertThat(verifiedMutations)
            .as(unverifiedLeafIds.toString())
            .isEqualTo(EXPECTED_LEAF_ACTION_MUTATIONS);
        assertThat(unverifiedLeafIds).isEmpty();
        assertThat(unverifiedLeaves).isZero();
        System.out.printf(
            "PROJECT_TARGET_STATEMENT_COVERAGE statements=%d actionBindings=%d leaves=%d "
                + "anyNodes=%d verifiedLeafActionMutations=%d unverified=%d%n",
            statements.size(), actionBindings, leafCount, anyNodeCount, verifiedMutations, unverifiedLeaves);
    }

    private void assertAllows(
        CompiledPolicyBundle bundle,
        String actionId,
        PolicyAttributeSet attributes,
        String statementId
    ) {
        var result = evaluator.evaluate(new PolicyEvaluationRequest(bundle, actionId, attributes, EVALUATED_AT));
        assertThat(result).as(statementId + " / " + actionId).isInstanceOf(PolicyDecision.class);
        PolicyDecision decision = (PolicyDecision) result;
        assertThat(decision.effect()).as(statementId + " / " + actionId).isEqualTo(PolicyEffect.ALLOW);
        assertThat(decision.matchedAllowStatementIds()).containsExactly(statementId);
    }

    private void assertDefaultDenies(
        CompiledPolicyBundle bundle,
        String actionId,
        PolicyAttributeSet attributes,
        String statementId
    ) {
        var result = evaluator.evaluate(new PolicyEvaluationRequest(bundle, actionId, attributes, EVALUATED_AT));
        assertThat(result).as(statementId + " / " + actionId).isInstanceOf(PolicyDecision.class);
        PolicyDecision decision = (PolicyDecision) result;
        assertThat(decision.effect()).as(statementId + " / " + actionId).isEqualTo(PolicyEffect.DENY);
        assertThat(decision.matchedAllowStatementIds()).isEmpty();
        assertThat(decision.matchedDenyStatementIds()).isEmpty();
    }

    private void assertAttributeOutcomesAreNonEmptySets(
        CompiledPolicyStatement statement,
        PolicyAttributeSet attributes
    ) {
        for (CompiledPolicyOutcome outcome : statement.outcomes()) {
            if (!(outcome.value() instanceof PolicyOperand.Attribute attribute)) {
                continue;
            }
            PolicyValue value = attributes.value(attribute.name()).orElseThrow();
            assertThat(setSize(value)).as(statement.id() + " / " + outcome.key()).isPositive();
        }
    }

    private int setSize(PolicyValue value) {
        if (value instanceof PolicyValue.LongSetValue values) {
            return values.value().size();
        }
        if (value instanceof PolicyValue.StringSetValue values) {
            return values.value().size();
        }
        if (value instanceof PolicyValue.EnumSetValue values) {
            return values.value().size();
        }
        if (value instanceof PolicyValue.InstantSetValue values) {
            return values.value().size();
        }
        return 0;
    }

    private CompiledPolicyBundle singleStatementBundle(CompiledPolicyBundle source, StatementInModule entry) {
        CompiledPolicyModule module = entry.module();
        return new CompiledPolicyBundle(
            source.schemaVersion(), source.contextSchemaVersion(), source.namespace(), source.policyVersion(),
            source.defaultEffect(), source.combiningAlgorithm(), source.domainSchema(), source.policyFingerprint(),
            List.of(new CompiledPolicyModule(module.id(), module.filename(), List.of(entry.statement()))));
    }

    private record StatementInModule(CompiledPolicyModule module, CompiledPolicyStatement statement) {}
}
