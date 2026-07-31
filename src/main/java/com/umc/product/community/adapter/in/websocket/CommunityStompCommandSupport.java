package com.umc.product.community.adapter.in.websocket;

import java.security.Principal;
import java.util.UUID;

import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.umc.product.common.domain.exception.CommonException;
import com.umc.product.community.adapter.in.websocket.dto.event.CommunityCommandAcknowledgement;
import com.umc.product.community.application.service.realtime.CommunityThreadRealtimeMetrics;
import com.umc.product.community.application.service.realtime.CommunityThreadRealtimeMetrics.Operation;
import com.umc.product.community.application.service.realtime.CommunityThreadRealtimeMetrics.Outcome;
import com.umc.product.community.application.service.realtime.CommunityThreadRealtimeMetrics.Reason;
import com.umc.product.global.exception.BusinessException;
import com.umc.product.global.exception.constant.CommonErrorCode;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.websocket.support.StompCommandIdParser;

import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class CommunityStompCommandSupport {

    private final CommunityStompAckPublisher ackPublisher;
    private final CommunityStompErrorMapper errorMapper;
    private final CommunityThreadRealtimeMetrics metrics;

    CommunityStompCommandContext context(Principal principal, Message<?> inboundMessage) {
        if (!(principal instanceof Authentication authentication)
            || !(authentication.getPrincipal() instanceof MemberPrincipal memberPrincipal)
            || memberPrincipal.getMemberId() == null
            || memberPrincipal.getMemberId() <= 0) {
            throw new CommonException(CommonErrorCode.SECURITY_NOT_GIVEN);
        }

        StompHeaderAccessor accessor = accessorOf(inboundMessage);
        UUID commandId = accessor == null ? null : StompCommandIdParser.parse(accessor);
        if (commandId == null) {
            throw new CommonException(CommonErrorCode.BAD_REQUEST);
        }
        return new CommunityStompCommandContext(
            memberPrincipal.getMemberId(),
            principal.getName(),
            commandId
        );
    }

    void acknowledge(
        Long threadId,
        CommunityStompCommandContext context,
        CommunityStompCommandOutcome outcome
    ) {
        metrics.recordSend(operation(outcome.command()), Outcome.SUCCESS);
        ackPublisher.publish(
            threadId,
            context.memberId(),
            new CommunityCommandAcknowledgement(
                context.commandId(),
                outcome.command(),
                outcome.messageId(),
                outcome.clientMessageId(),
                outcome.deduplicated()
            )
        );
    }

    void reject(CommunityStompCorrelation correlation, RuntimeException exception) {
        Operation operation = operation(correlation.command());
        metrics.recordSend(operation, Outcome.FAILURE);
        metrics.recordReject(operation, reason(exception));
        errorMapper.publish(correlation, exception);
    }

    void handleMessageException(
        Exception exception,
        Principal principal,
        Message<?> inboundMessage
    ) {
        StompHeaderAccessor accessor = accessorOf(inboundMessage);
        if (accessor != null) {
            CommunityStompDestinationParser.parseSend(accessor.getDestination())
                .ifPresent(destination -> {
                    Operation operation = operation(destination.command());
                    metrics.recordSend(operation, Outcome.FAILURE);
                    metrics.recordReject(operation, reason(exception));
                });
        }
        errorMapper.publish(principal, inboundMessage, exception);
    }

    private StompHeaderAccessor accessorOf(Message<?> message) {
        return MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    }

    private Operation operation(CommunityStompCommandType command) {
        return switch (command) {
            case MESSAGE_CREATE -> Operation.MESSAGE_CREATE;
            case MESSAGE_EDIT -> Operation.MESSAGE_EDIT;
            case MESSAGE_DELETE -> Operation.MESSAGE_DELETE;
            case REACTION_ADD -> Operation.REACTION_ADD;
            case REACTION_REMOVE -> Operation.REACTION_REMOVE;
            case READ_UPDATE -> Operation.READ_UPDATE;
        };
    }

    private Reason reason(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof BusinessException businessException) {
                return switch (businessException.getBaseCode().getHttpStatus()) {
                    case BAD_REQUEST -> Reason.VALIDATION;
                    case UNAUTHORIZED -> Reason.AUTHENTICATION;
                    case FORBIDDEN -> Reason.AUTHORIZATION;
                    case NOT_FOUND, GONE -> Reason.NOT_FOUND;
                    case CONFLICT -> Reason.CONFLICT;
                    default -> Reason.APPLICATION;
                };
            }
            if (current instanceof IllegalArgumentException
                || current instanceof ConstraintViolationException
                || current instanceof MessageConversionException
                || current instanceof JsonProcessingException) {
                return Reason.VALIDATION;
            }
            if (current.getCause() == current) {
                break;
            }
            current = current.getCause();
        }
        return Reason.UNKNOWN;
    }
}
