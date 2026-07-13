package com.umc.product.global.event.adapter.in.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.umc.product.global.event.application.service.EventOutboxRelayService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EventOutboxPoller {

    private final EventOutboxRelayService relayService;

    @Scheduled(fixedDelayString = "${app.event-outbox.poll-interval-ms:1000}")
    public void poll() {
        relayService.relay();
    }
}
