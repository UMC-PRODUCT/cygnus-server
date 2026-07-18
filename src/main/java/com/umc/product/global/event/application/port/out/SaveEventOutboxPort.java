package com.umc.product.global.event.application.port.out;

import java.util.Collection;

import com.umc.product.global.event.domain.EventOutbox;

public interface SaveEventOutboxPort {

    void save(EventOutbox eventOutbox);

    boolean saveIfAbsent(EventOutbox eventOutbox);

    void saveAll(Collection<EventOutbox> eventOutboxes);
}
