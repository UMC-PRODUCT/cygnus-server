package com.umc.product.global.event.adapter.out;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.global.event.adapter.out.EventPayloadSerializer.SerializationResult;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.global.event.application.port.out.LoadEventOutboxPort;
import com.umc.product.global.event.application.port.out.SaveEventOutboxPort;
import com.umc.product.global.event.application.port.out.dto.OutboxPublishResult;
import com.umc.product.global.event.domain.DomainEvent;
import com.umc.product.global.event.domain.EventOutbox;
import com.umc.product.global.event.domain.OutboxIdempotencyConflictException;
import com.umc.product.global.observability.W3CTraceparent;

import io.micrometer.tracing.Tracer;

@Component
public class OutboxDomainEventPublisher implements DomainEventPublisher {

    private final SaveEventOutboxPort saveEventOutboxPort;
    private final LoadEventOutboxPort loadEventOutboxPort;
    private final EventPayloadSerializer serializer;
    private final Tracer tracer;

    @Autowired
    public OutboxDomainEventPublisher(
        SaveEventOutboxPort saveEventOutboxPort,
        LoadEventOutboxPort loadEventOutboxPort,
        EventPayloadSerializer serializer,
        ObjectProvider<Tracer> tracerProvider
    ) {
        this(
            saveEventOutboxPort,
            loadEventOutboxPort,
            serializer,
            tracerProvider.getIfAvailable(() -> Tracer.NOOP)
        );
    }

    // Tracer를 직접 주입하는 생성자. (테스트/수동 조립용 — 스프링은 @Autowired 생성자를 사용)
    public OutboxDomainEventPublisher(
        SaveEventOutboxPort saveEventOutboxPort,
        LoadEventOutboxPort loadEventOutboxPort,
        EventPayloadSerializer serializer,
        Tracer tracer
    ) {
        this.saveEventOutboxPort = saveEventOutboxPort;
        this.loadEventOutboxPort = loadEventOutboxPort;
        this.serializer = serializer;
        this.tracer = tracer;
    }

    @Override
    public void publish(DomainEvent event) {
        // 원 요청 trace 컨텍스트에서 traceparent를 캡처해 outbox에 함께 저장한다. (relay span link 복원용)
        String traceparent = W3CTraceparent.capture(tracer);
        SerializationResult serialized = serializer.serializeWithFingerprint(event);
        saveEventOutboxPort.save(EventOutbox.record(
            event,
            serialized.payload(),
            serialized.fingerprint(),
            Instant.now(),
            traceparent
        ));
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OutboxPublishResult publishOnce(DomainEvent event, Instant availableAt) {
        String traceparent = W3CTraceparent.capture(tracer);
        SerializationResult serialized = serializer.serializeWithFingerprint(event);
        EventOutbox candidate = EventOutbox.record(
            event,
            serialized.payload(),
            serialized.fingerprint(),
            availableAt,
            traceparent
        );
        if (saveEventOutboxPort.saveIfAbsent(candidate)) {
            return OutboxPublishResult.from(candidate, false);
        }
        EventOutbox existing = loadEventOutboxPort.findByEventId(candidate.getEventId())
            .orElseThrow(OutboxIdempotencyConflictException::new);
        if (!hasSameIdentity(existing, candidate)) {
            throw new OutboxIdempotencyConflictException();
        }
        return OutboxPublishResult.from(existing, true);
    }

    @Override
    public void publishAll(Collection<? extends DomainEvent> events) {
        // 동일 요청 컨텍스트에서 발행되므로 traceparent를 한 번만 캡처해 모든 이벤트에 적용한다.
        String traceparent = W3CTraceparent.capture(tracer);
        List<EventOutbox> outboxes = events.stream()
            .map(event -> {
                SerializationResult serialized = serializer.serializeWithFingerprint(event);
                return EventOutbox.record(
                    event,
                    serialized.payload(),
                    serialized.fingerprint(),
                    Instant.now(),
                    traceparent
                );
            })
            .toList();
        saveEventOutboxPort.saveAll(outboxes);
    }

    private boolean hasSameIdentity(EventOutbox existing, EventOutbox candidate) {
        return existing.getPayloadFingerprint() != null
            && existing.getAvailableAt() != null
            && existing.getEventClass().equals(candidate.getEventClass())
            && existing.getEventType().equals(candidate.getEventType())
            && existing.getPayloadFingerprint().equals(candidate.getPayloadFingerprint())
            && existing.getAvailableAt().equals(candidate.getAvailableAt());
    }
}
