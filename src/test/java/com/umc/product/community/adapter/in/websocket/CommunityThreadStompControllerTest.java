package com.umc.product.community.adapter.in.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.umc.product.community.adapter.in.websocket.dto.request.CreateCommunityThreadMessageRequest;
import com.umc.product.community.adapter.in.websocket.dto.request.EditCommunityThreadMessageRequest;
import com.umc.product.community.application.port.in.command.thread.message.CreateCommunityThreadMessageUseCase;
import com.umc.product.community.application.port.in.command.thread.message.EditCommunityThreadMessageUseCase;
import com.umc.product.community.application.port.in.command.thread.message.ManageCommunityThreadMessageReactionUseCase;
import com.umc.product.community.application.port.in.command.thread.message.TombstoneCommunityThreadMessageUseCase;
import com.umc.product.community.application.port.in.command.thread.message.UpdateCommunityThreadReadUseCase;
import com.umc.product.community.application.port.in.command.thread.message.dto.CreateCommunityThreadMessageCommand;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageInfo;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageMutationInfo;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageStatus;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageType;
import com.umc.product.community.domain.exception.CommunityDomainException;
import com.umc.product.community.domain.exception.CommunityErrorCode;
import com.umc.product.global.security.MemberPrincipal;

@ExtendWith(MockitoExtension.class)
class CommunityThreadStompControllerTest {

    private static final Long THREAD_ID = 12L;
    private static final Long MEMBER_ID = 41L;
    private static final Long MESSAGE_ID = 34L;
    private static final UUID COMMAND_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CLIENT_MESSAGE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Mock
    private CreateCommunityThreadMessageUseCase createMessageUseCase;
    @Mock
    private EditCommunityThreadMessageUseCase editMessageUseCase;
    @Mock
    private TombstoneCommunityThreadMessageUseCase tombstoneMessageUseCase;
    @Mock
    private ManageCommunityThreadMessageReactionUseCase manageReactionUseCase;
    @Mock
    private UpdateCommunityThreadReadUseCase updateReadUseCase;
    @Mock
    private CommunityStompCommandSupport commandSupport;

    @InjectMocks
    private CommunityThreadStompController controller;

    @Test
    @DisplayName("controller는 정확히 여섯 Community command mapping만 노출한다")
    void exposesExactlySixCommandMappings() {
        // when
        Set<String> mappings = Arrays.stream(CommunityThreadStompController.class.getDeclaredMethods())
            .map(method -> method.getAnnotation(MessageMapping.class))
            .filter(java.util.Objects::nonNull)
            .flatMap(annotation -> Arrays.stream(annotation.value()))
            .collect(Collectors.toSet());

        // then
        assertThat(mappings).containsExactlyInAnyOrder(
            "/community/threads/{threadId}/messages",
            "/community/threads/{threadId}/messages/{messageId}/edit",
            "/community/threads/{threadId}/messages/{messageId}/delete",
            "/community/threads/{threadId}/messages/{messageId}/reactions/add",
            "/community/threads/{threadId}/messages/{messageId}/reactions/remove",
            "/community/threads/{threadId}/read"
        );
    }

    @Test
    @DisplayName("message create는 body가 아니라 인증 principal의 member ID로 Port In을 호출하고 correlated ACK를 보낸다")
    void createUsesAuthenticatedMemberAndPublishesCorrelatedAck() {
        // given
        CreateCommunityThreadMessageRequest request = new CreateCommunityThreadMessageRequest(
            CLIENT_MESSAGE_ID.toString(),
            CommunityThreadMessageType.TEXT,
            "안녕하세요",
            List.of(),
            List.of(),
            null
        );
        given(createMessageUseCase.create(any())).willReturn(messageMutation(false));
        given(commandSupport.context(any(), any())).willReturn(commandContext());

        // when
        controller.createMessage(THREAD_ID, request, authentication(), inboundMessage());

        // then
        ArgumentCaptor<CreateCommunityThreadMessageCommand> commandCaptor =
            ArgumentCaptor.forClass(CreateCommunityThreadMessageCommand.class);
        verify(createMessageUseCase).create(commandCaptor.capture());
        assertThat(commandCaptor.getValue().senderMemberId()).isEqualTo(MEMBER_ID);
        assertThat(commandCaptor.getValue().clientMessageId()).isEqualTo(CLIENT_MESSAGE_ID);

        ArgumentCaptor<CommunityStompCommandOutcome> outcomeCaptor =
            ArgumentCaptor.forClass(CommunityStompCommandOutcome.class);
        verify(commandSupport).acknowledge(
            org.mockito.ArgumentMatchers.eq(THREAD_ID),
            org.mockito.ArgumentMatchers.eq(commandContext()),
            outcomeCaptor.capture()
        );
        assertThat(outcomeCaptor.getValue()).isEqualTo(new CommunityStompCommandOutcome(
            CommunityStompCommandType.MESSAGE_CREATE,
            MESSAGE_ID,
            CLIENT_MESSAGE_ID,
            false
        ));
    }

    @Test
    @DisplayName("application 오류는 ACK 없이 command correlation을 유지한 non-terminal error로 위임한다")
    void applicationErrorKeepsCommandCorrelationWithoutAck() {
        // given
        CommunityDomainException exception =
            new CommunityDomainException(CommunityErrorCode.THREAD_ACCESS_DENIED);
        given(editMessageUseCase.edit(any())).willThrow(exception);
        given(commandSupport.context(any(), any())).willReturn(commandContext());

        // when
        controller.editMessage(
            THREAD_ID,
            MESSAGE_ID,
            new EditCommunityThreadMessageRequest("수정"),
            authentication(),
            inboundMessage()
        );

        // then
        verify(commandSupport).reject(
            new CommunityStompCorrelation(
                MEMBER_ID.toString(),
                COMMAND_ID,
                null,
                CommunityStompCommandType.MESSAGE_EDIT
            ),
            exception
        );
        verify(commandSupport, never()).acknowledge(any(), any(), any());
    }

    private UsernamePasswordAuthenticationToken authentication() {
        return new UsernamePasswordAuthenticationToken(new MemberPrincipal(MEMBER_ID), null, List.of());
    }

    private Message<byte[]> inboundMessage() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setNativeHeader("x-command-id", COMMAND_ID.toString());
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private CommunityStompCommandContext commandContext() {
        return new CommunityStompCommandContext(MEMBER_ID, MEMBER_ID.toString(), COMMAND_ID);
    }

    private CommunityThreadMessageMutationInfo messageMutation(boolean deduplicated) {
        CommunityThreadMessageInfo message = new CommunityThreadMessageInfo(
            MESSAGE_ID,
            THREAD_ID,
            MEMBER_ID,
            "홍길동",
            "안녕하세요",
            CommunityThreadMessageType.TEXT,
            CommunityThreadMessageStatus.SENT,
            List.of(),
            List.of(),
            null,
            List.of(),
            CLIENT_MESSAGE_ID,
            Instant.EPOCH,
            null,
            null
        );
        return new CommunityThreadMessageMutationInfo(message, deduplicated);
    }
}
