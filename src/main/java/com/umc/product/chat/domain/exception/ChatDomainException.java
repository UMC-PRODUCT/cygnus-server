package com.umc.product.chat.domain.exception;

import com.umc.product.global.exception.BusinessException;
import com.umc.product.global.exception.constant.Domain;

public class ChatDomainException extends BusinessException {

    public ChatDomainException(ChatErrorCode errorCode) {
        super(Domain.CHAT, errorCode);
    }

    public ChatDomainException(ChatErrorCode errorCode, String message) {
        super(Domain.CHAT, errorCode, message);
    }
}
