package com.umc.product.community.adapter.in.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.fasterxml.jackson.core.JsonParseException;
import com.umc.product.common.domain.exception.CommonException;
import com.umc.product.community.adapter.in.websocket.dto.event.CommunityCommandAcknowledgement;
import com.umc.product.community.application.service.realtime.CommunityThreadRealtimeMetrics;
import com.umc.product.community.application.service.realtime.CommunityThreadRealtimeMetrics.Operation;
import com.umc.product.community.application.service.realtime.CommunityThreadRealtimeMetrics.Outcome;
import com.umc.product.community.application.service.realtime.CommunityThreadRealtimeMetrics.Reason;
import com.umc.product.community.domain.exception.CommunityDomainException;
import com.umc.product.community.domain.exception.CommunityErrorCode;
import com.umc.product.global.exception.constant.CommonErrorCode;
import com.umc.product.global.security.MemberPrincipal;

import jakarta.validation.ConstraintViolationException;

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

    @ParameterizedTest
    @EnumSource(CommunityStompCommandType.class)
    @DisplayName("모든 command type은 대응 operation으로 성공 metric과 ACK를 기록한다")
    void acknowledgesEveryCommandType(CommunityStompCommandType command) {
        CommunityStompCommandContext context = new CommunityStompCommandContext(
            MEMBER_ID,
            MEMBER_ID.toString(),
            COMMAND_ID
        );
        CommunityStompCommandOutcome outcome = new CommunityStompCommandOutcome(
            command,
            34L,
            null,
            false
        );

        support.acknowledge(THREAD_ID, context, outcome);

        verify(metrics).recordSend(operation(command), Outcome.SUCCESS);
    }

    @Test
    @DisplayName("인증 타입·principal·member ID·command ID가 잘못되면 context 생성을 거부한다")
    void rejectsInvalidCommandContext() {
        Principal plainPrincipal = () -> "41";
        UsernamePasswordAuthenticationToken wrongPrincipal =
            new UsernamePasswordAuthenticationToken("41", null, List.of());
        UsernamePasswordAuthenticationToken nullMember =
            new UsernamePasswordAuthenticationToken(new MemberPrincipal(null), null, List.of());
        UsernamePasswordAuthenticationToken zeroMember =
            new UsernamePasswordAuthenticationToken(new MemberPrincipal(0L), null, List.of());

        assertThatThrownBy(() -> support.context(null, inboundMessage()))
            .isInstanceOf(CommonException.class);
        assertThatThrownBy(() -> support.context(plainPrincipal, inboundMessage()))
            .isInstanceOf(CommonException.class);
        assertThatThrownBy(() -> support.context(wrongPrincipal, inboundMessage()))
            .isInstanceOf(CommonException.class);
        assertThatThrownBy(() -> support.context(nullMember, inboundMessage()))
            .isInstanceOf(CommonException.class);
        assertThatThrownBy(() -> support.context(zeroMember, inboundMessage()))
            .isInstanceOf(CommonException.class);
        assertThatThrownBy(() -> support.context(authentication(), MessageBuilder.withPayload(new byte[0]).build()))
            .isInstanceOf(CommonException.class);
        assertThatThrownBy(() -> support.context(authentication(), inboundMessageWithoutCommandId()))
            .isInstanceOf(CommonException.class);
    }

    @Test
    @DisplayName("BusinessException HTTP status를 저카디널리티 reject reason으로 정규화한다")
    void normalizesBusinessExceptionReasons() {
        assertRejectReason(new CommonException(CommonErrorCode.BAD_REQUEST), Reason.VALIDATION);
        assertRejectReason(new CommonException(CommonErrorCode.UNAUTHORIZED), Reason.AUTHENTICATION);
        assertRejectReason(new CommonException(CommonErrorCode.FORBIDDEN), Reason.AUTHORIZATION);
        assertRejectReason(new CommonException(CommonErrorCode.NOT_FOUND), Reason.NOT_FOUND);
        assertRejectReason(new CommunityDomainException(CommunityErrorCode.THREAD_DELETED), Reason.NOT_FOUND);
        assertRejectReason(new CommunityDomainException(CommunityErrorCode.REPORT_ALREADY_EXISTS), Reason.CONFLICT);
        assertRejectReason(new CommonException(CommonErrorCode.NOT_IMPLEMENTED), Reason.APPLICATION);
    }

    @Test
    @DisplayName("변환·validation 예외 원인은 VALIDATION, 알 수 없거나 순환 cause는 UNKNOWN으로 기록한다")
    void normalizesTechnicalFailureReasons() {
        assertRejectReason(new IllegalArgumentException(), Reason.VALIDATION);
        assertRejectReason(new ConstraintViolationException(Set.of()), Reason.VALIDATION);
        assertRejectReason(new MessageConversionException("invalid"), Reason.VALIDATION);
        assertRejectReason(new RuntimeException(new JsonParseException(null, "invalid")), Reason.VALIDATION);
        assertRejectReason(new RuntimeException("unknown"), Reason.UNKNOWN);
        RuntimeException cyclic = new RuntimeException("cyclic") {
            @Override
            public synchronized Throwable getCause() {
                return this;
            }
        };
        assertRejectReason(cyclic, Reason.UNKNOWN);
    }

    @Test
    @DisplayName("message exception handler는 destination이 있으면 실패 metric을 남기고 항상 오류 mapper에 위임한다")
    void handlesMessageExceptionWithAndWithoutDestination() {
        IllegalArgumentException invalid = new IllegalArgumentException("invalid");
        support.handleMessageException(invalid, authentication(), inboundMessage());
        Message<byte[]> withoutAccessor = MessageBuilder.withPayload(new byte[0]).build();
        support.handleMessageException(invalid, authentication(), withoutAccessor);

        verify(metrics).recordSend(Operation.MESSAGE_CREATE, Outcome.FAILURE);
        verify(metrics).recordReject(Operation.MESSAGE_CREATE, Reason.VALIDATION);
        verify(errorMapper, times(2)).publish(any(), any(), eq(invalid));
    }

    private void assertRejectReason(RuntimeException exception, Reason reason) {
        CommunityStompCorrelation correlation = new CommunityStompCorrelation(
            MEMBER_ID.toString(),
            COMMAND_ID,
            null,
            CommunityStompCommandType.MESSAGE_DELETE
        );

        support.reject(correlation, exception);

        verify(metrics, atLeastOnce()).recordReject(Operation.MESSAGE_DELETE, reason);
        verify(errorMapper).publish(correlation, exception);
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

    private UsernamePasswordAuthenticationToken authentication() {
        return new UsernamePasswordAuthenticationToken(new MemberPrincipal(MEMBER_ID), null, List.of());
    }

    private Message<byte[]> inboundMessage() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/community/threads/12/messages");
        accessor.setNativeHeader("x-command-id", COMMAND_ID.toString());
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<byte[]> inboundMessageWithoutCommandId() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/community/threads/12/messages");
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
