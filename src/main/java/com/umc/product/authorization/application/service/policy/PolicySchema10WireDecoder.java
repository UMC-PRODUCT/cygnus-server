package com.umc.product.authorization.application.service.policy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.umc.product.authorization.domain.policy.PolicyCompilationException;
import com.umc.product.authorization.domain.policy.PolicyFailureCode;

final class PolicySchema10WireDecoder implements PolicyWireDecoder {

    static final String BUNDLE_SCHEMA = "urn:umc:authorization:policy-bundle:1.0";
    static final String MODULE_SCHEMA = "urn:umc:authorization:policy-module:1.0";

    private static final Set<String> COMMON_ENVELOPE_FIELDS =
            Set.of("$schema", "schemaVersion", "contextSchemaVersion", "namespace", "policyVersion");

    @Override
    public String schemaVersion() {
        return "1.0";
    }

    @Override
    public PolicySchema10Wire.Bundle decodeBundle(JsonNode root) {
        requireObject(root);
        requireExactFields(root, with(COMMON_ENVELOPE_FIELDS, "defaultEffect", "combiningAlgorithm", "modules"));
        List<PolicySchema10Wire.ModuleReference> modules = new ArrayList<>();
        for (JsonNode module : requireArray(root, "modules")) {
            requireObject(module);
            requireExactFields(module, Set.of("id", "resource"));
            modules.add(new PolicySchema10Wire.ModuleReference(
                    requireText(module, "id", false), requireText(module, "resource", false)));
        }
        PolicySchema10Wire.CommonEnvelope envelope = decodeEnvelope(root);
        requireSchema(envelope.schema(), BUNDLE_SCHEMA);
        return new PolicySchema10Wire.Bundle(
                envelope,
                requireText(root, "defaultEffect", false),
                requireText(root, "combiningAlgorithm", false),
                List.copyOf(modules));
    }

    @Override
    public PolicySchema10Wire.Module decodeModule(JsonNode root) {
        requireObject(root);
        requireExactFields(root, with(COMMON_ENVELOPE_FIELDS, "moduleId", "statements"));
        PolicySchema10Wire.CommonEnvelope envelope = decodeEnvelope(root);
        requireSchema(envelope.schema(), MODULE_SCHEMA);
        List<PolicySchema10Wire.Statement> statements = new ArrayList<>();
        for (JsonNode statement : requireArray(root, "statements")) {
            statements.add(decodeStatement(statement));
        }
        return new PolicySchema10Wire.Module(
                envelope, requireText(root, "moduleId", false), List.copyOf(statements));
    }

    private PolicySchema10Wire.CommonEnvelope decodeEnvelope(JsonNode root) {
        return new PolicySchema10Wire.CommonEnvelope(
                requireText(root, "$schema", true),
                requireText(root, "schemaVersion", true),
                requireText(root, "contextSchemaVersion", true),
                requireText(root, "namespace", false),
                requireText(root, "policyVersion", false));
    }

    private PolicySchema10Wire.Statement decodeStatement(JsonNode node) {
        requireObject(node);
        requireExactFields(node, Set.of("id", "actions", "effect", "condition", "outcomes"));
        List<String> actions = new ArrayList<>();
        for (JsonNode action : requireArray(node, "actions")) {
            if (!action.isTextual()) {
                fail(PolicyFailureCode.INVALID_FIELD_TYPE);
            }
            actions.add(action.textValue());
        }
        List<PolicySchema10Wire.Outcome> outcomes = new ArrayList<>();
        for (JsonNode outcome : requireArray(node, "outcomes")) {
            outcomes.add(decodeOutcome(outcome));
        }
        return new PolicySchema10Wire.Statement(
                requireText(node, "id", false),
                List.copyOf(actions),
                requireText(node, "effect", false),
                decodeCondition(requireField(node, "condition"), 1, new ConditionBudget()),
                List.copyOf(outcomes));
    }

    private PolicySchema10Wire.Condition decodeCondition(JsonNode node, int depth, ConditionBudget budget) {
        if (depth > PolicySemanticCompiler.MAX_CONDITION_DEPTH) {
            fail(PolicyFailureCode.CONDITION_DEPTH_LIMIT_EXCEEDED);
        }
        budget.addNode();
        requireObject(node);
        String type = requireText(node, "type", false);
        if ("ALL".equals(type) || "ANY".equals(type)) {
            requireExactFields(node, Set.of("type", "conditions"));
            JsonNode childNodes = requireArray(node, "conditions");
            if (childNodes.size() > PolicySemanticCompiler.MAX_CONDITION_CHILDREN) {
                fail(PolicyFailureCode.CONDITION_CHILD_LIMIT_EXCEEDED);
            }
            List<PolicySchema10Wire.Condition> conditions = new ArrayList<>();
            for (JsonNode condition : childNodes) {
                conditions.add(decodeCondition(condition, depth + 1, budget));
            }
            return new PolicySchema10Wire.GroupCondition(type, List.copyOf(conditions));
        }
        if ("PREDICATE".equals(type)) {
            String operator = requireText(node, "operator", false);
            boolean unary = "EXISTS".equals(operator) || "NOT_EXISTS".equals(operator);
            if (unary) {
                requireExactFields(node, Set.of("type", "operator", "left"));
                return new PolicySchema10Wire.PredicateCondition(
                        operator, decodeOperand(requireField(node, "left")), Optional.empty());
            }
            requireExactFields(node, Set.of("type", "operator", "left", "right"));
            return new PolicySchema10Wire.PredicateCondition(
                    operator,
                    decodeOperand(requireField(node, "left")),
                    Optional.of(decodeOperand(requireField(node, "right"))));
        }
        throw new PolicyCompilationException(PolicyFailureCode.UNKNOWN_CONDITION_TYPE);
    }

