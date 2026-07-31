package com.umc.product.global.websocket.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.global.websocket.application.port.in.StompSendAuthorizer;

@DisplayName("StompSendAuthorizerRegistry")
class StompSendAuthorizerRegistryTest {

    private static final String DESTINATION = "/app/community/threads/10/messages";

    @Test
    @DisplayName("destination을 지원하는 authorizer 하나가 승인하면 SEND를 허용한다")
    void authorize_singleAuthorizerAllows() {
        StompSendAuthorizerRegistry sut = new StompSendAuthorizerRegistry(
            List.of(authorizer(true, true))
        );

        assertThat(sut.isAuthorized(10L, DESTINATION)).isTrue();
    }

    @Test
    @DisplayName("destination을 지원하는 authorizer가 없으면 SEND를 거부한다")
    void authorize_noAuthorizerDenies() {
        StompSendAuthorizerRegistry sut = new StompSendAuthorizerRegistry(
            List.of(authorizer(false, true))
        );

        assertThat(sut.isAuthorized(10L, DESTINATION)).isFalse();
    }

    @Test
    @DisplayName("destination을 지원하는 authorizer가 거부하면 SEND를 거부한다")
    void authorize_authorizerDenies() {
        StompSendAuthorizerRegistry sut = new StompSendAuthorizerRegistry(
            List.of(authorizer(true, false))
        );

        assertThat(sut.isAuthorized(10L, DESTINATION)).isFalse();
    }

    @Test
    @DisplayName("destination을 지원하는 authorizer가 둘 이상이면 SEND를 거부한다")
    void authorize_multipleAuthorizersDeny() {
        StompSendAuthorizerRegistry sut = new StompSendAuthorizerRegistry(
            List.of(authorizer(true, true), authorizer(true, true))
        );

        assertThat(sut.isAuthorized(10L, DESTINATION)).isFalse();
    }

    @Test
    @DisplayName("인증 주체나 destination이 없으면 SEND를 거부한다")
    void authorize_missingInputDenies() {
        StompSendAuthorizerRegistry sut = new StompSendAuthorizerRegistry(List.of());

        assertThat(sut.isAuthorized(null, DESTINATION)).isFalse();
        assertThat(sut.isAuthorized(10L, null)).isFalse();
    }

    private StompSendAuthorizer authorizer(boolean supports, boolean authorized) {
        return new StompSendAuthorizer() {
            @Override
            public boolean supports(String destination) {
                return supports;
            }

            @Override
            public boolean isAuthorized(Long memberId, String destination) {
                return authorized;
            }
        };
    }
}
