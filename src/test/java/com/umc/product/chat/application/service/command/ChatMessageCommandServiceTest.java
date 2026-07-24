package com.umc.product.chat.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.chat.application.authorization.ChatPolicyAction;
import com.umc.product.chat.application.policy.ChatAttachmentPolicy;
import com.umc.product.chat.application.policy.ChatRoomAccessPolicy;
import com.umc.product.chat.application.port.in.command.UpdateChatReadUseCase;
import com.umc.product.chat.application.port.in.command.dto.SendChatMessageCommand;
import com.umc.product.chat.application.port.in.query.dto.ChatMessageInfo;
import com.umc.product.chat.application.port.out.LoadChatMessagePort;
import com.umc.product.chat.application.port.out.LoadChatRoomPort;
import com.umc.product.chat.application.port.out.SaveChatMemberPort;
import com.umc.product.chat.application.port.out.SaveChatMessagePort;
import com.umc.product.chat.domain.ChatMessage;
import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.chat.domain.event.ChatMessageCreatedEvent;
import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;
import com.umc.product.storage.application.port.in.query.dto.FileMetadataInfo;
import com.umc.product.storage.domain.enums.FileCategory;
import com.umc.product.storage.domain.exception.StorageErrorCode;
import com.umc.product.storage.domain.exception.StorageException;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatMessageCommandService")
class ChatMessageCommandServiceTest {

    @Mock
    SaveChatMessagePort saveChatMessagePort;
    @Mock
    LoadChatMessagePort loadChatMessagePort;
    @Mock
    LoadChatRoomPort loadChatRoomPort;
    @Mock
    SaveChatMemberPort saveChatMemberPort;
    @Mock
    GetFileUseCase getFileUseCase;
    @Mock
    ChatAttachmentPolicy chatAttachmentPolicy;
    @Mock
    ChatRoomAccessPolicy chatRoomAccessPolicy;
    @Mock
    UpdateChatReadUseCase updateChatReadUseCase;
    @Mock
    DomainEventPublisher domainEventPublisher;

    @InjectMocks
    ChatMessageCommandService sut;

