package com.umc.product.global.event.adapter.out;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.umc.product.global.event.domain.DomainEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EventPayloadSerializer {

    private static final String EVENT_ID = "eventId";
    private static final String OCCURRED_AT = "occurredAt";
    private static final String EVENT_TYPE = "eventType";

    private final ObjectMapper objectMapper;

    public String serialize(DomainEvent event) {
        return serializeWithFingerprint(event).payload();
    }

    public SerializationResult serializeWithFingerprint(DomainEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            JsonNode payloadTree = objectMapper.readTree(payload);
            JsonNode canonicalTree = canonicalize(payloadTree, true);
            String fingerprint = sha256(canonicalTree.toString());
            return new SerializationResult(payload, fingerprint);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("도메인 이벤트 직렬화에 실패했습니다. eventType=" + event.eventType(), e);
        }
    }

    private JsonNode canonicalize(JsonNode node, boolean root) {
        if (node instanceof ObjectNode objectNode) {
            ObjectNode sortedNode = objectMapper.createObjectNode();
            Map<String, JsonNode> sortedFields = new TreeMap<>();
            Iterator<String> fieldNames = objectNode.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                sortedFields.put(fieldName, objectNode.get(fieldName));
            }
            sortedFields.forEach((key, value) -> {
                if (!root || !isEventMetadata(key)) {
                    sortedNode.set(key, canonicalize(value, false));
                }
            });
            return sortedNode;
        }
        if (node instanceof ArrayNode arrayNode) {
            ArrayNode sortedArray = objectMapper.createArrayNode();
            arrayNode.elements().forEachRemaining(element -> sortedArray.add(canonicalize(element, false)));
            return sortedArray;
        }
        return node;
    }

    private boolean isEventMetadata(String key) {
        return EVENT_ID.equals(key) || OCCURRED_AT.equals(key) || EVENT_TYPE.equals(key);
    }

    private String sha256(String canonicalJson) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(canonicalJson.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("도메인 이벤트 fingerprint 계산에 실패했습니다.", e);
        }
    }

    public record SerializationResult(String payload, String fingerprint) {
    }
}
