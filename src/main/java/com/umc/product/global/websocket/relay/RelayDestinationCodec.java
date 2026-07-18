package com.umc.product.global.websocket.relay;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public final class RelayDestinationCodec {

    private static final String PUBLIC_USER_DESTINATION = "/topic/__internal/user-destination";
    private static final String PUBLIC_USER_REGISTRY = "/topic/__internal/user-registry";
    private static final String BROKER_USER_DESTINATION = "/topic/__internal.user-destination";
    private static final String BROKER_USER_REGISTRY = "/topic/__internal.user-registry";
    private static final String BROKER_COMMUNITY_PREFIX = "/topic/__relay.";
    private static final Pattern THREAD_DESTINATION = Pattern.compile(
        "^/topic/community/threads/[1-9][0-9]*/members/[1-9][0-9]*/events$"
    );
    private static final Pattern MEMBER_DESTINATION = Pattern.compile(
        "^/topic/community/members/[1-9][0-9]*/events$"
    );
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    public String toBroker(String destination) {
        if (PUBLIC_USER_DESTINATION.equals(destination)) {
            return BROKER_USER_DESTINATION;
        }
        if (PUBLIC_USER_REGISTRY.equals(destination)) {
            return BROKER_USER_REGISTRY;
        }
        if (!isCommunityDestination(destination)) {
            return destination;
        }
        return BROKER_COMMUNITY_PREFIX + encode(destination);
    }

    public String toPublic(String destination) {
        if (BROKER_USER_DESTINATION.equals(destination)) {
            return PUBLIC_USER_DESTINATION;
        }
        if (BROKER_USER_REGISTRY.equals(destination)) {
            return PUBLIC_USER_REGISTRY;
        }
        if (destination == null || !destination.startsWith(BROKER_COMMUNITY_PREFIX)) {
            return destination;
        }

        String encoded = destination.substring(BROKER_COMMUNITY_PREFIX.length());
        try {
            String decoded = new String(DECODER.decode(encoded), StandardCharsets.UTF_8);
            return encode(decoded).equals(encoded) && isCommunityDestination(decoded)
                ? decoded
                : destination;
        } catch (IllegalArgumentException ignored) {
            return destination;
        }
    }

    private boolean isCommunityDestination(String destination) {
        return destination != null
            && (THREAD_DESTINATION.matcher(destination).matches()
            || MEMBER_DESTINATION.matcher(destination).matches());
    }

    private String encode(String destination) {
        return ENCODER.encodeToString(destination.getBytes(StandardCharsets.UTF_8));
    }
}
