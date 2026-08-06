package com.umc.product.chat.domain.event;

import com.umc.product.global.event.domain.DomainEvent;
import com.umc.product.global.event.domain.OutboxDispatchMode;

public interface ChatRealtimeEvent extends DomainEvent {

    @Override
    default OutboxDispatchMode outboxDispatchMode() {
        return OutboxDispatchMode.NON_TRANSACTIONAL;
    }
}
