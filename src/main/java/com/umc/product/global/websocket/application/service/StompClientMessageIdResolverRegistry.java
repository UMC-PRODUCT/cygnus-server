package com.umc.product.global.websocket.application.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.umc.product.global.websocket.application.port.in.StompClientMessageIdResolver;

public class StompClientMessageIdResolverRegistry {

    private final List<StompClientMessageIdResolver> resolvers;

    public StompClientMessageIdResolverRegistry(List<StompClientMessageIdResolver> resolvers) {
        this.resolvers = List.copyOf(resolvers);
    }

    public Optional<UUID> resolve(String destination, Object payload) {
        if (destination == null || payload == null) {
            return Optional.empty();
        }

        try {
            List<StompClientMessageIdResolver> matchedResolvers = resolvers.stream()
                .filter(resolver -> resolver.supports(destination))
                .toList();
            if (matchedResolvers.size() != 1) {
                return Optional.empty();
            }
            return matchedResolvers.getFirst().resolve(payload);
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }
}
