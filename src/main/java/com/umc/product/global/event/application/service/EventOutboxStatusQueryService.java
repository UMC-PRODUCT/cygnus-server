package com.umc.product.global.event.application.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.global.event.application.port.in.query.GetEventOutboxStatusUseCase;
import com.umc.product.global.event.application.port.in.query.dto.EventOutboxStatusInfo;
import com.umc.product.global.event.application.port.out.LoadEventOutboxPort;
import com.umc.product.global.event.domain.EventOutboxNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventOutboxStatusQueryService implements GetEventOutboxStatusUseCase {

    private final LoadEventOutboxPort loadEventOutboxPort;

    @Override
    public EventOutboxStatusInfo getByEventId(UUID eventId) {
        return loadEventOutboxPort.findByEventId(eventId)
            .map(EventOutboxStatusInfo::from)
            .orElseThrow(EventOutboxNotFoundException::new);
    }
}
