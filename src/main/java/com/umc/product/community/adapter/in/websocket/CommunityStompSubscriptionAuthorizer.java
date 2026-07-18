package com.umc.product.community.adapter.in.websocket;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.umc.product.community.adapter.in.websocket.CommunityStompDestinationParser.PersonalSubscription;
import com.umc.product.community.adapter.in.websocket.CommunityStompDestinationParser.SubscriptionDestination;
import com.umc.product.community.adapter.in.websocket.CommunityStompDestinationParser.ThreadSubscription;
import com.umc.product.community.application.port.in.query.thread.GetCommunityThreadDetailUseCase;
import com.umc.product.community.application.port.in.query.thread.dto.GetThreadDetailQuery;
import com.umc.product.global.exception.BusinessException;
import com.umc.product.global.websocket.application.port.in.StompSubscriptionAuthorizer;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CommunityStompSubscriptionAuthorizer implements StompSubscriptionAuthorizer {

    private final GetCommunityThreadDetailUseCase getThreadDetailUseCase;

    @Override
    public boolean supports(String destination) {
        return CommunityStompDestinationParser.parseSubscription(destination).isPresent();
    }

    @Override
    public boolean isAuthorized(Long memberId, String destination) {
        Optional<SubscriptionDestination> parsed =
            CommunityStompDestinationParser.parseSubscription(destination);
        if (memberId == null || memberId <= 0 || parsed.isEmpty()) {
            return false;
        }

        return switch (parsed.get()) {
            case PersonalSubscription personal -> personal.memberId().equals(memberId);
            case ThreadSubscription thread -> isAuthorizedThreadMember(memberId, thread);
        };
    }

    private boolean isAuthorizedThreadMember(Long memberId, ThreadSubscription subscription) {
        if (!subscription.memberId().equals(memberId)) {
            return false;
        }
        try {
            getThreadDetailUseCase.getThread(new GetThreadDetailQuery(subscription.threadId(), memberId));
            return true;
        } catch (BusinessException | IllegalArgumentException ignored) {
            return false;
        }
    }
}
