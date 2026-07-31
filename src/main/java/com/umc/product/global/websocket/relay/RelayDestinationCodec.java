package com.umc.product.global.websocket.relay;

import org.springframework.stereotype.Component;

@Component
public final class RelayDestinationCodec {

    private static final String PUBLIC_USER_DESTINATION = "/topic/__internal/user-destination";
    private static final String PUBLIC_USER_REGISTRY = "/topic/__internal/user-registry";
    private static final String BROKER_USER_DESTINATION = "/topic/__internal.user-destination";
    private static final String BROKER_USER_REGISTRY = "/topic/__internal.user-registry";
    private static final String PUBLIC_COMMUNITY_USER_QUEUE =
        "/queue/community/threads/events-user";
    private static final String BROKER_COMMUNITY_USER_QUEUE =
        "/queue/community.threads.events-user";

    public String toBroker(String destination) {
        if (PUBLIC_USER_DESTINATION.equals(destination)) {
            return BROKER_USER_DESTINATION;
        }
        if (PUBLIC_USER_REGISTRY.equals(destination)) {
            return BROKER_USER_REGISTRY;
        }
        if (hasSessionSuffix(destination, PUBLIC_COMMUNITY_USER_QUEUE)) {
            return replacePrefix(
                destination,
                PUBLIC_COMMUNITY_USER_QUEUE,
                BROKER_COMMUNITY_USER_QUEUE
            );
        }
        return destination;
    }

    public String toPublic(String destination) {
        if (BROKER_USER_DESTINATION.equals(destination)) {
            return PUBLIC_USER_DESTINATION;
        }
        if (BROKER_USER_REGISTRY.equals(destination)) {
            return PUBLIC_USER_REGISTRY;
        }
        if (hasSessionSuffix(destination, BROKER_COMMUNITY_USER_QUEUE)) {
            return replacePrefix(
                destination,
                BROKER_COMMUNITY_USER_QUEUE,
                PUBLIC_COMMUNITY_USER_QUEUE
            );
        }
        return destination;
    }

    private boolean hasSessionSuffix(String destination, String prefix) {
        return destination != null && destination.startsWith(prefix)
            && destination.length() > prefix.length();
    }

    private String replacePrefix(String destination, String source, String target) {
        return target + destination.substring(source.length());
    }
}