    private PolicySchema10Wire.Outcome decodeOutcome(JsonNode node) {
        requireObject(node);
        requireExactFields(node, Set.of("key", "value"));
        return new PolicySchema10Wire.Outcome(
                requireText(node, "key", false), decodeOperand(requireField(node, "value")));
    }

    private PolicySchema10Wire.Operand decodeOperand(JsonNode node) {
        requireObject(node);
        String type = requireText(node, "type", false);
        if ("ATTRIBUTE".equals(type)) {
            requireExactFields(node, Set.of("type", "name"));
            return new PolicySchema10Wire.Operand(type, requireText(node, "name", false), null);
        }
        boolean setType = type.endsWith("_SET");
        String valueField = setType ? "values" : "value";
        requireExactFields(node, Set.of("type", valueField));
        return new PolicySchema10Wire.Operand(type, null, decodeLiteral(type, requireField(node, valueField)));
    }

    private Object decodeLiteral(String type, JsonNode value) {
        return switch (type) {
            case "BOOLEAN" -> requireBoolean(value);
            case "LONG" -> requireLong(value);
            case "STRING", "ENUM", "INSTANT" -> requireText(value);
            case "LONG_SET" -> decodeArray(value, this::requireLong);
            case "STRING_SET", "ENUM_SET", "INSTANT_SET" -> decodeArray(value, this::requireText);
            default -> throw new PolicyCompilationException(PolicyFailureCode.UNKNOWN_OPERAND_TYPE);
        };
    }

    private <T> List<T> decodeArray(JsonNode value, NodeDecoder<T> decoder) {
        if (!value.isArray()) {
            fail(value.isFloatingPointNumber()
                    ? PolicyFailureCode.FLOAT_NOT_ALLOWED
                    : PolicyFailureCode.INVALID_FIELD_TYPE);
        }
        List<T> result = new ArrayList<>();
        for (JsonNode element : value) {
            result.add(decoder.decode(element));
        }
        return List.copyOf(result);
    }

    private boolean requireBoolean(JsonNode value) {
        if (!value.isBoolean()) {
            fail(value.isFloatingPointNumber()
                    ? PolicyFailureCode.FLOAT_NOT_ALLOWED
                    : PolicyFailureCode.INVALID_FIELD_TYPE);
        }
        return value.booleanValue();
    }

    private long requireLong(JsonNode value) {
        if (value.isFloatingPointNumber()) {
            fail(PolicyFailureCode.FLOAT_NOT_ALLOWED);
        }
        if (!value.isIntegralNumber() || !value.canConvertToLong()) {
            fail(PolicyFailureCode.INVALID_FIELD_TYPE);
        }
        return value.longValue();
    }

    private String requireText(JsonNode value) {
        if (!value.isTextual()) {
            fail(value.isFloatingPointNumber()
                    ? PolicyFailureCode.FLOAT_NOT_ALLOWED
                    : PolicyFailureCode.INVALID_FIELD_TYPE);
        }
        return value.textValue();
    }

    private String requireText(JsonNode object, String field, boolean schemaField) {
        JsonNode value = object.get(field);
        if (value == null) {
            fail(schemaField ? PolicyFailureCode.MISSING_SCHEMA_FIELD : PolicyFailureCode.INVALID_FIELD_TYPE);
        }
        return requireText(value);
    }

    private JsonNode requireField(JsonNode object, String field) {
        JsonNode value = object.get(field);
        if (value == null) {
            fail(PolicyFailureCode.INVALID_FIELD_TYPE);
        }
        return value;
    }

    private JsonNode requireArray(JsonNode object, String field) {
        JsonNode value = requireField(object, field);
        if (!value.isArray()) {
            fail(PolicyFailureCode.INVALID_FIELD_TYPE);
        }
        return value;
    }

    private void requireObject(JsonNode node) {
        if (!node.isObject()) {
            fail(PolicyFailureCode.INVALID_FIELD_TYPE);
        }
    }

    private void requireExactFields(JsonNode node, Set<String> allowedFields) {
        Iterator<String> names = node.fieldNames();
        while (names.hasNext()) {
            if (!allowedFields.contains(names.next())) {
                fail(PolicyFailureCode.UNKNOWN_FIELD);
            }
        }
    }

    private Set<String> with(Set<String> base, String... additions) {
        Set<String> result = new HashSet<>(base);
        result.addAll(List.of(additions));
        return Set.copyOf(result);
    }

    private void requireSchema(String actual, String expected) {
        if (!expected.equals(actual)) {
            fail(PolicyFailureCode.ENVELOPE_MISMATCH);
        }
    }

    private void fail(PolicyFailureCode code) {
        throw new PolicyCompilationException(code);
    }

    @FunctionalInterface
    private interface NodeDecoder<T> {
        T decode(JsonNode node);
    }

    private static final class ConditionBudget {
        private int nodes;

        private void addNode() {
            nodes++;
            if (nodes > PolicySemanticCompiler.MAX_CONDITION_NODES) {
                throw new PolicyCompilationException(PolicyFailureCode.CONDITION_NODE_LIMIT_EXCEEDED);
            }
        }
    }
}
