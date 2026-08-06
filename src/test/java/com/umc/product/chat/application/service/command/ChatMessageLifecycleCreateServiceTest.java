package com.umc.product.chat.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.chat.application.policy.ChatMessagePayloadFingerprint;
import com.umc.product.chat.application.policy.ChatRoomAccessPolicy;
import com.umc.product.chat.application.policy.CommunityChatMessagePolicy;
import com.umc.product.chat.application.port.in.command.dto.ChatMessageMutationResult;
import com.umc.product.chat.application.port.in.command.dto.CreateChatMessageCommand;
import com.umc.product.chat.application.port.in.query.dto.ChatMessageInfo;
import com.umc.product.chat.application.port.out.LoadChatMemberPort;
import com.umc.product.chat.application.port.out.LoadChatMessageMentionPort;
import com.umc.product.chat.application.port.out.LoadChatMessagePort;
import com.umc.product.chat.application.port.out.LoadChatRoomPort;
import com.umc.product.chat.application.port.out.SaveChatMemberPort;
import com.umc.product.chat.application.port.out.SaveChatMessageMentionPort;
import com.umc.product.chat.application.port.out.SaveChatMessagePort;
import com.umc.product.chat.application.port.out.SaveChatMessageReactionPort;
import com.umc.product.chat.application.service.query.ChatMessageInfoAssembler;
import com.umc.product.chat.domain.ChatMember;
import com.umc.product.chat.domain.ChatMessage;
import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.chat.domain.event.ChatMessageCreatedEvent;
import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatMessageLifecycleCommandService.create")
class ChatMessageLifecycleCreateServiceTest {

    @Mock
    SaveChatMessagePort saveChatMessagePort;
    @Mock
    LoadChatMessagePort loadChatMessagePort;
    @Mock
    LoadChatRoomPort loadChatRoomPort;
    @Mock
    LoadChatMemberPort loadChatMemberPort;
    @Mock
    SaveChatMemberPort saveChatMemberPort;
    @Mock
    LoadChatMessageMentionPort loadChatMessageMentionPort;
    @Mock
    SaveChatMessageMentionPort saveChatMessageMentionPort;
    @Mock
    SaveChatMessageReactionPort saveChatMessageReactionPort;
    @Mock
    CommunityChatMessagePolicy communityChatMessagePolicy;
    @Mock
    ChatRoomAccessPolicy chatRoomAccessPolicy;
    @Mock
    ChatMessageInfoAssembler chatMessageInfoAssembler;
    @Mock
    DomainEventPublisher domainEventPublisher;

    @InjectMocks
    ChatMessageLifecycleCommandService sut;

