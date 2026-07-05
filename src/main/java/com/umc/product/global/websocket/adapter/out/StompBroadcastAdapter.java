package com.umc.product.global.websocket.adapter.out;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.umc.product.global.websocket.application.port.out.BroadcastPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * STOMP 메시지 브로커를 사용한 {@link BroadcastPort} 구현 어댑터.
 * <p>
 * 전달받은 destination 을 가공하지 않고 그대로 {@link SimpMessagingTemplate#convertAndSend(Object, Object)}
 * 로 전송한다. 경로 규칙은 호출자가 책임진다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompBroadcastAdapter implements BroadcastPort {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void broadcast(String destination, Object payload) {
        log.debug("STOMP broadcast: destination={}", destination);
        messagingTemplate.convertAndSend(destination, payload);
    }
}
