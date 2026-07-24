package com.umc.product.notification.application.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.global.config.notification.NotificationTransportProperties;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.global.event.domain.DomainEvent;
import com.umc.product.notification.application.event.FcmNotificationRequestedEvent;
import com.umc.product.notification.application.event.FcmNotificationRequestedIntegrationEvent;
import com.umc.product.notification.application.port.in.RequestFcmNotificationUseCase;
import com.umc.product.notification.application.port.in.dto.FcmNotificationRequestInfo;
import com.umc.product.notification.application.port.in.dto.RequestFcmNotificationCommand;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
public class FcmNotificationCommandService implements RequestFcmNotificationUseCase {

    private static final int MEMBER_CHUNK_SIZE = 500;

    private final DomainEventPublisher eventPublisher;
    private final FcmAudienceResolver audienceResolver;
    private final NotificationTransportProperties transportProperties;

    @Autowired
    public FcmNotificationCommandService(
        DomainEventPublisher eventPublisher,
        FcmAudienceResolver audienceResolver,
        NotificationTransportProperties transportProperties
    ) {
        this.eventPublisher = eventPublisher;
        this.audienceResolver = audienceResolver;
        this.transportProperties = transportProperties;
    }

    public FcmNotificationCommandService(DomainEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        this.audienceResolver = null;
        this.transportProperties = NotificationTransportProperties.local();
    }

    @Override
    public FcmNotificationRequestInfo request(RequestFcmNotificationCommand command) {
        UUID requestId = UUID.randomUUID();
        Instant queuedAt = Instant.now();
        FcmNotificationRequestedEvent localEvent =
            FcmNotificationRequestedEvent.from(requestId, queuedAt, command);
        List<DomainEvent> events = new ArrayList<>();
        if (transportProperties.transport().sendsLocally()) {
            events.add(localEvent);
        }
        if (transportProperties.transport().sendsExternally()) {
            events.addAll(integrationEvents(localEvent));
        }
        eventPublisher.publishAll(events);
        log.info(
            "FCM 알림 요청을 큐잉했습니다: requestId={}, transport={}, explicitMemberCount={}, eventCount={}",
            requestId,
            transportProperties.transport(),
            command.memberIds().size(),
            events.size()
        );
        return FcmNotificationRequestInfo.of(requestId, queuedAt);
    }

    private List<FcmNotificationRequestedIntegrationEvent> integrationEvents(
        FcmNotificationRequestedEvent localEvent
    ) {
        if (audienceResolver == null) {
            throw new IllegalStateException("외부 FCM 전송에는 audience resolver가 필요합니다.");
        }
        List<Long> memberIds = audienceResolver.resolve(localEvent);
        List<List<Long>> chunks = partition(memberIds, MEMBER_CHUNK_SIZE);
        int chunkCount = chunks.size();
        List<FcmNotificationRequestedIntegrationEvent> events = new ArrayList<>(chunkCount);
        for (int index = 0; index < chunkCount; index++) {
            events.add(FcmNotificationRequestedIntegrationEvent.create(
                localEvent,
                chunks.get(index),
                index,
                chunkCount
            ));
        }
        return events;
    }

    private List<List<Long>> partition(List<Long> memberIds, int size) {
        List<List<Long>> chunks = new ArrayList<>();
        for (int index = 0; index < memberIds.size(); index += size) {
            chunks.add(List.copyOf(memberIds.subList(index, Math.min(index + size, memberIds.size()))));
        }
        return chunks;
    }
}
