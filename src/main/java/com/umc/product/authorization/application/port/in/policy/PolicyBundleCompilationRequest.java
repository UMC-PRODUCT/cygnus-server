package com.umc.product.authorization.application.port.in.policy;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.umc.product.authorization.domain.policy.PolicyDomainSchema;

public record PolicyBundleCompilationRequest(
        byte[] bundleJson, Map<String, byte[]> moduleJsonByFilename, PolicyDomainSchema domainSchema) {

    public PolicyBundleCompilationRequest {
        bundleJson = Objects.requireNonNull(bundleJson).clone();
        Objects.requireNonNull(moduleJsonByFilename);
        Map<String, byte[]> copies = new LinkedHashMap<>();
        moduleJsonByFilename.forEach((filename, content) -> copies.put(
                Objects.requireNonNull(filename), Objects.requireNonNull(content).clone()));
        moduleJsonByFilename = Map.copyOf(copies);
        Objects.requireNonNull(domainSchema);
    }

    @Override
    public byte[] bundleJson() {
        return bundleJson.clone();
    }

    @Override
    public Map<String, byte[]> moduleJsonByFilename() {
        Map<String, byte[]> copies = new LinkedHashMap<>();
        moduleJsonByFilename.forEach((filename, content) -> copies.put(filename, content.clone()));
        return Map.copyOf(copies);
    }
}
