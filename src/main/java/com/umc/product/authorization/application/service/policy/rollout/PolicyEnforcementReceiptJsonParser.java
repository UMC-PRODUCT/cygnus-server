package com.umc.product.authorization.application.service.policy.rollout;

import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.umc.product.authorization.domain.policy.rollout.PolicyEnforcementReceipt;
import com.umc.product.authorization.domain.policy.rollout.PolicyEnforcementReceiptDocument;

public final class PolicyEnforcementReceiptJsonParser {

    static final int MAX_BYTES = 256 * 1024;

    private final JsonMapper mapper = strictMapper();

    public PolicyEnforcementReceiptDocument parse(byte[] bytes) {
        if (bytes == null || bytes.length > MAX_BYTES) {
            throw invalid("Policy enforcement receipt 크기가 유효하지 않습니다.");
        }
        try {
            JsonNode root = mapper.readTree(bytes);
            rejectNullAndFloat(root);
            WireDocument document = mapper.treeToValue(root, WireDocument.class);
            if (!"1.0".equals(document.schemaVersion())) {
                throw invalid("지원하지 않는 policy enforcement receipt schemaVersion입니다.");
            }
            List<PolicyEnforcementReceipt> receipts = document.receipts().stream()
                .map(this::toReceipt)
                .toList();
            validateNoOverlap(receipts);
            return new PolicyEnforcementReceiptDocument(
                document.schemaVersion(),
                document.namespace(),
                receipts);
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw invalid("Policy enforcement receipt field가 유효하지 않습니다.");
        } catch (JsonProcessingException exception) {
            throw invalid("Policy enforcement receipt JSON이 유효하지 않습니다.");
        } catch (IOException exception) {
            throw invalid("Policy enforcement receipt JSON을 읽지 못했습니다.");
        }
    }

    private PolicyEnforcementReceipt toReceipt(WireReceipt wire) {
        try {
            if (wire.actions().stream().anyMatch(action -> action.contains("*") || action.contains("?"))) {
                throw invalid("Policy enforcement receipt는 wildcard action을 지원하지 않습니다.");
            }
            return new PolicyEnforcementReceipt(
                wire.policyVersion(),
                wire.policyFingerprint(),
                wire.artifactSha256(),
                Set.copyOf(wire.actions()),
                wire.approver(),
                Instant.parse(wire.approvedAt()),
                Instant.parse(wire.expiresAt()));
        } catch (RuntimeException exception) {
            throw invalid("Policy enforcement receipt field가 유효하지 않습니다.");
        }
    }

    private void validateNoOverlap(List<PolicyEnforcementReceipt> receipts) {
        Set<String> actions = new HashSet<>();
        for (PolicyEnforcementReceipt receipt : receipts) {
            for (String action : receipt.actions()) {
                if (!actions.add(action)) {
                    throw invalid("Policy enforcement receipt action이 중복되었습니다: " + action);
                }
            }
        }
    }

    private void rejectNullAndFloat(JsonNode node) {
        if (node == null || node.isNull()) {
            throw invalid("Policy enforcement receipt에 null을 사용할 수 없습니다.");
        }
        if (node.isFloatingPointNumber()) {
            throw invalid("Policy enforcement receipt에 floating point number를 사용할 수 없습니다.");
        }
        node.elements().forEachRemaining(this::rejectNullAndFloat);
    }

    private JsonMapper strictMapper() {
        JsonFactory factory = JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .build();
        return JsonMapper.builder(factory)
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .build();
    }

    private IllegalStateException invalid(String message) {
        return new IllegalStateException(message);
    }

    private record WireDocument(
        String schemaVersion,
        String namespace,
        List<WireReceipt> receipts
    ) {

        private WireDocument {
            if (receipts == null) {
                throw new IllegalArgumentException("receipts는 필수입니다.");
            }
        }
    }

    private record WireReceipt(
        String policyVersion,
        String policyFingerprint,
        String artifactSha256,
        List<String> actions,
        String approver,
        String approvedAt,
        String expiresAt
    ) {

        private WireReceipt {
            if (actions == null) {
                throw new IllegalArgumentException("actions는 필수입니다.");
            }
        }
    }
}
