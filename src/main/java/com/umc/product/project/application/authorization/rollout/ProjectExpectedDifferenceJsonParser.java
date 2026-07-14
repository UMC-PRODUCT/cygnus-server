package com.umc.product.project.application.authorization.rollout;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.project.application.authorization.ProjectPolicyAction;
import com.umc.product.project.application.authorization.rollout.ProjectExpectedDifferenceWire.AttributeValue;
import com.umc.product.project.application.authorization.rollout.ProjectExpectedDifferenceWire.BooleanValue;
import com.umc.product.project.application.authorization.rollout.ProjectExpectedDifferenceWire.ClassificationScope;
import com.umc.product.project.application.authorization.rollout.ProjectExpectedDifferenceWire.Decision;
import com.umc.product.project.application.authorization.rollout.ProjectExpectedDifferenceWire.Document;
import com.umc.product.project.application.authorization.rollout.ProjectExpectedDifferenceWire.Entry;
import com.umc.product.project.application.authorization.rollout.ProjectExpectedDifferenceWire.EnumValue;
import com.umc.product.project.application.authorization.rollout.ProjectExpectedDifferenceWire.LongSetAttributes;
import com.umc.product.project.application.authorization.rollout.ProjectExpectedDifferenceWire.Outcome;
import com.umc.product.project.application.authorization.rollout.ProjectExpectedDifferenceWire.Value;

final class ProjectExpectedDifferenceJsonParser {

    static final String RESOURCE = "policies/project/expected-differences.json";
    private static final int MAX_BYTES = 256 * 1024;
    private static final Set<String> ROOT_FIELDS = Set.of(
        "$schema", "schemaVersion", "contextSchemaVersion", "namespace", "policyVersion",
        "classificationOnly", "entries");
    private static final Set<String> ENTRY_FIELDS = Set.of(
        "id", "testCaseId", "action", "internalOrigin", "classificationScope",
        "contextPredicate", "expectedLegacy", "expectedTarget", "reason", "owner");
    private static final Set<String> PREDICATE_FIELDS = Set.of("type", "operator", "left", "right");
    private static final Set<String> DECISION_FIELDS = Set.of(
        "effect", "capabilityAuthorized", "outcomes", "obligations");
    private static final Set<String> OUTCOME_FIELDS = Set.of("key", "value");

    private final JsonMapper mapper = strictMapper();

    Document parseClasspath() {
        ClassLoader loader = ProjectExpectedDifferenceJsonParser.class.getClassLoader();
        try (InputStream input = loader.getResourceAsStream(RESOURCE)) {
            if (input == null) {
                throw invalid("EXPECTED_DIFFERENCE_RESOURCE_MISSING");
            }
            byte[] bytes = input.readNBytes(MAX_BYTES + 1);
            if (bytes.length > MAX_BYTES) {
                throw invalid("EXPECTED_DIFFERENCE_DOCUMENT_TOO_LARGE");
            }
            return parse(bytes);
        } catch (IOException exception) {
            throw invalid("EXPECTED_DIFFERENCE_RESOURCE_READ_FAILED");
        }
    }

    Document parse(byte[] bytes) {
        rejectBom(bytes);
        JsonNode root = readTree(bytes);
        exactFields(root, ROOT_FIELDS);
        requireText(root, "$schema", "urn:umc:project:expected-difference-matrix:1.0");
        requireText(root, "schemaVersion", "1.0");
        requireText(root, "contextSchemaVersion", "project-1.0");
        requireText(root, "namespace", "project");
        requireText(root, "policyVersion", "1.1.0");
        if (!required(root, "classificationOnly").isBoolean()
            || !root.get("classificationOnly").booleanValue()) {
            throw invalid("EXPECTED_DIFFERENCE_NOT_CLASSIFICATION_ONLY");
        }
        List<Entry> entries = new ArrayList<>();
        for (JsonNode entry : array(root, "entries")) {
            entries.add(parseEntry(entry));
        }
        validateEntries(entries);
        return new Document(entries);
    }

