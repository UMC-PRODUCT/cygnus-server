package com.umc.product.community.adapter.in.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.community.application.port.in.query.thread.GetCommunityThreadDetailUseCase;
import com.umc.product.community.application.port.in.query.thread.dto.GetThreadDetailQuery;
import com.umc.product.community.domain.exception.CommunityDomainException;
import com.umc.product.community.domain.exception.CommunityErrorCode;

@ExtendWith(MockitoExtension.class)
class CommunityStompSubscriptionAuthorizerTest {

    @Mock
    private GetCommunityThreadDetailUseCase getThreadDetailUseCase;

    @InjectMocks
    private CommunityStompSubscriptionAuthorizer authorizer;

    @Test
    @DisplayName("principal과 path member가 같고 ACTIVE thread 멤버이면 구독을 승인한다")
    void authorizesSelfThreadMemberSubscription() {
        // given
        Long memberId = 41L;
        String destination = "/topic/community/threads/12/members/41/events";

        // when
        boolean authorized = authorizer.isAuthorized(memberId, destination);

        // then
        assertThat(authorized).isTrue();
        verify(getThreadDetailUseCase).getThread(new GetThreadDetailQuery(12L, memberId));
    }

    @Test
    @DisplayName("path member가 principal과 다르면 thread 조회 없이 spoof 구독을 거부한다")
    void rejectsSpoofedThreadMemberSubscription() {
        // when
        boolean authorized = authorizer.isAuthorized(
            41L,
            "/topic/community/threads/12/members/42/events"
        );

        // then
        assertThat(authorized).isFalse();
        verifyNoInteractions(getThreadDetailUseCase);
    }

    @Test
    @DisplayName("principal 자신의 personal topic만 승인한다")
    void authorizesOnlySelfPersonalSubscription() {
        // when
        boolean self = authorizer.isAuthorized(41L, "/topic/community/members/41/events");
        boolean spoofed = authorizer.isAuthorized(41L, "/topic/community/members/42/events");

        // then
        assertThat(self).isTrue();
        assertThat(spoofed).isFalse();
        verifyNoInteractions(getThreadDetailUseCase);
    }

    @Test
    @DisplayName("ACTIVE membership 또는 non-deleted thread 검증 실패 시 구독을 거부한다")
    void rejectsInaccessibleThreadSubscription() {
        // given
        Long memberId = 41L;
        String destination = "/topic/community/threads/12/members/41/events";
        GetThreadDetailQuery query = new GetThreadDetailQuery(12L, memberId);
        given(getThreadDetailUseCase.getThread(query))
            .willThrow(new CommunityDomainException(CommunityErrorCode.THREAD_DELETED));

        // when
        boolean authorized = authorizer.isAuthorized(memberId, destination);

        // then
        assertThat(authorized).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/topic/community/threads/12/events",
        "/topic/community/threads/0/members/41/events",
        "/topic/community/threads/12/members/01/events",
        "/topic/community/threads/12/members/9223372036854775808/events",
        "/topic/community/threads/12//members/41/events",
        "/topic/community/members/41/events?x=1",
        "/queue/community/members/41/events"
    })
    @DisplayName("공유 topic과 malformed 또는 대체 namespace 구독은 소유하지 않는다")
    void rejectsSharedAndMalformedSubscriptionDestinations(String destination) {
        // when
        boolean supported = authorizer.supports(destination);

        // then
        assertThat(supported).isFalse();
    }
}