    @Test
    @DisplayName("정상 전송 시 메시지를 저장하고 생성 이벤트를 발행한다")
    void send_success() {
        SendChatMessageCommand command =
            new SendChatMessageCommand(1L, 10L, MessageContentType.TEXT, "안녕", null);
        ChatMessage saved = ChatMessage.create(1L, 10L, MessageContentType.TEXT, "안녕", null);
        ReflectionTestUtils.setField(saved, "id", 100L);
        Instant createdAt = Instant.parse("2026-06-13T00:00:00Z");
        ReflectionTestUtils.setField(saved, "createdAt", createdAt);
        given(saveChatMessagePort.save(any(ChatMessage.class))).willReturn(saved);

        ChatMessageInfo result = sut.send(command);

        assertThat(result.messageId()).isEqualTo(100L);
        assertThat(result.roomId()).isEqualTo(1L);
        assertThat(result.content()).isEqualTo("안녕");
        assertThat(result.replyToMessageId()).isNull();

        // 방 row 락을 잡은 뒤 저장한다(동시 전송 직렬화)
        then(loadChatRoomPort).should().getByIdForUpdate(1L);
        then(saveChatMessagePort).should().save(any(ChatMessage.class));
        InOrder lockOrder = inOrder(loadChatRoomPort, chatRoomAccessPolicy, saveChatMessagePort);
        lockOrder.verify(loadChatRoomPort).getByIdForUpdate(1L);
        lockOrder.verify(chatRoomAccessPolicy)
            .verifyMember(ChatPolicyAction.MESSAGE_CREATE, 1L, 10L);
        lockOrder.verify(saveChatMessagePort).save(any(ChatMessage.class));
        // 발신자 읽음 위치는 원자 단조 갱신으로 처리한다
        then(saveChatMemberPort).should().bumpLastReadMessageId(1L, 10L, 100L);

        ArgumentCaptor<ChatMessageCreatedEvent> captor = ArgumentCaptor.forClass(ChatMessageCreatedEvent.class);
        then(domainEventPublisher).should().publish(captor.capture());
        assertThat(captor.getValue().messageId()).isEqualTo(100L);
        assertThat(captor.getValue().roomId()).isEqualTo(1L);
        assertThat(captor.getValue().senderMemberId()).isEqualTo(10L);
        assertThat(captor.getValue().replyToMessageId()).isNull();
        // 이벤트 발생 시각은 메시지의 저장 시각(createdAt)과 일치해야 한다
        assertThat(captor.getValue().occurredAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("답장 대상이 같은 방에 있으면 답장 메시지를 저장하고 생성 이벤트에 포함한다")
    void send_reply_success() {
        SendChatMessageCommand command =
            new SendChatMessageCommand(1L, 10L, MessageContentType.TEXT, "답장", null, 90L);
        ChatMessage saved = ChatMessage.create(1L, 10L, MessageContentType.TEXT, "답장", null, 90L);
        ReflectionTestUtils.setField(saved, "id", 100L);
        Instant createdAt = Instant.parse("2026-06-13T00:00:00Z");
        ReflectionTestUtils.setField(saved, "createdAt", createdAt);
        given(loadChatMessagePort.existsByIdAndRoomId(90L, 1L)).willReturn(true);
        given(saveChatMessagePort.save(any(ChatMessage.class))).willReturn(saved);

        ChatMessageInfo result = sut.send(command);

        assertThat(result.replyToMessageId()).isEqualTo(90L);

        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        then(saveChatMessagePort).should().save(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getReplyToMessageId()).isEqualTo(90L);

        ArgumentCaptor<ChatMessageCreatedEvent> eventCaptor = ArgumentCaptor.forClass(ChatMessageCreatedEvent.class);
        then(domainEventPublisher).should().publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().replyToMessageId()).isEqualTo(90L);
    }

    @Test
    @DisplayName("답장 대상이 같은 방에 없으면 전송할 수 없고 저장/발행하지 않는다")
    void send_invalidReplyTarget() {
        SendChatMessageCommand command =
            new SendChatMessageCommand(1L, 10L, MessageContentType.TEXT, "답장", null, 90L);
        given(loadChatMessagePort.existsByIdAndRoomId(90L, 1L)).willReturn(false);

        assertThatThrownBy(() -> sut.send(command))
            .isInstanceOf(ChatDomainException.class)
            .extracting(e -> ((ChatDomainException) e).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_MESSAGE_INVALID_REPLY_TARGET);

        then(saveChatMessagePort).shouldHaveNoInteractions();
        then(domainEventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("SYSTEM 타입은 전송할 수 없다")
    void send_systemRejected() {
        SendChatMessageCommand command =
            new SendChatMessageCommand(1L, 10L, MessageContentType.SYSTEM, "x", null);

        assertThatThrownBy(() -> sut.send(command))
            .isInstanceOf(ChatDomainException.class)
            .extracting(e -> ((ChatDomainException) e).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_MESSAGE_INVALID_CONTENT_TYPE);

        then(saveChatMessagePort).shouldHaveNoInteractions();
        then(domainEventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("내용과 첨부가 모두 비어 있으면 전송할 수 없다")
    void send_emptyRejected() {
        SendChatMessageCommand command =
            new SendChatMessageCommand(1L, 10L, MessageContentType.TEXT, "   ", List.of());

        assertThatThrownBy(() -> sut.send(command))
            .isInstanceOf(ChatDomainException.class)
            .extracting(e -> ((ChatDomainException) e).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_MESSAGE_EMPTY);

        then(saveChatMessagePort).shouldHaveNoInteractions();
        then(domainEventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("IMAGE 메시지에 첨부파일이 없으면 전송할 수 없다")
    void send_imageWithoutAttachmentRejected() {
        SendChatMessageCommand command =
            new SendChatMessageCommand(1L, 10L, MessageContentType.IMAGE, "캡션", List.of());

        assertThatThrownBy(() -> sut.send(command))
            .isInstanceOf(ChatDomainException.class)
            .extracting(e -> ((ChatDomainException)e).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_MESSAGE_ATTACHMENT_REQUIRED);

        then(saveChatMessagePort).shouldHaveNoInteractions();
        then(domainEventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("TEXT 메시지에는 파일을 첨부할 수 없다")
    void send_textWithAttachmentRejected() {
        SendChatMessageCommand command =
            new SendChatMessageCommand(1L, 10L, MessageContentType.TEXT, "본문", List.of("file-1"));

        assertThatThrownBy(() -> sut.send(command))
            .isInstanceOf(ChatDomainException.class)
            .extracting(e -> ((ChatDomainException)e).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_MESSAGE_ATTACHMENT_NOT_ALLOWED);

        then(getFileUseCase).shouldHaveNoInteractions();
        then(saveChatMessagePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("중복된 파일 ID는 첨부할 수 없다")
    void send_duplicateAttachmentRejected() {
        SendChatMessageCommand command = new SendChatMessageCommand(
            1L,
            10L,
            MessageContentType.IMAGE,
            null,
            List.of("file-1", "file-1")
        );

        assertThatThrownBy(() -> sut.send(command))
            .isInstanceOf(ChatDomainException.class)
            .extracting(e -> ((ChatDomainException)e).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_MESSAGE_INVALID_ATTACHMENT);

        then(getFileUseCase).shouldHaveNoInteractions();
        then(saveChatMessagePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("여러 개의 유효한 첨부파일은 개수 제한 없이 검증 후 저장한다")
    void send_multipleAttachmentsSuccess() {
        List<String> fileIds = List.of("file-1", "file-2");
        List<FileMetadataInfo> files = List.of(
            fileMetadataInfo("file-1", "jpg", "image/jpeg"),
            fileMetadataInfo("file-2", "png", "image/png")
        );
        SendChatMessageCommand command =
            new SendChatMessageCommand(1L, 10L, MessageContentType.IMAGE, "캡션", fileIds);
        ChatMessage saved = ChatMessage.create(1L, 10L, MessageContentType.IMAGE, "캡션", fileIds);
        ReflectionTestUtils.setField(saved, "id", 100L);
        ReflectionTestUtils.setField(saved, "createdAt", Instant.parse("2026-06-13T00:00:00Z"));
        given(getFileUseCase.batchGetUsableByIds(fileIds, 10L)).willReturn(files);
        given(saveChatMessagePort.save(any(ChatMessage.class))).willReturn(saved);

        ChatMessageInfo result = sut.send(command);

        assertThat(result.fileMetadataIds()).containsExactly("file-1", "file-2");
        then(chatAttachmentPolicy).should().validate(MessageContentType.IMAGE, files);
        then(saveChatMessagePort).should().save(any(ChatMessage.class));
        then(domainEventPublisher).should().publish(any(ChatMessageCreatedEvent.class));
    }

    @Test
    @DisplayName("첨부파일이 storage 검증을 통과하지 못하면 저장하거나 이벤트를 발행하지 않는다")
    void send_invalidStorageFileRejected() {
        List<String> fileIds = List.of("file-1");
        SendChatMessageCommand command =
            new SendChatMessageCommand(1L, 10L, MessageContentType.IMAGE, null, fileIds);
        willThrow(new StorageException(StorageErrorCode.FILE_USE_FORBIDDEN))
            .given(getFileUseCase).batchGetUsableByIds(fileIds, 10L);

        assertThatThrownBy(() -> sut.send(command))
            .isInstanceOf(StorageException.class)
            .extracting(e -> ((StorageException)e).getBaseCode())
            .isEqualTo(StorageErrorCode.FILE_USE_FORBIDDEN);

        then(chatAttachmentPolicy).shouldHaveNoInteractions();
        then(saveChatMessagePort).shouldHaveNoInteractions();
        then(domainEventPublisher).shouldHaveNoInteractions();
    }

    private FileMetadataInfo fileMetadataInfo(String fileId, String extension, String contentType) {
        return new FileMetadataInfo(
            fileId,
            "file." + extension,
            extension,
            FileCategory.ETC,
            contentType,
            1024L,
            true,
            10L,
            null
        );
    }
}
