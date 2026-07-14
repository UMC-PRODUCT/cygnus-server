package com.umc.product.project.application.authorization;

import java.util.Comparator;
import java.util.stream.Collectors;

import com.umc.product.authorization.domain.policy.CompiledPolicyBundle;
import com.umc.product.authorization.domain.policy.CompiledPolicyOutcome;

public final class ProjectCompiledPolicyMatrix {

    private ProjectCompiledPolicyMatrix() {}

    public static String render(CompiledPolicyBundle bundle) {
        StringBuilder matrix = new StringBuilder();
        matrix.append("# Project Target Policy Matrix\n\n");
        matrix.append("- schemaVersion: `").append(bundle.schemaVersion()).append("`\n");
        matrix.append("- contextSchemaVersion: `").append(bundle.contextSchemaVersion()).append("`\n");
        matrix.append("- policyVersion: `").append(bundle.policyVersion()).append("`\n");
        matrix.append("- policyFingerprint: `").append(bundle.policyFingerprint()).append("`\n\n");
        matrix.append("| Module | Statement | Effect | Actions | Outcomes |\n");
        matrix.append("|---|---|---|---|---|\n");
        bundle.modules().stream()
            .sorted(Comparator.comparing(module -> module.id()))
            .forEach(module -> module.statements().stream()
                .sorted(Comparator.comparing(statement -> statement.id()))
                .forEach(statement -> matrix.append("| ")
                    .append(module.id()).append(" | ")
                    .append(statement.id()).append(" | ")
                    .append(statement.effect()).append(" | ")
                    .append(String.join(", ", statement.actions())).append(" | ")
                    .append(statement.outcomes().stream()
                        .map(CompiledPolicyOutcome::key)
                        .sorted()
                        .collect(Collectors.joining(", ")))
                    .append(" |\n")));
        return matrix.toString();
    }
}
