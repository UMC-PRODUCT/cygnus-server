package com.umc.product.authorization.application.service.policy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.umc.product.authorization.domain.policy.CompiledPolicyModule;
import com.umc.product.authorization.domain.policy.CompiledPolicyOutcome;
import com.umc.product.authorization.domain.policy.CompiledPolicyStatement;
import com.umc.product.authorization.domain.policy.PolicyCondition;

final class PolicyAstCanonicalizer {

    private final PolicyCanonicalJsonWriter writer = new PolicyCanonicalJsonWriter();

    List<CompiledPolicyModule> canonicalize(List<CompiledPolicyModule> modules) {
        return modules.stream()
                .map(this::canonicalizeModule)
                .sorted(Comparator.comparing(CompiledPolicyModule::id))
                .toList();
    }

    private CompiledPolicyModule canonicalizeModule(CompiledPolicyModule module) {
        List<CompiledPolicyStatement> statements = module.statements().stream()
                .map(this::canonicalizeStatement)
                .sorted(Comparator.comparing(CompiledPolicyStatement::id))
                .toList();
        return new CompiledPolicyModule(module.id(), module.filename(), statements);
    }

    private CompiledPolicyStatement canonicalizeStatement(CompiledPolicyStatement statement) {
        List<String> actions = statement.actions().stream().sorted().toList();
        List<CompiledPolicyOutcome> outcomes = statement.outcomes().stream()
                .sorted(Comparator.comparing(CompiledPolicyOutcome::key))
                .toList();
        return new CompiledPolicyStatement(
                statement.id(),
                actions,
                statement.effect(),
                canonicalizeCondition(statement.condition()),
                outcomes);
    }

    private PolicyCondition canonicalizeCondition(PolicyCondition condition) {
        if (condition instanceof PolicyCondition.All all) {
            return new PolicyCondition.All(canonicalizeChildren(all.children()));
        }
        if (condition instanceof PolicyCondition.Any any) {
            return new PolicyCondition.Any(canonicalizeChildren(any.children()));
        }
        return condition;
    }

    private List<PolicyCondition> canonicalizeChildren(List<PolicyCondition> children) {
        List<PolicyCondition> canonical = new ArrayList<>();
        children.forEach(child -> canonical.add(canonicalizeCondition(child)));
        canonical.sort(Comparator.comparing(writer::writeCondition));
        return List.copyOf(canonical);
    }
}
