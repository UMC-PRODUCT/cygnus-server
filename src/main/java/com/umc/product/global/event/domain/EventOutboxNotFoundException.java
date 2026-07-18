package com.umc.product.global.event.domain;

import com.umc.product.global.exception.BusinessException;
import com.umc.product.global.exception.constant.Domain;

public class EventOutboxNotFoundException extends BusinessException {

    public EventOutboxNotFoundException() {
        super(Domain.COMMON, EventOutboxErrorCode.EVENT_NOT_FOUND);
    }
}
