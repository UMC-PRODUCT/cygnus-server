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
import com.umc.product.community.application.service.realtime.CommunityThreadRealtimeMetrics;
import com.umc.product.community.application.service.realtime.CommunityThreadRealtimeMetrics.Operation;
import com.umc.product.community.application.service.realtime.CommunityThreadRealtimeMetrics.Outcome;
import com.umc.product.community.application.service.realtime.CommunityThreadRealtimeMetrics.Reason;
import com.umc.product.community.domain.exception.CommunityDomainException;
import com.umc.product.community.domain.exception.CommunityErrorCode;

@ExtendWith(MockitoExtension.class)
class CommunityStompSendAuthorizerTest {

    @Mock
    private GetCommunityThreadDetailUseCase getThreadDetailUseCase;
    @Mock
    private CommunityThreadRealtimeMetrics metrics;

    @InjectMocks
    private CommunityStompSendAuthorizer authorizer;

    @ParameterizedTest
    @ValueSource(strings = {
        "/app/community/threads/12/messages",
        "/app/community/threads/12/messages/34/edit",
        "/app/community/threads/12/messages/34/delete",
        "/app/community/threads/12/messages/34/reactions/add",
        "/app/community/threads/12/messages/34/reactions/remove",
        "/app/community/threads/12/read"
    })
    @DisplayName("정확한 여섯 Community SEND destination만 소유한다")
    void supportsExactlySixCommunitySendDestinations(String destination) {
        // when
        boolean supported = authorizer.supports(destination);

        // then
        assertThat(supported).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/app/community/threads/0/messages",
        "/app/community/threads/-1/messages",
        "/app/community/threads/01/messages",
        "/app/community/threads/9223372036854775808/messages",
        "/app/community/threads/12//messages",
        "/app/community/threads/12/messages?cursor=1",
        "/app/community/threads/12/messages/34/edit/extra",
        "/app/community/threads/12/events",
        "/app/chat/threads/12/messages"
    })
    @DisplayName("비정규 숫자와 부분 일치 및 대체 namespace SEND destination은 소유하지 않는다")
    void rejectsMalformedOrPartialSendDestinations(String destination) {
        // when
        boolean supported = authorizer.supports(destination);

        // then
        assertThat(supported).isFalse();
    }

    @Test
    @DisplayName("ACTIVE 멤버가 접근 가능한 thread SEND만 승인한다")
    void authorizesAccessibleThreadSend() {
        // given
        Long memberId = 41L;
        String destination = "/app/community/threads/12/messages/34/edit";

        // when
        boolean authorized = authorizer.isAuthorized(memberId, destination);

        // then
        assertThat(authorized).isTrue();
        verify(getThreadDetailUseCase).getThread(new GetThreadDetailQuery(12L, memberId));
    }

    @Test
    @DisplayName("thread 접근 Port In이 거부하면 SEND를 fail-closed 처리한다")
    void rejectsInaccessibleThreadSend() {
        // given
        Long memberId = 41L;
        String destination = "/app/community/threads/12/read";
        GetThreadDetailQuery query = new GetThreadDetailQuery(12L, memberId);
        given(getThreadDetailUseCase.getThread(query))
            .willThrow(new CommunityDomainException(CommunityErrorCode.THREAD_ACCESS_DENIED));

        // when
        boolean authorized = authorizer.isAuthorized(memberId, destination);

        // then
        assertThat(authorized).isFalse();
        verify(metrics).recordSend(Operation.READ_UPDATE, Outcome.REJECTED);
        verify(metrics).recordReject(Operation.READ_UPDATE, Reason.AUTHORIZATION);
    }

    @Test
    @DisplayName("인증 멤버가 없거나 destination이 정확하지 않으면 Port In 호출 없이 거부한다")
    void rejectsMissingPrincipalOrInvalidDestinationWithoutLookup() {
        // when
        boolean missingPrincipal = authorizer.isAuthorized(null, "/app/community/threads/12/messages");
        boolean invalidDestination = authorizer.isAuthorized(41L, "/app/community/threads/12/messages/0/edit");

        // then
        assertThat(missingPrincipal).isFalse();
        assertThat(invalidDestination).isFalse();
        verifyNoInteractions(getThreadDetailUseCase);
    }
}
