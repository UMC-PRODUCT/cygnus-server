package com.umc.product.chat.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.chat.application.policy.ChatRoomAccessPolicy;
import com.umc.product.chat.application.policy.CommunityChatMessagePolicy;
import com.umc.product.chat.application.port.in.command.dto.ChatMessageMutationResult;
import com.umc.product.chat.application.port.in.command.dto.EditChatMessageCommand;
import com.umc.product.chat.application.port.in.command.dto.TombstoneChatMessageCommand;
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
import com.umc.product.chat.domain.ChatMessage;
import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.chat.domain.event.ChatMessageDeletedEvent;
import com.umc.product.chat.domain.event.ChatMessageUpdatedEvent;
import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatMessageLifecycleCommandService edit/tombstone")
class ChatMessageLifecycleMutationServiceTest {

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
    @DisplayName("작성자는 내용만 수정하고 updated 이벤트를 발행한다")
    void edit_authorSuccess() {
        ChatMessage message = message(MessageContentType.TEXT, "이전", List.of(), 90L);
        ChatMessageInfo info = ChatMessageInfo.from(message);
        given(loadChatMessagePort.getByIdAndRoomId(100L, 1L)).willReturn(message);
        given(saveChatMessagePort.save(message)).willReturn(message);
        given(loadChatMessageMentionPort.listMemberIdsByMessageId(100L)).willReturn(List.of(20L));
        given(chatMessageInfoAssembler.assemble(message, 10L)).willReturn(info);

        ChatMessageMutationResult result = sut.edit(new EditChatMessageCommand(1L, 100L, 10L, "수정"));

        assertThat(result.deduplicated()).isFalse();
        assertThat(message.getContent()).isEqualTo("수정");
        assertThat(message.getEditedAt()).isNotNull();
        ArgumentCaptor<ChatMessageUpdatedEvent> event = ArgumentCaptor.forClass(ChatMessageUpdatedEvent.class);
        then(domainEventPublisher).should().publish(event.capture());
        assertThat(event.getValue().eventId()).isNotNull();
        assertThat(event.getValue().message().mentionedMemberIds()).containsExactly(20L);
    }

    @Test
    @DisplayName("같은 내용 수정은 timestamp와 이벤트를 바꾸지 않는 no-op이다")
    void edit_sameContentDeduplicated() {
        ChatMessage message = message(MessageContentType.TEXT, "같은 내용", List.of(), null);
        ChatMessageInfo info = ChatMessageInfo.from(message);
        given(loadChatMessagePort.getByIdAndRoomId(100L, 1L)).willReturn(message);
        given(chatMessageInfoAssembler.assemble(message, 10L)).willReturn(info);

        ChatMessageMutationResult result = sut.edit(
            new EditChatMessageCommand(1L, 100L, 10L, "같은 내용")
        );

        assertThat(result.deduplicated()).isTrue();
        assertThat(message.getEditedAt()).isNull();
        then(saveChatMessagePort).shouldHaveNoInteractions();
        then(domainEventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("작성자가 아닌 멤버는 수정할 수 없다")
    void edit_notAuthor() {
        ChatMessage message = message(MessageContentType.TEXT, "본문", List.of(), null);
        given(loadChatMessagePort.getByIdAndRoomId(100L, 1L)).willReturn(message);

        assertThatThrownBy(() -> sut.edit(new EditChatMessageCommand(1L, 100L, 20L, "수정")))
            .isInstanceOf(ChatDomainException.class)
            .extracting(error -> ((ChatDomainException) error).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_MESSAGE_MUTATION_FORBIDDEN);

        then(saveChatMessagePort).shouldHaveNoInteractions();
        then(domainEventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("moderator tombstone은 identity와 reply와 기존 timestamp를 보존하고 부가 상태를 지운다")
    void tombstone_moderatorSuccess() {
        ChatMessage message = message(MessageContentType.IMAGE, "캡션", List.of("file-1"), 90L);
        Instant createdAt = message.getCreatedAt();
        Instant editedAt = Instant.parse("2026-07-18T01:00:00Z");
        ReflectionTestUtils.setField(message, "editedAt", editedAt);
        given(loadChatMessagePort.getByIdAndRoomId(100L, 1L)).willReturn(message);
        given(saveChatMessagePort.save(message)).willReturn(message);
        given(chatMessageInfoAssembler.assemble(message, 99L)).willReturn(ChatMessageInfo.from(message));

        ChatMessageMutationResult result = sut.tombstone(
            new TombstoneChatMessageCommand(1L, 100L, 99L, true)
        );

        assertThat(result.deduplicated()).isFalse();
        assertThat(message.getId()).isEqualTo(100L);
        assertThat(message.getSenderMemberId()).isEqualTo(10L);
        assertThat(message.getReplyToMessageId()).isEqualTo(90L);
        assertThat(message.getCreatedAt()).isEqualTo(createdAt);
        assertThat(message.getEditedAt()).isEqualTo(editedAt);
        assertThat(message.getDeletedAt()).isNotNull();
        assertThat(message.getContentType()).isEqualTo(MessageContentType.SYSTEM);
        assertThat(message.getContent()).isEqualTo(ChatMessage.DELETED_CONTENT);
        assertThat(message.getFileMetadataIds()).isEmpty();
        then(saveChatMessageMentionPort).should().deleteByMessageId(100L);
        then(saveChatMessageReactionPort).should().deleteByMessageId(100L);
        ArgumentCaptor<ChatMessageDeletedEvent> event = ArgumentCaptor.forClass(ChatMessageDeletedEvent.class);
        then(domainEventPublisher).should().publish(event.capture());
        assertThat(event.getValue().eventId()).isNotNull();
        assertThat(event.getValue().message().replyToMessageId()).isEqualTo(90L);
    }

    @Test
    @DisplayName("이미 tombstone인 메시지 삭제 재시도는 부가 상태를 다시 지우지 않는다")
    void tombstone_alreadyDeleted() {
        ChatMessage message = message(MessageContentType.TEXT, "본문", List.of(), null);
        message.tombstone();
        given(loadChatMessagePort.getByIdAndRoomId(100L, 1L)).willReturn(message);
        given(chatMessageInfoAssembler.assemble(message, 10L)).willReturn(ChatMessageInfo.from(message));

        ChatMessageMutationResult result = sut.tombstone(
            new TombstoneChatMessageCommand(1L, 100L, 10L, false)
        );

        assertThat(result.deduplicated()).isTrue();
        then(saveChatMessageMentionPort).shouldHaveNoInteractions();
        then(saveChatMessageReactionPort).shouldHaveNoInteractions();
        then(domainEventPublisher).shouldHaveNoInteractions();
    }

    private ChatMessage message(
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
            UUID.fromString("44354c3f-6f2b-46cc-ae2f-76088893be2e"),
            "0".repeat(64)
        );
        ReflectionTestUtils.setField(message, "id", 100L);
        ReflectionTestUtils.setField(message, "createdAt", Instant.parse("2026-07-18T00:00:00Z"));
        return message;
    }
}
