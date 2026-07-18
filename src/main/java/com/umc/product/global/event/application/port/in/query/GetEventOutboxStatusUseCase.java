package com.umc.product.global.event.application.port.in.query;

import java.util.UUID;

import com.umc.product.global.event.application.port.in.query.dto.EventOutboxStatusInfo;

public interface GetEventOutboxStatusUseCase {

    EventOutboxStatusInfo getByEventId(UUID eventId);
}
