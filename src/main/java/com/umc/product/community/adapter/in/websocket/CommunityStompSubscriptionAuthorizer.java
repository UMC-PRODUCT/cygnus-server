package com.umc.product.community.adapter.in.websocket;

import org.springframework.stereotype.Component;

import com.umc.product.global.websocket.application.port.in.StompSubscriptionAuthorizer;

@Component
public class CommunityStompSubscriptionAuthorizer implements StompSubscriptionAuthorizer {

    static final String USER_EVENT_DESTINATION = "/user/queue/community/threads/events";

    @Override
    public boolean supports(String destination) {
        return USER_EVENT_DESTINATION.equals(destination);
    }

    @Override
    public boolean isAuthorized(Long memberId, String destination) {
        return memberId != null && memberId > 0 && supports(destination);
    }
}
