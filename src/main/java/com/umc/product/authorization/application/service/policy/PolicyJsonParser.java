package com.umc.product.authorization.application.service.policy;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.umc.product.authorization.domain.policy.PolicyCompilationException;
import com.umc.product.authorization.domain.policy.PolicyFailureCode;

public final class PolicyJsonParser {

    static final int MAX_BUNDLE_BYTES = 1024 * 1024;

    private final JsonMapper jsonMapper;
    private final PolicyWireDecoderRegistry decoderRegistry;

    public PolicyJsonParser() {
        this.jsonMapper = createStrictMapper();
        this.decoderRegistry = new PolicyWireDecoderRegistry(List.of(new PolicySchema10WireDecoder()));
    }

    PolicySchema10Wire.Bundle parseBundle(byte[] json) {
        JsonNode root = parseTree(json);
        String schemaVersion = schemaVersion(root);
        return decoderRegistry.get(schemaVersion).decodeBundle(root);
    }

    PolicySchema10Wire.Module parseModule(byte[] json) {
        JsonNode root = parseTree(json);
        String schemaVersion = schemaVersion(root);
        return decoderRegistry.get(schemaVersion).decodeModule(root);
    }

    private JsonNode parseTree(byte[] json) {
        if (json == null) {
            throw new PolicyCompilationException(PolicyFailureCode.MALFORMED_JSON);
        }
        if (json.length > MAX_BUNDLE_BYTES) {
            throw new PolicyCompilationException(PolicyFailureCode.DOCUMENT_TOO_LARGE);
        }
        try {
            JsonNode root = jsonMapper.readTree(json);
            if (root == null) {
                throw new PolicyCompilationException(PolicyFailureCode.MALFORMED_JSON);
            }
            rejectNulls(root);
            return root;
        } catch (PolicyCompilationException exception) {
            throw exception;
        } catch (JsonProcessingException exception) {
            throw new PolicyCompilationException(classify(exception));
        } catch (IOException exception) {
            throw new PolicyCompilationException(PolicyFailureCode.MALFORMED_JSON);
        }
    }

    private String schemaVersion(JsonNode root) {
        if (!root.isObject()) {
            throw new PolicyCompilationException(PolicyFailureCode.INVALID_FIELD_TYPE);
        }
        JsonNode schemaVersion = root.get("schemaVersion");
        if (schemaVersion == null) {
            throw new PolicyCompilationException(PolicyFailureCode.MISSING_SCHEMA_FIELD);
        }
        if (!schemaVersion.isTextual()) {
            throw new PolicyCompilationException(PolicyFailureCode.INVALID_FIELD_TYPE);
        }
        return schemaVersion.textValue();
    }

    private void rejectNulls(JsonNode node) {
        if (node.isNull()) {
            throw new PolicyCompilationException(PolicyFailureCode.NULL_NOT_ALLOWED);
        }
        if (node.isContainerNode()) {
            node.elements().forEachRemaining(this::rejectNulls);
        }
    }

    private PolicyFailureCode classify(JsonProcessingException exception) {
        String message = exception.getOriginalMessage().toLowerCase(Locale.ROOT);
        if (message.contains("duplicate field")) {
            return PolicyFailureCode.DUPLICATE_KEY;
        }
        if (message.contains("trailing token")) {
            return PolicyFailureCode.TRAILING_CONTENT;
        }
        return PolicyFailureCode.MALFORMED_JSON;
    }

    private JsonMapper createStrictMapper() {
        JsonFactory factory = JsonFactory.builder()
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .disable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
                .disable(JsonReadFeature.ALLOW_YAML_COMMENTS)
                .disable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
                .disable(JsonReadFeature.ALLOW_TRAILING_COMMA)
                .disable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
                .disable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS)
                .disable(JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS)
                .disable(JsonReadFeature.ALLOW_TRAILING_DECIMAL_POINT_FOR_NUMBERS)
                .build();
        JsonMapper mapper = JsonMapper.builder(factory)
                .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
                .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        mapper.deactivateDefaultTyping();
        return mapper;
    }
}
