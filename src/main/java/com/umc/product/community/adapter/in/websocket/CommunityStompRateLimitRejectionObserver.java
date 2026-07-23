package com.umc.product.community.adapter.in.websocket;

import org.springframework.stereotype.Component;

import com.umc.product.community.application.service.realtime.CommunityThreadRealtimeMetrics;
import com.umc.product.community.application.service.realtime.CommunityThreadRealtimeMetrics.Operation;
import com.umc.product.global.websocket.application.port.in.WebSocketRateLimitRejectionObserver;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CommunityStompRateLimitRejectionObserver implements WebSocketRateLimitRejectionObserver {

    private final CommunityThreadRealtimeMetrics metrics;

    @Override
    public void observe(String destination) {
        CommunityStompDestinationParser.parseSend(destination)
            .map(CommunityStompDestinationParser.SendDestination::command)
            .map(this::toOperation)
            .ifPresent(metrics::recordRateLimit);
    }

    private Operation toOperation(CommunityStompCommandType command) {
        return switch (command) {
            case MESSAGE_CREATE -> Operation.MESSAGE_CREATE;
            case MESSAGE_EDIT -> Operation.MESSAGE_EDIT;
            case MESSAGE_DELETE -> Operation.MESSAGE_DELETE;
            case REACTION_ADD -> Operation.REACTION_ADD;
            case REACTION_REMOVE -> Operation.REACTION_REMOVE;
            case READ_UPDATE -> Operation.READ_UPDATE;
        };
    }
}
