package com.umc.product.notification.adapter.in.event;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.umc.product.notification.application.port.in.DeliverTemplateEmailUseCase;
import com.umc.product.notification.domain.TemplateEmailRequestedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TemplateEmailRequestedEventListener {

    private final DeliverTemplateEmailUseCase deliverTemplateEmailUseCase;

    @EventListener
    public void handle(TemplateEmailRequestedEvent event) {
        deliverTemplateEmailUseCase.deliver(event);
    }
}
