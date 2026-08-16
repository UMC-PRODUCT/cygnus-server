package com.umc.product.global.client;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ClientOriginRegistry {

    private final Map<String, ClientContextProperties.Origin> origins;

    public ClientOriginRegistry(ClientContextProperties properties) {
        this.origins = properties.origins().stream()
            .filter(origin -> normalize(origin.origin()) != null)
            .collect(Collectors.toUnmodifiableMap(
                origin -> normalize(origin.origin()),
                Function.identity(),
                (left, right) -> right
            ));
    }

    public Optional<ClientContextProperties.Origin> findByOrigin(String origin) {
        String normalized = normalize(origin);
        if (normalized == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(origins.get(normalized));
    }

    private static String normalize(String origin) {
        if (origin == null || origin.isBlank()) {
            return null;
        }
        String normalized = origin.trim();
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
