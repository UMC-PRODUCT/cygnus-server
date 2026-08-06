package com.umc.product.global.event.application.port.out;

import java.util.Collection;

import com.umc.product.global.event.domain.DomainEvent;

/**
 * 도메인 이벤트 발행을 위한 Port Out.
 * <p>
 * 응용 레이어와 도메인 레이어는 Spring {@code ApplicationEventPublisher} 같은 인프라 API에
 * 직접 의존하지 않고, 이 인터페이스에만 의존한다. 실제 발행 메커니즘은 어댑터 구현체가 담당한다.
 * <p>
 * 이벤트는 공용 event outbox에 영속화한다. 향후 Kafka, RabbitMQ 등 외부 메시지 브로커 도입 시
 * outbox relay 어댑터를 교체하는 방식으로 확장한다.
 */
public interface DomainEventPublisher {

    /**
     * 단일 도메인 이벤트를 발행한다.
     */
    void publish(DomainEvent event);

    /**
     * 여러 도메인 이벤트를 일괄 발행한다.
     * <p>
     * 발행 순서는 입력 컬렉션의 순회 순서를 따른다. 트랜잭션 경계나 원자성은 어댑터 구현체가
     * 정의한다.
     */
    void publishAll(Collection<? extends DomainEvent> events);
}
