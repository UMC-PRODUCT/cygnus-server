package com.umc.product.community.adapter.in.websocket;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.community.adapter.in.websocket.dto.request.CreateCommunityThreadMessageRequest;
import com.umc.product.global.exception.BusinessException;
import com.umc.product.global.exception.constant.CommonErrorCode;
import com.umc.product.global.response.code.BaseCode;
import com.umc.product.global.websocket.handler.WebSocketErrorEvent;
import com.umc.product.global.websocket.support.StompCommandIdParser;

import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommunityStompErrorMapper {

    private static final String CLIENT_MESSAGE_ID = "clientMessageId";

    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public void publish(Principal principal, Message<?> message, Exception exception) {
        if (principal == null) {
            throw new IllegalArgumentException("principal must not be null", exception);
        }
        UUID commandId = commandIdOf(message);
        CommunityStompCommandType command = commandOf(message);
        if (commandId == null || command == null) {
            eventPublisher.publishEvent(WebSocketErrorEvent.from(
                principal.getName(),
                commandId,
                clientMessageIdOf(message),
                errorCodeOf(exception),
                false
            ));
            return;
        }
        publish(new CommunityStompCorrelation(
            principal.getName(),
            commandId,
            clientMessageIdOf(message),
            command
        ), exception);
    }

    public void publish(CommunityStompCorrelation correlation, Throwable exception) {
        BaseCode errorCode = errorCodeOf(exception);
        if (errorCode == CommonErrorCode.INTERNAL_SERVER_ERROR) {
            log.error("[COMMUNITY STOMP COMMAND FAILED] code={}", errorCode.getCode(), exception);
        }
        eventPublisher.publishEvent(WebSocketErrorEvent.from(
            correlation.userName(),
            correlation.commandId(),
            correlation.clientMessageId(),
            errorCode,
            errorCode.getHttpStatus().is5xxServerError()
        ));
    }

    private BaseCode errorCodeOf(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof BusinessException businessException) {
                return businessException.getBaseCode();
            }
            if (current instanceof IllegalArgumentException
                || current instanceof ConstraintViolationException
                || current instanceof MessageConversionException
                || current instanceof JsonProcessingException) {
                return CommonErrorCode.BAD_REQUEST;
            }
            if (current.getCause() == current) {
                break;
            }
            current = current.getCause();
        }
        return CommonErrorCode.INTERNAL_SERVER_ERROR;
    }

    private UUID commandIdOf(Message<?> message) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        return accessor == null ? null : StompCommandIdParser.parse(accessor);
    }

    private CommunityStompCommandType commandOf(Message<?> message) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return null;
        }
        return CommunityStompDestinationParser.parseSend(accessor.getDestination())
            .map(CommunityStompDestinationParser.SendDestination::command)
            .orElse(null);
    }

    private UUID clientMessageIdOf(Message<?> message) {
        Object payload = message.getPayload();
        if (payload instanceof CreateCommunityThreadMessageRequest request) {
            return request.clientMessageUuid();
        }

        String json = switch (payload) {
            case byte[] bytes -> new String(bytes, StandardCharsets.UTF_8);
            case String text -> text;
            default -> null;
        };
        if (json == null) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode value = root == null ? null : root.get(CLIENT_MESSAGE_ID);
            return value != null && value.isTextual()
                ? CommunityStompUuid.parseOrNull(value.textValue())
                : null;
        } catch (JsonProcessingException ignored) {
            return null;
        }
    }
}
