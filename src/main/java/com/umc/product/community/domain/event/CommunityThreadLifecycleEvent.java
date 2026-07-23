package com.umc.product.community.domain.event;

import com.umc.product.global.event.domain.DomainEvent;
import com.umc.product.global.event.domain.OutboxDispatchMode;

public interface CommunityThreadLifecycleEvent extends DomainEvent {

    @Override
    default OutboxDispatchMode outboxDispatchMode() {
        return OutboxDispatchMode.NON_TRANSACTIONAL;
    }
}
