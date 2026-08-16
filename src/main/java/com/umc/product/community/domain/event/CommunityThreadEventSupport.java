package com.umc.product.community.domain.event;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

final class CommunityThreadEventSupport {

    private CommunityThreadEventSupport() {
    }

    static UUID eventId(UUID value) {
        return Objects.requireNonNull(value, "eventId must not be null");
    }

    static Instant occurredAt(Instant value) {
        return Objects.requireNonNull(value, "occurredAt must not be null");
    }

    static Long id(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("event IDs must be positive");
        }
        return value;
    }

    static List<Long> idSnapshot(Collection<Long> values) {
        if (values == null) {
            throw new IllegalArgumentException("event ID snapshot must not be null");
        }
        return values.stream()
            .map(CommunityThreadEventSupport::id)
            .distinct()
            .sorted()
            .toList();
    }
}
