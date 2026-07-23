package com.umc.product.community.adapter.in.websocket;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.umc.product.community.application.port.in.query.thread.GetCommunityThreadDetailUseCase;
import com.umc.product.community.application.port.in.query.thread.dto.GetThreadDetailQuery;
import com.umc.product.community.application.service.realtime.CommunityThreadRealtimeMetrics;
import com.umc.product.community.application.service.realtime.CommunityThreadRealtimeMetrics.Operation;
import com.umc.product.community.application.service.realtime.CommunityThreadRealtimeMetrics.Outcome;
import com.umc.product.community.application.service.realtime.CommunityThreadRealtimeMetrics.Reason;
import com.umc.product.global.exception.BusinessException;
import com.umc.product.global.websocket.application.port.in.StompSendAuthorizer;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CommunityStompSendAuthorizer implements StompSendAuthorizer {

    private final GetCommunityThreadDetailUseCase getThreadDetailUseCase;
    private final CommunityThreadRealtimeMetrics metrics;

    @Override
    public boolean supports(String destination) {
        return CommunityStompDestinationParser.parseSend(destination).isPresent();
    }

    @Override
    public boolean isAuthorized(Long memberId, String destination) {
        Optional<CommunityStompDestinationParser.SendDestination> parsed =
            CommunityStompDestinationParser.parseSend(destination);
        if (memberId == null || memberId <= 0 || parsed.isEmpty()) {
            parsed.ifPresent(value -> recordAuthorizationReject(value.command()));
            return false;
        }

        try {
            getThreadDetailUseCase.getThread(new GetThreadDetailQuery(parsed.get().threadId(), memberId));
            return true;
        } catch (BusinessException | IllegalArgumentException ignored) {
            recordAuthorizationReject(parsed.get().command());
            return false;
        }
    }

    private void recordAuthorizationReject(CommunityStompCommandType command) {
        Operation operation = operation(command);
        metrics.recordSend(operation, Outcome.REJECTED);
        metrics.recordReject(operation, Reason.AUTHORIZATION);
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
}
