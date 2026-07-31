package com.umc.product.global.websocket.application.service;

import java.util.List;

import com.umc.product.global.websocket.application.port.in.StompSendAuthorizer;

import lombok.RequiredArgsConstructor;

/**
 * STOMP SEND destination을 소유한 소비 도메인 authorizer에 인가를 위임한다.
 *
 * <p>destination을 지원하는 authorizer가 없거나 둘 이상이면 소유권이 불명확하므로 fail-closed 처리한다.
 */
@RequiredArgsConstructor
public class StompSendAuthorizerRegistry {

    private final List<StompSendAuthorizer> authorizers;

    public boolean isAuthorized(Long memberId, String destination) {
        if (memberId == null || destination == null) {
            return false;
        }

        List<StompSendAuthorizer> matchedAuthorizers = authorizers.stream()
            .filter(authorizer -> authorizer.supports(destination))
            .toList();

        return matchedAuthorizers.size() == 1
            && matchedAuthorizers.getFirst().isAuthorized(memberId, destination);
    }
}
