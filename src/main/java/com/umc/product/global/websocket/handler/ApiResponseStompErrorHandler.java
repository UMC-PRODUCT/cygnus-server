package com.umc.product.global.websocket.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.global.exception.BusinessException;
import com.umc.product.global.exception.constant.CommonErrorCode;
import com.umc.product.global.response.ApiResponse;
import com.umc.product.global.response.code.BaseCode;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

/**
 * STOMP 예외를 HTTP API와 동일한 ApiResponse 형식의 ERROR 프레임으로 변환한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiResponseStompErrorHandler extends StompSubProtocolErrorHandler {

    private static final byte[] INTERNAL_SERVER_ERROR_PAYLOAD = (
        "{\"success\":false,\"code\":\"COMMON-0001\","
            + "\"message\":\"알 수 없는 오류입니다. 관리자에게 문의해주세요.\"}"
    ).getBytes(StandardCharsets.UTF_8);

    private final ObjectMapper objectMapper;

    @Override
    public Message<byte[]> handleClientMessageProcessingError(
        @Nullable Message<byte[]> clientMessage,
        Throwable ex
    ) {
        Optional<BusinessException> businessException = findBusinessException(ex);
        BaseCode errorCode = businessException
            .map(BusinessException::getBaseCode)
            .orElse(CommonErrorCode.INTERNAL_SERVER_ERROR);

        logException(ex, businessException);

        byte[] payload = createErrorPayload(errorCode);
        return createErrorMessage(clientMessage, errorCode, payload);
    }

    private void logException(Throwable ex, Optional<BusinessException> businessException) {
        if (businessException.isPresent()) {
            BusinessException exception = businessException.get();
            log.warn("[WEBSOCKET BUSINESS EXCEPTION] domain={}, code={}, message={}",
                exception.getDomain(), exception.getBaseCode().getCode(), exception.getBaseCode().getMessage());
            return;
        }

        log.error("[WEBSOCKET UNHANDLED EXCEPTION] {}", ex.getMessage(), ex);
    }

    private byte[] createErrorPayload(BaseCode errorCode) {
        ApiResponse<Object> response = ApiResponse.onFailure(
            errorCode.getCode(),
            errorCode.getMessage(),
            null
        );

        try {
            return objectMapper.writeValueAsBytes(response);
        } catch (JsonProcessingException e) {
            log.error("[WEBSOCKET ERROR SERIALIZATION FAILED] {}", e.getMessage(), e);
            return INTERNAL_SERVER_ERROR_PAYLOAD;
        }
    }

    private Message<byte[]> createErrorMessage(
        @Nullable Message<byte[]> clientMessage,
        BaseCode errorCode,
        byte[] payload
    ) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
        accessor.setMessage(errorCode.getMessage());
        accessor.setContentType(MimeTypeUtils.APPLICATION_JSON);
        accessor.setLeaveMutable(true);
        copyReceiptId(clientMessage, accessor);

        return MessageBuilder.createMessage(payload, accessor.getMessageHeaders());
    }

    private void copyReceiptId(@Nullable Message<byte[]> clientMessage, StompHeaderAccessor errorAccessor) {
        if (clientMessage == null) {
            return;
        }

        StompHeaderAccessor clientAccessor =
            MessageHeaderAccessor.getAccessor(clientMessage, StompHeaderAccessor.class);
        if (clientAccessor != null && clientAccessor.getReceipt() != null) {
            errorAccessor.setReceiptId(clientAccessor.getReceipt());
        }
    }

    private Optional<BusinessException> findBusinessException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof BusinessException businessException) {
                return Optional.of(businessException);
            }

            Throwable cause = current.getCause();
            if (cause == current) {
                break;
            }
            current = cause;
        }
        return Optional.empty();
    }
}
