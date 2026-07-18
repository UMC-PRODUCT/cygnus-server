package com.umc.product.global.event.adapter.out.persistence;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.umc.product.global.event.application.port.out.LoadEventOutboxPort;
import com.umc.product.global.event.application.port.out.SaveEventOutboxPort;
import com.umc.product.global.event.domain.EventOutbox;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EventOutboxPersistenceAdapter implements SaveEventOutboxPort, LoadEventOutboxPort {

    private final EventOutboxJpaRepository eventOutboxJpaRepository;

    @Override
    public void save(EventOutbox eventOutbox) {
        eventOutboxJpaRepository.save(eventOutbox);
    }

    @Override
    public boolean saveIfAbsent(EventOutbox eventOutbox) {
        return eventOutboxJpaRepository.insertIfAbsent(eventOutbox) == 1;
    }

    @Override
    public void saveAll(Collection<EventOutbox> eventOutboxes) {
        eventOutboxJpaRepository.saveAll(eventOutboxes);
    }

    @Override
    public Optional<EventOutbox> findByEventId(UUID eventId) {
        return eventOutboxJpaRepository.findByEventId(eventId);
    }

    @Override
    public List<EventOutbox> listPublishable(int limit, Instant now) {
        return eventOutboxJpaRepository.findPublishableForUpdate(limit, now);
    }
}
