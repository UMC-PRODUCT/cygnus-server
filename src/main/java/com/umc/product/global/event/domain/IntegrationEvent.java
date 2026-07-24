package com.umc.product.global.event.domain;

import java.util.UUID;

/**
 * 애플리케이션 경계를 넘어 EventBridge로 전달되는 integration event 마커.
 *
 * <p>일반 {@link DomainEvent}는 기존 Spring event bus로 전달하고, 이 타입만 외부 event bus로 전달한다.
 */
public interface IntegrationEvent extends DomainEvent {

    default int schemaVersion() {
        return 1;
    }

    UUID requestId();

    String source();

    Object detail();
}
