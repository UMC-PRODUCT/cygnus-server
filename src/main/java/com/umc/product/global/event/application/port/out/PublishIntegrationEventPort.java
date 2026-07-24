package com.umc.product.global.event.application.port.out;

import com.umc.product.global.event.domain.IntegrationEvent;

public interface PublishIntegrationEventPort {

    /**
     * 외부 broker가 이벤트를 수락할 때까지 동기적으로 발행한다.
     *
     * @throws RuntimeException broker가 이벤트를 수락하지 못한 경우
     */
    void publish(IntegrationEvent event, String traceparent);
}
