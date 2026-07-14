package com.umc.product.authorization.application.service.policy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.umc.product.authorization.domain.policy.PolicyCompilationException;
import com.umc.product.authorization.domain.policy.PolicyFailureCode;

final class PolicyWireDecoderRegistry {

    private final Map<String, PolicyWireDecoder> decoders;

    PolicyWireDecoderRegistry(List<PolicyWireDecoder> decoders) {
        Map<String, PolicyWireDecoder> indexed = new LinkedHashMap<>();
        for (PolicyWireDecoder decoder : decoders) {
            if (indexed.putIfAbsent(decoder.schemaVersion(), decoder) != null) {
                throw new IllegalArgumentException("Duplicate policy schema decoder");
            }
        }
        this.decoders = Map.copyOf(indexed);
    }

    PolicyWireDecoder get(String schemaVersion) {
        PolicyWireDecoder decoder = decoders.get(schemaVersion);
        if (decoder == null) {
            throw new PolicyCompilationException(PolicyFailureCode.UNSUPPORTED_SCHEMA_VERSION);
        }
        return decoder;
    }
}
