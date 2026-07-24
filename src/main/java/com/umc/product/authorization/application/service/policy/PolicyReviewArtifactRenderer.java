package com.umc.product.authorization.application.service.policy;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.umc.product.authorization.domain.policy.CompiledPolicyBundle;
import com.umc.product.authorization.domain.policy.PolicyCondition;
import com.umc.product.authorization.domain.policy.PolicyOperand;
import com.umc.product.authorization.domain.policy.PolicySurfaceDescriptor;
import com.umc.product.authorization.domain.policy.PolicyValue;

public final class PolicyReviewArtifactRenderer {

    private PolicyReviewArtifactRenderer() {
    }

    public static String render(
        CompiledPolicyBundle bundle,
        List<PolicySurfaceDescriptor> surfaces
    ) {
        StringBuilder result = new StringBuilder();
        result.append("# ").append(title(bundle.namespace())).append(" Policy Generated Review Artifact\n\n");
        result.append("- schemaVersion: `").append(bundle.schemaVersion()).append("`\n");
        result.append("- contextSchemaVersion: `").append(bundle.contextSchemaVersion()).append("`\n");
        result.append("- policyVersion: `").append(bundle.policyVersion()).append("`\n");
        result.append("- policyFingerprint: `").append(bundle.policyFingerprint()).append("`\n");
        result.append("- defaultEffect: `").append(bundle.defaultEffect()).append("`\n");
        result.append("- combiningAlgorithm: `").append(bundle.combiningAlgorithm()).append("`\n\n");

        result.append("## Runtime surfaces\n\n");
        result.append("| Surface | Handler | Type | Action | Module | Gate |\n");
        result.append("|---|---|---|---|---|---|\n");
        surfaces.stream()
            .filter(surface -> surface.namespace().equals(bundle.namespace()))
            .sorted()
            .forEach(surface -> result.append("| ")
                .append(escape(surface.id())).append(" | ")
                .append(escape(surface.handler())).append(" | ")
                .append(surface.type()).append(" | ")
                .append(surface.actionId()).append(" | ")
                .append(surface.moduleId()).append(" | ")
                .append(surface.gate()).append(" |\n"));

        result.append("\n## Compiled statements\n\n");
        result.append("| Module | Statement | Effect | Actions | Condition | Outcomes |\n");
        result.append("|---|---|---|---|---|---|\n");
        bundle.modules().stream()
            .sorted(Comparator.comparing(module -> module.id()))
            .forEach(module -> module.statements().stream()
                .sorted(Comparator.comparing(statement -> statement.id()))
                .forEach(statement -> result.append("| ")
                    .append(module.id()).append(" | ")
                    .append(statement.id()).append(" | ")
                    .append(statement.effect()).append(" | ")
                    .append(escape(String.join(", ", statement.actions()))).append(" | ")
                    .append(escape(renderCondition(statement.condition()))).append(" | ")
                    .append(escape(statement.outcomes().stream()
                        .map(outcome -> outcome.key() + "=" + renderOperand(outcome.value()))
                        .sorted()
                        .collect(Collectors.joining(", "))))
                    .append(" |\n")));
        return result.toString();
    }

    private static String renderCondition(PolicyCondition condition) {
        return switch (condition) {
            case PolicyCondition.All all -> "ALL("
                + all.children().stream()
                    .map(PolicyReviewArtifactRenderer::renderCondition)
                    .collect(Collectors.joining(", "))
                + ")";
            case PolicyCondition.Any any -> "ANY("
                + any.children().stream()
                    .map(PolicyReviewArtifactRenderer::renderCondition)
                    .collect(Collectors.joining(", "))
                + ")";
            case PolicyCondition.Predicate predicate -> predicate.operator().name()
                + "("
                + predicate.operands().stream()
                    .map(PolicyReviewArtifactRenderer::renderOperand)
                    .collect(Collectors.joining(", "))
                + ")";
        };
    }

    private static String renderOperand(PolicyOperand operand) {
        return switch (operand) {
            case PolicyOperand.Attribute attribute -> "ATTRIBUTE(" + attribute.name() + ")";
            case PolicyOperand.Literal literal -> renderValue(literal.value());
        };
    }

    private static String renderValue(PolicyValue value) {
        return switch (value) {
            case PolicyValue.BooleanValue booleanValue -> Boolean.toString(booleanValue.value());
            case PolicyValue.LongValue longValue -> Long.toString(longValue.value());
            case PolicyValue.StringValue stringValue -> quote(stringValue.value());
            case PolicyValue.EnumValue enumValue -> enumValue.value();
            case PolicyValue.InstantValue instantValue -> instantValue.value().toString();
            case PolicyValue.LongSetValue setValue -> renderSet(setValue.value());
            case PolicyValue.StringSetValue setValue -> renderSet(setValue.value());
            case PolicyValue.EnumSetValue setValue -> renderSet(setValue.value());
            case PolicyValue.InstantSetValue setValue -> renderSet(setValue.value());
        };
    }

    private static String renderSet(Set<?> values) {
        return values.stream()
            .map(String::valueOf)
            .collect(Collectors.joining(", ", "[", "]"));
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String title(String namespace) {
        if (namespace.isEmpty()) {
            return namespace;
        }
        return Character.toUpperCase(namespace.charAt(0)) + namespace.substring(1);
    }

    private static String escape(String value) {
        return value.replace("|", "\\|").replace("\n", " ");
    }
}
