package com.umc.product.community.adapter.in.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Community STOMP user destination 구독 인가")
class CommunityStompSubscriptionAuthorizerTest {

    private final CommunityStompSubscriptionAuthorizer authorizer =
        new CommunityStompSubscriptionAuthorizer();

    @Test
    @DisplayName("인증 회원은 Community thread event user queue를 구독할 수 있다")
    void authorizesCommunityThreadUserDestination() {
        String destination = "/user/queue/community/threads/events";

        assertThat(authorizer.supports(destination)).isTrue();
        assertThat(authorizer.isAuthorized(41L, destination)).isTrue();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {-1L, 0L})
    @DisplayName("유효한 인증 회원 ID가 없으면 user queue 구독을 거부한다")
    void rejectsInvalidMemberId(Long memberId) {
        assertThat(authorizer.isAuthorized(
            memberId,
            "/user/queue/community/threads/events"
        )).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/user/queue/community/threads/events/",
        "/user/queue/community/threads/events?x=1",
        "/user/queue/community/events",
        "/user/queue/errors",
        "/topic/community/threads/12/members/41/events",
        "/topic/community/members/41/events",
        "/queue/community/threads/events"
    })
    @DisplayName("유사 경로와 legacy topic은 소유하거나 승인하지 않는다")
    void rejectsAlternativeAndLegacyDestinations(String destination) {
        assertThat(authorizer.supports(destination)).isFalse();
        assertThat(authorizer.isAuthorized(41L, destination)).isFalse();
    }
}
