package com.umc.product.chat.application.service.command;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.chat.application.policy.ChatAttachmentPolicy;
import com.umc.product.chat.application.policy.ChatRoomAccessPolicy;
import com.umc.product.chat.application.port.in.command.MarkChatRoomReadUseCase;
import com.umc.product.chat.application.port.in.command.SendChatMessageUseCase;
import com.umc.product.chat.application.port.in.command.UpdateChatReadUseCase;
import com.umc.product.chat.application.port.in.command.dto.MarkChatRoomReadCommand;
import com.umc.product.chat.application.port.in.command.dto.SendChatMessageCommand;
import com.umc.product.chat.application.port.in.command.dto.UpdateChatReadCommand;
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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatMessageCommandService implements SendChatMessageUseCase, MarkChatRoomReadUseCase {

    private final SaveChatMessagePort saveChatMessagePort;
    private final LoadChatMessagePort loadChatMessagePort;
    private final LoadChatRoomPort loadChatRoomPort;
    private final SaveChatMemberPort saveChatMemberPort;
    private final GetFileUseCase getFileUseCase;
    private final ChatAttachmentPolicy chatAttachmentPolicy;
    private final ChatRoomAccessPolicy chatRoomAccessPolicy;
    private final UpdateChatReadUseCase updateChatReadUseCase;
    private final DomainEventPublisher domainEventPublisher;

    /**
     * 메시지를 저장하고 생성 이벤트를 발행한다.
     * <p>
     * broadcast 및 문의 상태 전환은 이 이벤트를 수신하는 다른 컴포넌트가 처리한다. chat은 알지 못한다. 이벤트 발행은 {@link DomainEventPublisher} 한 곳에만 위임하며,
     * 이벤트는 원 트랜잭션의 outbox에 저장된다.
     */
    @Override
    public ChatMessageInfo send(SendChatMessageCommand command) {
        validate(command);

        // 같은 방의 동시 전송을 직렬화한다(방 row 락). insert 이전에 락을 잡아야 방 안에서 message id 배정
        // 순서가 commit 순서와 일치하고, 그 결과 읽음 watermark(id 기준)가 안전해진다.
        loadChatRoomPort.getByIdForUpdate(command.roomId());
        chatRoomAccessPolicy.verifyMember(command.roomId(), command.senderMemberId());
        validateReplyTarget(command);
        validateAttachments(command);

        ChatMessage saved = saveChatMessagePort.save(ChatMessage.create(
            command.roomId(),
            command.senderMemberId(),
            command.contentType(),
            command.content(),
            command.fileMetadataIds(),
            command.replyToMessageId()
        ));

        markSenderRead(command, saved);

        domainEventPublisher.publish(ChatMessageCreatedEvent.from(saved));

        return ChatMessageInfo.from(saved);
    }

    @Override
    public void markRead(MarkChatRoomReadCommand command) {
        updateChatReadUseCase.update(new UpdateChatReadCommand(
            command.roomId(),
            command.memberId(),
            command.lastSeenMessageId()
        ));
    }

    private void validate(SendChatMessageCommand command) {
        // SYSTEM 메시지는 서버 내부에서만 생성한다(클라이언트 전송 불가).
        if (command.contentType() == null || command.contentType() == MessageContentType.SYSTEM) {
            throw new ChatDomainException(ChatErrorCode.CHAT_MESSAGE_INVALID_CONTENT_TYPE);
        }
        boolean noContent = command.content() == null || command.content().isBlank();
        boolean noFiles = command.fileMetadataIds() == null || command.fileMetadataIds().isEmpty();
        if (command.contentType() == MessageContentType.TEXT && !noFiles) {
            throw new ChatDomainException(ChatErrorCode.CHAT_MESSAGE_ATTACHMENT_NOT_ALLOWED);
        }
        if ((command.contentType() == MessageContentType.IMAGE || command.contentType() == MessageContentType.FILE)
            && noFiles) {
            throw new ChatDomainException(ChatErrorCode.CHAT_MESSAGE_ATTACHMENT_REQUIRED);
        }
        if (noContent && noFiles) {
            throw new ChatDomainException(ChatErrorCode.CHAT_MESSAGE_EMPTY);
        }
        if (!noFiles && hasInvalidFileId(command.fileMetadataIds())) {
            throw new ChatDomainException(ChatErrorCode.CHAT_MESSAGE_INVALID_ATTACHMENT);
        }
    }

    private boolean hasInvalidFileId(List<String> fileIds) {
        return fileIds.stream().anyMatch(fileId -> fileId == null || fileId.isBlank())
            || fileIds.stream().distinct().count() != fileIds.size();
    }

    private void validateAttachments(SendChatMessageCommand command) {
        if (command.fileMetadataIds() == null || command.fileMetadataIds().isEmpty()) {
            return;
        }

        List<FileMetadataInfo> files = getFileUseCase.batchGetUsableByIds(
            command.fileMetadataIds(),
            command.senderMemberId()
        );
        chatAttachmentPolicy.validate(command.contentType(), files);
    }

    private void markSenderRead(SendChatMessageCommand command, ChatMessage saved) {
        // 발신자는 자기 메시지를 읽은 것으로 처리한다. markRead 와 동일하게 원자 단조 갱신을 사용해
        // 다른 기기의 동시 읽음 처리와 lost update 가 나지 않도록 한다.
        saveChatMemberPort.bumpLastReadMessageId(command.roomId(), command.senderMemberId(), saved.getId());
    }

    private void validateReplyTarget(SendChatMessageCommand command) {
        Long replyToMessageId = command.replyToMessageId();
        if (replyToMessageId == null) {
            return;
        }
        if (!loadChatMessagePort.existsByIdAndRoomId(replyToMessageId, command.roomId())) {
            throw new ChatDomainException(ChatErrorCode.CHAT_MESSAGE_INVALID_REPLY_TARGET);
        }
    }
}