    private Entry parseEntry(JsonNode node) {
        exactFields(node, ENTRY_FIELDS);
        String actionId = text(node, "action");
        if (actionId.indexOf('*') >= 0 || actionId.indexOf('?') >= 0) {
            throw invalid("EXPECTED_DIFFERENCE_WILDCARD_ACTION");
        }
        return new Entry(
            text(node, "id"),
            enumValue(ProjectExpectedDifferenceId.class, text(node, "testCaseId")),
            action(actionId),
            enumValue(ProjectAuthorizationInternalOrigin.class, text(node, "internalOrigin")),
            enumValue(ClassificationScope.class, text(node, "classificationScope")),
            predicate(node.get("contextPredicate")),
            decision(node.get("expectedLegacy")),
            decision(node.get("expectedTarget")),
            boundedText(node, "reason"),
            boundedText(node, "owner")
        );
    }

    private ProjectPolicyAction action(String actionId) {
        try {
            return ProjectPolicyAction.fromId(actionId);
        } catch (IllegalArgumentException exception) {
            throw invalid("EXPECTED_DIFFERENCE_ACTION_UNSUPPORTED");
        }
    }

    private String predicate(JsonNode node) {
        exactFields(node, PREDICATE_FIELDS);
        requireText(node, "type", "PREDICATE");
        requireText(node, "operator", "EQ");
        JsonNode left = required(node, "left");
        exactFields(left, Set.of("type", "name"));
        requireText(left, "type", "ATTRIBUTE");
        JsonNode right = required(node, "right");
        exactFields(right, Set.of("type", "value"));
        requireText(right, "type", "BOOLEAN");
        if (!required(right, "value").isBoolean() || !right.get("value").booleanValue()) {
            throw invalid("EXPECTED_DIFFERENCE_PREDICATE_MUST_EQUAL_TRUE");
        }
        return text(left, "name");
    }

    private Decision decision(JsonNode node) {
        exactFields(node, DECISION_FIELDS);
        JsonNode capability = required(node, "capabilityAuthorized");
        if (!capability.isBoolean()) {
            throw invalid("EXPECTED_DIFFERENCE_INVALID_BOOLEAN");
        }
        return new Decision(
            enumValue(PolicyEffect.class, text(node, "effect")),
            capability.booleanValue(),
            outcomes(node, "outcomes"),
            outcomes(node, "obligations")
        );
    }

    private List<Outcome> outcomes(JsonNode node, String field) {
        List<Outcome> outcomes = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        for (JsonNode outcome : array(node, field)) {
            exactFields(outcome, OUTCOME_FIELDS);
            String key = text(outcome, "key");
            if (!keys.add(key)) {
                throw invalid("EXPECTED_DIFFERENCE_DUPLICATE_OUTCOME");
            }
            outcomes.add(new Outcome(key, value(required(outcome, "value"))));
        }
        return outcomes;
    }

    private Value value(JsonNode node) {
        String type = text(node, "type");
        return switch (type) {
            case "BOOLEAN" -> {
                exactFields(node, Set.of("type", "value"));
                JsonNode value = required(node, "value");
                if (!value.isBoolean()) {
                    throw invalid("EXPECTED_DIFFERENCE_INVALID_BOOLEAN");
                }
                yield new BooleanValue(value.booleanValue());
            }
            case "ENUM" -> {
                exactFields(node, Set.of("type", "value"));
                yield new EnumValue(text(node, "value"));
            }
            case "LONG_SET" -> {
                exactFields(node, Set.of("type", "attributes"));
                List<String> attributes = new ArrayList<>();
                for (JsonNode attribute : array(node, "attributes")) {
                    if (!attribute.isTextual()) {
                        throw invalid("EXPECTED_DIFFERENCE_INVALID_ATTRIBUTE");
                    }
                    attributes.add(attribute.textValue());
                }
                yield new LongSetAttributes(attributes);
            }
            case "ATTRIBUTE" -> {
                exactFields(node, Set.of("type", "name"));
                yield new AttributeValue(text(node, "name"));
            }
            default -> throw invalid("EXPECTED_DIFFERENCE_VALUE_TYPE_UNSUPPORTED");
        };
    }