    @Test
    @DisplayName("room lock 아래에서 멤버십과 멘션과 idempotency를 재검증한 뒤 한 번만 생성한다")
    void create_fresh() {
        UUID clientMessageId = UUID.fromString("6f2189d2-b356-4cd0-877f-c16558bbca98");
        CreateChatMessageCommand command = text(clientMessageId, List.of(30L, 20L, 30L));
        ChatMessage saved = savedText(100L, clientMessageId, "본문", null);
        ChatMessageInfo info = ChatMessageInfo.from(saved);
        given(loadChatMemberPort.listByRoomId(1L)).willReturn(List.of(
            ChatMember.of(1L, 10L),
            ChatMember.of(1L, 20L),
            ChatMember.of(1L, 30L)
        ));
        given(loadChatMessagePort.findByRoomIdAndSenderMemberIdAndClientMessageId(1L, 10L, clientMessageId))
            .willReturn(Optional.empty());
        given(saveChatMessagePort.save(any(ChatMessage.class))).willReturn(saved);
        given(chatMessageInfoAssembler.assemble(saved, 10L)).willReturn(info);

        ChatMessageMutationResult result = sut.create(command);

        assertThat(result.deduplicated()).isFalse();
        assertThat(command.mentionedMemberIds()).containsExactly(20L, 30L);
        ArgumentCaptor<ChatMessage> persisted = ArgumentCaptor.forClass(ChatMessage.class);
        then(saveChatMessagePort).should().save(persisted.capture());
        assertThat(persisted.getValue().getClientPayloadFingerprint()).hasSize(64);
        then(saveChatMessageMentionPort).should().saveAll(100L, List.of(20L, 30L));
        then(saveChatMemberPort).should().bumpLastReadMessageId(1L, 10L, 100L);
        ArgumentCaptor<ChatMessageCreatedEvent> event = ArgumentCaptor.forClass(ChatMessageCreatedEvent.class);
        then(domainEventPublisher).should().publish(event.capture());
        assertThat(event.getValue().clientMessageId()).isEqualTo(clientMessageId);
        assertThat(event.getValue().mentionedMemberIds()).containsExactly(20L, 30L);

        InOrder validationOrder = inOrder(
            loadChatRoomPort,
            chatRoomAccessPolicy,
            loadChatMemberPort,
            loadChatMessagePort,
            saveChatMessagePort
        );
        validationOrder.verify(loadChatRoomPort).getByIdForUpdate(1L);
        validationOrder.verify(chatRoomAccessPolicy).verifyMember(1L, 10L);
        validationOrder.verify(loadChatMemberPort).listByRoomId(1L);
        validationOrder.verify(loadChatMessagePort)
            .findByRoomIdAndSenderMemberIdAndClientMessageId(1L, 10L, clientMessageId);
        validationOrder.verify(saveChatMessagePort).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("mutation 후에도 동일 clientMessageId와 canonical create payload 재시도는 기존 메시지를 반환한다")
    void create_samePayloadReplay() {
        UUID clientMessageId = UUID.fromString("bbf25844-28b9-4f35-8552-119e3a24e89a");
        CreateChatMessageCommand command = new CreateChatMessageCommand(
            1L,
            10L,
            clientMessageId,
            MessageContentType.IMAGE,
            "캡션",
            List.of("file-b", "file-a"),
            List.of(30L, 20L),
            90L
        );
        ChatMessage existing = saved(
            100L,
            clientMessageId,
            MessageContentType.IMAGE,
            "캡션",
            List.of("file-b", "file-a"),
            90L
        );
        ReflectionTestUtils.setField(
            existing,
            "clientPayloadFingerprint",
            ChatMessagePayloadFingerprint.from(command)
        );
        existing.tombstone();
        ChatMessageInfo info = ChatMessageInfo.from(existing);
        given(loadChatMemberPort.listByRoomId(1L)).willReturn(List.of(
            ChatMember.of(1L, 10L),
            ChatMember.of(1L, 20L),
            ChatMember.of(1L, 30L)
        ));
        given(loadChatMessagePort.existsByIdAndRoomId(90L, 1L)).willReturn(true);
        given(loadChatMessagePort.findByRoomIdAndSenderMemberIdAndClientMessageId(1L, 10L, clientMessageId))
            .willReturn(Optional.of(existing));
        given(loadChatMessageMentionPort.listMemberIdsByMessageId(100L)).willReturn(List.of());
        given(chatMessageInfoAssembler.assemble(existing, 10L)).willReturn(info);

        ChatMessageMutationResult result = sut.create(command);

        assertThat(result.deduplicated()).isTrue();
        assertThat(result.message()).isSameAs(info);
        then(communityChatMessagePolicy).should(never()).validateAttachments(command);
        then(saveChatMessagePort).shouldHaveNoInteractions();
        then(saveChatMessageMentionPort).shouldHaveNoInteractions();
        then(saveChatMemberPort).shouldHaveNoInteractions();
        then(domainEventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("같은 clientMessageId라도 파일 순서가 다르면 conflict로 거부한다")
    void create_differentOrderedFilesConflict() {
        UUID clientMessageId = UUID.fromString("db46c057-5f4f-41ec-b1da-6a21144ffacd");
        CreateChatMessageCommand command = new CreateChatMessageCommand(
            1L,
            10L,
            clientMessageId,
            MessageContentType.IMAGE,
            null,
            List.of("file-b", "file-a"),
            List.of(),
            null
        );
        ChatMessage existing = saved(
            100L,
            clientMessageId,
            MessageContentType.IMAGE,
            null,
            List.of("file-a", "file-b"),
            null
        );
        given(loadChatMessagePort.findByRoomIdAndSenderMemberIdAndClientMessageId(1L, 10L, clientMessageId))
            .willReturn(Optional.of(existing));
        given(loadChatMessageMentionPort.listMemberIdsByMessageId(100L)).willReturn(List.of());

        assertThatThrownBy(() -> sut.create(command))
            .isInstanceOf(ChatDomainException.class)
            .extracting(error -> ((ChatDomainException) error).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_MESSAGE_IDEMPOTENCY_CONFLICT);

        then(saveChatMessagePort).shouldHaveNoInteractions();
        then(domainEventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("멘션 대상이 잠금 시점에 멤버가 아니면 idempotency lookup 전에 거부한다")
    void create_mentionLeftBeforeLock() {
        UUID clientMessageId = UUID.fromString("208bc80a-45c8-49bd-81dd-b578cb51d93c");
        CreateChatMessageCommand command = text(clientMessageId, List.of(20L));
        given(loadChatMemberPort.listByRoomId(1L)).willReturn(List.of(ChatMember.of(1L, 10L)));

        assertThatThrownBy(() -> sut.create(command))
            .isInstanceOf(ChatDomainException.class)
            .extracting(error -> ((ChatDomainException) error).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_MESSAGE_INVALID_MENTION);

        then(loadChatMessagePort).should(never())
            .findByRoomIdAndSenderMemberIdAndClientMessageId(1L, 10L, clientMessageId);
        then(saveChatMessagePort).shouldHaveNoInteractions();
    }

    private CreateChatMessageCommand text(UUID clientMessageId, List<Long> mentions) {
        return new CreateChatMessageCommand(
            1L,
            10L,
            clientMessageId,
            MessageContentType.TEXT,
            "본문",
            List.of(),
            mentions,
            null
        );
    }

    private ChatMessage savedText(Long id, UUID clientMessageId, String content, Long replyToId) {
        return saved(id, clientMessageId, MessageContentType.TEXT, content, List.of(), replyToId);
    }

    private ChatMessage saved(
        Long id,
        UUID clientMessageId,
        MessageContentType type,
        String content,
        List<String> files,
        Long replyToId
    ) {
        ChatMessage message = ChatMessage.create(
            1L,
            10L,
            type,
            content,
            files,
            replyToId,
            clientMessageId,
            "0".repeat(64)
        );
        ReflectionTestUtils.setField(message, "id", id);
        ReflectionTestUtils.setField(message, "createdAt", Instant.parse("2026-07-18T00:00:00Z"));
        return message;
    }
}
