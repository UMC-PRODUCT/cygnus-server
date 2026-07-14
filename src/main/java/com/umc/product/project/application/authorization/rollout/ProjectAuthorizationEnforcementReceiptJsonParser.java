package com.umc.product.project.application.authorization.rollout;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.umc.product.project.application.authorization.ProjectPolicyAction;

public final class ProjectAuthorizationEnforcementReceiptJsonParser {

    static final int MAX_BYTES = 256 * 1024;
    private static final Set<String> ROOT_FIELDS = Set.of("schemaVersion", "receipts");
    private static final Set<String> RECEIPT_FIELDS = Set.of(
        "policyVersion",
        "policyFingerprint",
        "artifactSha256",
        "wave",
        "actions",
        "approver",
        "approvedAt",
        "expiresAt"
    );

    private final JsonMapper mapper = strictMapper();

    public ProjectAuthorizationEnforcementReceiptDocument parse(byte[] bytes) {
        if (bytes == null) {
            fail(ProjectAuthorizationRolloutFailureCode.RECEIPT_MALFORMED_JSON);
        }
        if (bytes.length > MAX_BYTES) {
            fail(ProjectAuthorizationRolloutFailureCode.RECEIPT_DOCUMENT_TOO_LARGE);
        }
        JsonNode root = readTree(bytes);
        exactFields(root, ROOT_FIELDS);
        String schemaVersion = text(root, "schemaVersion");
        if (!"1.0".equals(schemaVersion)) {
            fail(ProjectAuthorizationRolloutFailureCode.RECEIPT_SCHEMA_UNSUPPORTED);
        }
        List<ProjectAuthorizationEnforcementReceipt> receipts = new ArrayList<>();
        Set<ProjectAuthorizationEnforcementWave> waves = EnumSet.noneOf(ProjectAuthorizationEnforcementWave.class);
        Set<ProjectPolicyAction> actions = EnumSet.noneOf(ProjectPolicyAction.class);
        for (JsonNode node : array(root, "receipts")) {
            ProjectAuthorizationEnforcementReceipt receipt = receipt(node);
            if (!waves.add(receipt.wave())) {
                fail(ProjectAuthorizationRolloutFailureCode.RECEIPT_DUPLICATE_WAVE);
            }
            Set<ProjectPolicyAction> overlap = new HashSet<>(actions);
            overlap.retainAll(receipt.actions());
            if (!overlap.isEmpty()) {
                fail(ProjectAuthorizationRolloutFailureCode.RECEIPT_OVERLAPPING_WAVE);
            }
            actions.addAll(receipt.actions());
            receipts.add(receipt);
        }
        return new ProjectAuthorizationEnforcementReceiptDocument(schemaVersion, receipts);
    }

    private ProjectAuthorizationEnforcementReceipt receipt(JsonNode node) {
        exactFields(node, RECEIPT_FIELDS);
        ProjectAuthorizationEnforcementWave wave = wave(text(node, "wave"));
        Set<ProjectPolicyAction> actions = actions(node);
        if (!actions.equals(wave.actions())) {
            fail(ProjectAuthorizationRolloutFailureCode.RECEIPT_ACTION_SET_MISMATCH);
        }
        String approver = text(node, "approver");
        if (approver.isBlank()) {
            fail(ProjectAuthorizationRolloutFailureCode.RECEIPT_APPROVER_BLANK);
        }
        return new ProjectAuthorizationEnforcementReceipt(
            text(node, "policyVersion"),
            text(node, "policyFingerprint"),
            text(node, "artifactSha256"),
            wave,
            actions,
            approver,
            instant(node, "approvedAt"),
            instant(node, "expiresAt")
        );
    }

    private Set<ProjectPolicyAction> actions(JsonNode node) {
        Set<ProjectPolicyAction> actions = EnumSet.noneOf(ProjectPolicyAction.class);
        for (JsonNode value : array(node, "actions")) {
            if (!value.isTextual()) {
                fail(ProjectAuthorizationRolloutFailureCode.RECEIPT_INVALID_FIELD_TYPE);
            }
            String actionId = value.textValue();
            if (actionId.indexOf('*') >= 0 || actionId.indexOf('?') >= 0) {
                fail(ProjectAuthorizationRolloutFailureCode.RECEIPT_WILDCARD_ACTION);
            }
            ProjectPolicyAction action;
            try {
                action = ProjectPolicyAction.fromId(actionId);
            } catch (IllegalArgumentException exception) {
                throw invalid(ProjectAuthorizationRolloutFailureCode.RECEIPT_ACTION_UNSUPPORTED);
            }
            if (!actions.add(action)) {
                fail(ProjectAuthorizationRolloutFailureCode.RECEIPT_DUPLICATE_ACTION);
            }
        }
        return Set.copyOf(actions);
    }

