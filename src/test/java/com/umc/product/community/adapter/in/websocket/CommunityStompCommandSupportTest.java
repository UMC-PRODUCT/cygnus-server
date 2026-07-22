package com.umc.product.community.adapter.in.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.umc.product.community.adapter.in.websocket.dto.event.CommunityCommandAcknowledgement;
import com.umc.product.community.application.service.realtime.CommunityThreadRealtimeMetrics;
import com.umc.product.community.application.service.realtime.CommunityThreadRealtimeMetrics.Operation;
import com.umc.product.community.application.service.realtime.CommunityThreadRealtimeMetrics.Outcome;
import com.umc.product.community.application.service.realtime.CommunityThreadRealtimeMetrics.Reason;
import com.umc.product.community.domain.exception.CommunityDomainException;
import com.umc.product.community.domain.exception.CommunityErrorCode;
import com.umc.product.global.security.MemberPrincipal;

@ExtendWith(MockitoExtension.class)
class CommunityStompCommandSupportTest {

    private static final Long THREAD_ID = 12L;
    private static final Long MEMBER_ID = 41L;
    private static final UUID COMMAND_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    private CommunityStompAckPublisher ackPublisher;
    @Mock
    private CommunityStompErrorMapper errorMapper;
    @Mock
    private CommunityThreadRealtimeMetrics metrics;

    private CommunityStompCommandSupport support;

    @BeforeEach
    void setUp() {
        support = new CommunityStompCommandSupport(ackPublisher, errorMapper, metrics);
    }

    @Test
    @DisplayName("인증 principal과 canonical x-command-id를 typed command context로 해석한다")
    void parsesAuthenticatedCommandContext() {
        // when
        CommunityStompCommandContext context = support.context(authentication(), inboundMessage());

        // then
        assertThat(context).isEqualTo(new CommunityStompCommandContext(
            MEMBER_ID,
            MEMBER_ID.toString(),
            COMMAND_ID
        ));
    }

    @Test
    @DisplayName("성공 명령은 저카디널리티 operation과 outcome을 기록하고 correlated ACK를 보낸다")
    void recordsSuccessAndPublishesAcknowledgement() {
        // given
        CommunityStompCommandContext context = new CommunityStompCommandContext(
            MEMBER_ID,
            MEMBER_ID.toString(),
            COMMAND_ID
        );
        CommunityStompCommandOutcome outcome = new CommunityStompCommandOutcome(
            CommunityStompCommandType.MESSAGE_CREATE,
            34L,
            UUID.fromString("00000000-0000-0000-0000-000000000002"),
            false
        );

        // when
        support.acknowledge(THREAD_ID, context, outcome);

        // then
        verify(metrics).recordSend(Operation.MESSAGE_CREATE, Outcome.SUCCESS);
        ArgumentCaptor<CommunityCommandAcknowledgement> acknowledgement =
            ArgumentCaptor.forClass(CommunityCommandAcknowledgement.class);
        verify(ackPublisher).publish(
            org.mockito.ArgumentMatchers.eq(THREAD_ID),
            org.mockito.ArgumentMatchers.eq(MEMBER_ID),
            acknowledgement.capture()
        );
        assertThat(acknowledgement.getValue().commandId()).isEqualTo(COMMAND_ID);
        assertThat(acknowledgement.getValue().messageId()).isEqualTo(34L);
    }

    @Test
    @DisplayName("권한 오류는 failure와 authorization reason을 기록하고 correlation을 보존한다")
    void recordsAuthorizationFailureAndPreservesCorrelation() {
        // given
        CommunityStompCorrelation correlation = new CommunityStompCorrelation(
            MEMBER_ID.toString(),
            COMMAND_ID,
            null,
            CommunityStompCommandType.MESSAGE_EDIT
        );
        CommunityDomainException exception =
            new CommunityDomainException(CommunityErrorCode.THREAD_ACCESS_DENIED);

        // when
        support.reject(correlation, exception);

        // then
        verify(metrics).recordSend(Operation.MESSAGE_EDIT, Outcome.FAILURE);
        verify(metrics).recordReject(Operation.MESSAGE_EDIT, Reason.AUTHORIZATION);
        verify(errorMapper).publish(correlation, exception);
    }

    private UsernamePasswordAuthenticationToken authentication() {
        return new UsernamePasswordAuthenticationToken(new MemberPrincipal(MEMBER_ID), null, List.of());
    }

    private Message<byte[]> inboundMessage() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/community/threads/12/messages");
        accessor.setNativeHeader("x-command-id", COMMAND_ID.toString());
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
