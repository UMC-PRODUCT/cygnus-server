package com.umc.product.community.adapter.in.websocket;

import org.springframework.stereotype.Component;

import com.umc.product.community.adapter.in.websocket.dto.event.CommunityCommandAcknowledgement;
import com.umc.product.community.adapter.in.websocket.dto.event.CommunityStompEventEnvelope;
import com.umc.product.global.websocket.application.port.out.BroadcastPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommunityStompAckPublisher {

    private final BroadcastPort broadcastPort;

    public void publish(
        Long threadId,
        Long memberId,
        CommunityCommandAcknowledgement acknowledgement
    ) {
        CommunityStompEventEnvelope<CommunityCommandAcknowledgement> event =
            CommunityStompEventEnvelope.acknowledged(threadId, acknowledgement);
        try {
            broadcastPort.broadcast(
                CommunityStompDestinationParser.memberThreadTopic(threadId, memberId),
                event
            );
        } catch (RuntimeException exception) {
            log.warn(
                "[COMMUNITY STOMP ACK BROADCAST FAILED] command={}",
                acknowledgement.command(),
                exception
            );
        }
    }
}