    private void validateEntries(List<Entry> entries) {
        if (entries.size() != 58) {
            throw invalid("EXPECTED_DIFFERENCE_ROW_COUNT_MISMATCH");
        }
        Set<String> ids = new HashSet<>();
        Set<String> identities = new HashSet<>();
        for (Entry entry : entries) {
            if (!ids.add(entry.id())) {
                throw invalid("EXPECTED_DIFFERENCE_DUPLICATE_ID");
            }
            String identity = entry.testCaseId() + ":" + entry.action().id() + ":" + entry.id();
            if (!identities.add(identity)) {
                throw invalid("EXPECTED_DIFFERENCE_DUPLICATE_IDENTITY");
            }
            boolean synthetic = entry.classificationScope() == ClassificationScope.SYNTHETIC_ONLY;
            if (synthetic != (entry.testCaseId() == ProjectExpectedDifferenceId.E011)) {
                throw invalid("EXPECTED_DIFFERENCE_SYNTHETIC_SCOPE_INVALID");
            }
        }
    }

    private JsonNode readTree(byte[] bytes) {
        if (bytes == null) {
            throw invalid("EXPECTED_DIFFERENCE_MALFORMED_JSON");
        }
        try {
            JsonNode root = mapper.readTree(bytes);
            if (root == null || !root.isObject()) {
                throw invalid("EXPECTED_DIFFERENCE_MALFORMED_JSON");
            }
            rejectNullAndFloat(root);
            return root;
        } catch (JsonProcessingException exception) {
            throw invalid("EXPECTED_DIFFERENCE_MALFORMED_JSON");
        } catch (IOException exception) {
            throw invalid("EXPECTED_DIFFERENCE_MALFORMED_JSON");
        }
    }

    private void rejectNullAndFloat(JsonNode node) {
        if (node.isNull() || node.isFloatingPointNumber()) {
            throw invalid("EXPECTED_DIFFERENCE_VALUE_NOT_ALLOWED");
        }
        node.elements().forEachRemaining(this::rejectNullAndFloat);
    }

    private void exactFields(JsonNode node, Set<String> expected) {
        if (!node.isObject()) {
            throw invalid("EXPECTED_DIFFERENCE_INVALID_OBJECT");
        }
        Set<String> actual = new HashSet<>();
        Iterator<String> names = node.fieldNames();
        names.forEachRemaining(actual::add);
        if (!actual.equals(expected)) {
            throw invalid("EXPECTED_DIFFERENCE_FIELDS_MISMATCH");
        }
    }

    private List<JsonNode> array(JsonNode node, String field) {
        JsonNode value = required(node, field);
        if (!value.isArray()) {
            throw invalid("EXPECTED_DIFFERENCE_INVALID_ARRAY");
        }
        List<JsonNode> values = new ArrayList<>();
        value.forEach(values::add);
        return values;
    }

    private JsonNode required(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null) {
            throw invalid("EXPECTED_DIFFERENCE_MISSING_FIELD");
        }
        return value;
    }

    private String boundedText(JsonNode node, String field) {
        String value = text(node, field);
        if (value.isBlank() || value.length() > 1024) {
            throw invalid("EXPECTED_DIFFERENCE_STRING_LIMIT");
        }
        return value;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = required(node, field);
        if (!value.isTextual()) {
            throw invalid("EXPECTED_DIFFERENCE_INVALID_TEXT");
        }
        return value.textValue();
    }

    private void requireText(JsonNode node, String field, String expected) {
        if (!expected.equals(text(node, field))) {
            throw invalid("EXPECTED_DIFFERENCE_ENVELOPE_MISMATCH");
        }
    }

    private <E extends Enum<E>> E enumValue(Class<E> type, String value) {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException exception) {
            throw invalid("EXPECTED_DIFFERENCE_ENUM_UNSUPPORTED");
        }
    }

    private void rejectBom(byte[] bytes) {
        if (bytes != null && bytes.length >= 3
            && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
            throw invalid("EXPECTED_DIFFERENCE_UTF8_BOM_NOT_ALLOWED");
        }
    }

    private IllegalStateException invalid(String code) {
        return new IllegalStateException(code);
    }

    private JsonMapper strictMapper() {
        JsonFactory factory = JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .disable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
            .disable(JsonReadFeature.ALLOW_YAML_COMMENTS)
            .disable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .disable(JsonReadFeature.ALLOW_TRAILING_COMMA)
            .disable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
            .disable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS)
            .build();
        JsonMapper strict = JsonMapper.builder(factory)
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
            .enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();
        strict.deactivateDefaultTyping();
        return strict;
    }
}
