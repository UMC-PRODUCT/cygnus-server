package com.umc.product.global.websocket.application.port.in;

import java.util.Optional;
import java.util.UUID;

public interface StompClientMessageIdResolver {

    boolean supports(String destination);

    Optional<UUID> resolve(Object payload);
}
