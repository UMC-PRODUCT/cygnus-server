package com.umc.product.global.event.application.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.umc.product.global.event.adapter.out.EventPayloadDeserializer;
import com.umc.product.global.event.application.port.out.LoadEventOutboxPort;
import com.umc.product.global.event.application.port.out.SaveEventOutboxPort;
import com.umc.product.global.event.domain.DomainEvent;
import com.umc.product.global.event.domain.EventOutbox;
import com.umc.product.global.event.domain.OutboxDispatchMode;
import com.umc.product.global.observability.W3CTraceparent;

import io.micrometer.tracing.Link;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;

@Service
public class EventOutboxRelayService {

    private static final Duration BASE_BACKOFF = Duration.ofSeconds(5);
    private static final Duration MAX_BACKOFF = Duration.ofMinutes(5);
    private static final Duration PROCESSING_LEASE = Duration.ofMinutes(5);
    private static final String RELAY_SPAN_NAME = "outbox.relay.publish";

    private final LoadEventOutboxPort loadEventOutboxPort;
    private final SaveEventOutboxPort saveEventOutboxPort;
    private final EventPayloadDeserializer deserializer;
    private final ApplicationEventPublisher eventPublisher;
    private final Tracer tracer;
    private final TransactionTemplate transactionTemplate;
    private final int batchSize;
    private final int maxAttempts;

    @Autowired
    public EventOutboxRelayService(
        LoadEventOutboxPort loadEventOutboxPort,
        SaveEventOutboxPort saveEventOutboxPort,
        EventPayloadDeserializer deserializer,
        ApplicationEventPublisher eventPublisher,
        PlatformTransactionManager transactionManager,
        ObjectProvider<Tracer> tracerProvider,
        @Value("${app.event-outbox.batch-size:100}") int batchSize,
        @Value("${app.event-outbox.max-attempts:5}") int maxAttempts
    ) {
        this(
            loadEventOutboxPort,
            saveEventOutboxPort,
            deserializer,
            eventPublisher,
            transactionManager,
            tracerProvider.getIfAvailable(() -> Tracer.NOOP),
            batchSize,
            maxAttempts
        );
    }

    EventOutboxRelayService(
        LoadEventOutboxPort loadEventOutboxPort,
        SaveEventOutboxPort saveEventOutboxPort,
        EventPayloadDeserializer deserializer,
        ApplicationEventPublisher eventPublisher,
        PlatformTransactionManager transactionManager,
        Tracer tracer,
        int batchSize,
        int maxAttempts
    ) {
        this.loadEventOutboxPort = loadEventOutboxPort;
        this.saveEventOutboxPort = saveEventOutboxPort;
        this.deserializer = deserializer;
        this.eventPublisher = eventPublisher;
        this.tracer = tracer;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.batchSize = batchSize;
        this.maxAttempts = maxAttempts;
    }

    public void relay() {
        List<EventOutbox> outboxes = claimPublishable();
        for (EventOutbox outbox : outboxes) {
            relayOne(outbox);
        }
    }

    private List<EventOutbox> claimPublishable() {
        List<EventOutbox> outboxes = transactionTemplate.execute(status -> {
            Instant now = Instant.now();
            List<EventOutbox> publishableOutboxes = loadEventOutboxPort.listPublishable(batchSize, now);
            Instant leaseUntil = now.plus(PROCESSING_LEASE);
            publishableOutboxes.forEach(outbox -> outbox.markProcessing(leaseUntil));
            saveEventOutboxPort.saveAll(publishableOutboxes);
            return publishableOutboxes;
        });
        return outboxes == null ? List.of() : outboxes;
    }

    private void relayOne(EventOutbox outbox) {
        try {
            publish(outbox);
        } catch (RuntimeException e) {
            recordFailure(outbox, e);
        }
    }

    private void publish(EventOutbox outbox) {
        // 발행 시점에 저장해둔 traceparent로 원 요청 trace를 복원해 span link로 연결한다.
        // relay span은 폴러의 task span 아래 자식으로 생성되며, link로 원 요청 trace와 이어진다.
        TraceContext origin = W3CTraceparent.restore(tracer, outbox.getTraceparent());
        Span span = newRelaySpan(origin);
        try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
            doPublish(outbox);
        } catch (RuntimeException e) {
            span.error(e);
            throw e;
        } finally {
            span.end();
        }
    }

    private Span newRelaySpan(TraceContext origin) {
        Span.Builder builder = tracer.spanBuilder().name(RELAY_SPAN_NAME);
        if (origin != null) {
            builder.addLink(new Link(origin));
        }
        return builder.start();
    }

    private void doPublish(EventOutbox outbox) {
        DomainEvent event = deserializer.deserialize(outbox);
        if (event.outboxDispatchMode() == OutboxDispatchMode.NON_TRANSACTIONAL) {
            eventPublisher.publishEvent(event);
            markPublished(outbox);
            return;
        }

        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(event);
            outbox.markPublished();
            saveEventOutboxPort.save(outbox);
        });
    }

    private void markPublished(EventOutbox outbox) {
        transactionTemplate.executeWithoutResult(status -> {
            outbox.markPublished();
            saveEventOutboxPort.save(outbox);
        });
    }

    private void recordFailure(EventOutbox outbox, RuntimeException e) {
        transactionTemplate.executeWithoutResult(status -> {
            outbox.recordFailure(errorMessage(e), nextAttemptAt(outbox), maxAttempts);
            saveEventOutboxPort.save(outbox);
        });
    }

    private Instant nextAttemptAt(EventOutbox outbox) {
        long multiplier = 1L << Math.min(outbox.getAttempts(), 6);
        Duration backoff = BASE_BACKOFF.multipliedBy(multiplier);
        if (backoff.compareTo(MAX_BACKOFF) > 0) {
            backoff = MAX_BACKOFF;
        }
        return Instant.now().plus(backoff);
    }

    private String errorMessage(RuntimeException e) {
        if (e.getMessage() == null || e.getMessage().isBlank()) {
            return e.getClass().getName();
        }
        return e.getMessage();
    }
}
