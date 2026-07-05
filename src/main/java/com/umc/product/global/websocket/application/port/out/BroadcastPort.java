package com.umc.product.global.websocket.application.port.out;

/**
 * 구독자에게 메시지를 브로드캐스트하기 위한 Port Out.
 * <p>
 * 특정 도메인(inquiry, chat 등)에 의존하지 않는 범용 인터페이스다. destination 경로 규칙(예:
 * {@code /topic/inquiry/{id}})의 조립 책임은 호출자에게 있으며, 본 포트는 전달받은 destination 으로 payload 를
 * 그대로 전송하기만 한다. 이렇게 함으로써 어떤 도메인이든 동일한 포트를 재사용할 수 있다.
 * <p>
 * 실제 전송 메커니즘(STOMP simple broker, 외부 broker relay 등)은 어댑터 구현체가 담당한다.
 */
public interface BroadcastPort {

    /**
     * 지정한 destination 을 구독 중인 클라이언트들에게 payload 를 전송한다.
     *
     * @param destination 메시지를 전달할 목적지 경로(예: {@code /topic/inquiry/1}). 호출자가 완성된 경로를 넘긴다.
     * @param payload     전송할 임의의 객체. messageId 등 식별자를 포함할 수 있으나 타입을 강제하지 않는다.
     */
    void broadcast(String destination, Object payload);
}
