package com.umc.product.notification.application.port.in;

import com.umc.product.notification.domain.TemplateEmailRequestedEvent;

public interface DeliverTemplateEmailUseCase {

    void deliver(TemplateEmailRequestedEvent event);
}