    private ProjectAuthorizationEnforcementWave wave(String value) {
        try {
            return ProjectAuthorizationEnforcementWave.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw invalid(ProjectAuthorizationRolloutFailureCode.RECEIPT_WAVE_UNSUPPORTED);
        }
    }

    private Instant instant(JsonNode node, String field) {
        try {
            return Instant.parse(text(node, field));
        } catch (DateTimeParseException exception) {
            throw invalid(ProjectAuthorizationRolloutFailureCode.RECEIPT_INSTANT_INVALID);
        }
    }

    private JsonNode readTree(byte[] bytes) {
        try {
            JsonNode root = mapper.readTree(bytes);
            if (root == null || !root.isObject()) {
                fail(ProjectAuthorizationRolloutFailureCode.RECEIPT_MALFORMED_JSON);
            }
            rejectNullAndFloat(root);
            return root;
        } catch (ProjectAuthorizationRolloutConfigurationException exception) {
            throw exception;
        } catch (JsonProcessingException exception) {
            String message = exception.getOriginalMessage().toLowerCase(Locale.ROOT);
            if (message.contains("duplicate field")) {
                throw invalid(ProjectAuthorizationRolloutFailureCode.RECEIPT_DUPLICATE_KEY);
            }
            if (message.contains("trailing token")) {
                throw invalid(ProjectAuthorizationRolloutFailureCode.RECEIPT_TRAILING_CONTENT);
            }
            throw invalid(ProjectAuthorizationRolloutFailureCode.RECEIPT_MALFORMED_JSON);
        } catch (IOException exception) {
            throw invalid(ProjectAuthorizationRolloutFailureCode.RECEIPT_MALFORMED_JSON);
        }
    }

    private void rejectNullAndFloat(JsonNode node) {
        if (node.isNull()) {
            fail(ProjectAuthorizationRolloutFailureCode.RECEIPT_NULL_NOT_ALLOWED);
        }
        if (node.isFloatingPointNumber()) {
            fail(ProjectAuthorizationRolloutFailureCode.RECEIPT_FLOAT_NOT_ALLOWED);
        }
        node.elements().forEachRemaining(this::rejectNullAndFloat);
    }

    private void exactFields(JsonNode node, Set<String> expected) {
        if (!node.isObject()) {
            fail(ProjectAuthorizationRolloutFailureCode.RECEIPT_INVALID_FIELD_TYPE);
        }
        Set<String> actual = new HashSet<>();
        Iterator<String> fields = node.fieldNames();
        fields.forEachRemaining(actual::add);
        if (!actual.equals(expected)) {
            fail(ProjectAuthorizationRolloutFailureCode.RECEIPT_UNKNOWN_FIELD);
        }
    }

    private List<JsonNode> array(JsonNode node, String field) {
        JsonNode value = required(node, field);
        if (!value.isArray()) {
            fail(ProjectAuthorizationRolloutFailureCode.RECEIPT_INVALID_FIELD_TYPE);
        }
        List<JsonNode> values = new ArrayList<>();
        value.elements().forEachRemaining(values::add);
        return values;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = required(node, field);
        if (!value.isTextual()) {
            fail(ProjectAuthorizationRolloutFailureCode.RECEIPT_INVALID_FIELD_TYPE);
        }
        return value.textValue();
    }

    private JsonNode required(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null) {
            fail(ProjectAuthorizationRolloutFailureCode.RECEIPT_UNKNOWN_FIELD);
        }
        return value;
    }

    private static JsonMapper strictMapper() {
        JsonFactory factory = JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .disable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
            .disable(JsonReadFeature.ALLOW_YAML_COMMENTS)
            .disable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .disable(JsonReadFeature.ALLOW_TRAILING_COMMA)
            .disable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
            .disable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS)
            .build();
        JsonMapper result = JsonMapper.builder(factory)
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();
        result.deactivateDefaultTyping();
        return result;
    }

    private static ProjectAuthorizationRolloutConfigurationException invalid(
        ProjectAuthorizationRolloutFailureCode code
    ) {
        return new ProjectAuthorizationRolloutConfigurationException(code);
    }

    private static void fail(ProjectAuthorizationRolloutFailureCode code) {
        throw invalid(code);
    }
}
