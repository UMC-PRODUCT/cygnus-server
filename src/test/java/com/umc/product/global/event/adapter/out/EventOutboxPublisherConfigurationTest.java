package com.umc.product.global.event.adapter.out;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.global.event.adapter.in.scheduler.EventOutboxPoller;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.global.event.application.port.out.SaveEventOutboxPort;
import com.umc.product.global.event.application.service.EventOutboxRelayService;

@DisplayName("Event Outbox 고정 구성")
class EventOutboxPublisherConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withBean(SaveEventOutboxPort.class, () -> mock(SaveEventOutboxPort.class))
        .withBean(EventPayloadSerializer.class,
            () -> new EventPayloadSerializer(new ObjectMapper().findAndRegisterModules()))
        .withBean(EventOutboxRelayService.class, () -> mock(EventOutboxRelayService.class))
        .withUserConfiguration(OutboxDomainEventPublisher.class, EventOutboxPoller.class);

    @Test
    @DisplayName("과거 비활성화 property가 있어도 outbox publisher와 relay poller를 사용한다")
    void legacy_disabled_property_does_not_disable_outbox() {
        contextRunner
            .withPropertyValues("app.event-outbox.enabled=false")
            .run(context -> {
                assertThat(context).hasSingleBean(DomainEventPublisher.class);
                assertThat(context.getBean(DomainEventPublisher.class))
                    .isInstanceOf(OutboxDomainEventPublisher.class);
                assertThat(context).hasSingleBean(EventOutboxPoller.class);
            });
    }

    @Test
    @DisplayName("relay를 중지해도 outbox publisher는 유지한다")
    void relay_can_be_disabled_without_disabling_outbox_publisher() {
        contextRunner
            .withPropertyValues("app.event-outbox.relay-enabled=false")
            .run(context -> {
                assertThat(context).hasSingleBean(DomainEventPublisher.class);
                assertThat(context.getBean(DomainEventPublisher.class))
                    .isInstanceOf(OutboxDomainEventPublisher.class);
                assertThat(context).doesNotHaveBean(EventOutboxPoller.class);
            });
    }
}
