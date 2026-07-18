package com.umc.product.global.event.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.umc.product.global.event.domain.EventOutbox;

public interface LoadEventOutboxPort {

    Optional<EventOutbox> findByEventId(UUID eventId);

    List<EventOutbox> listPublishable(int limit, Instant now);
}
