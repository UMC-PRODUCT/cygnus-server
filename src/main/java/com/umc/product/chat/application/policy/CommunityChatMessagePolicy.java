package com.umc.product.chat.application.policy;

import java.util.List;

import org.springframework.stereotype.Component;

import com.umc.product.chat.application.port.in.command.dto.CreateChatMessageCommand;
import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;
import com.umc.product.storage.application.port.in.query.dto.FileMetadataInfo;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CommunityChatMessagePolicy {

    private static final int MAX_CONTENT_CODE_POINTS = 2_000;
    private static final int MAX_MENTIONS = 100;
    private static final int MAX_IMAGE_COUNT = 4;
    private static final long MAX_IMAGE_SIZE = 10L * 1024L * 1024L;
    private static final long MAX_TOTAL_IMAGE_SIZE = 40L * 1024L * 1024L;

    private final GetFileUseCase getFileUseCase;
    private final ChatAttachmentPolicy chatAttachmentPolicy;

    public void validateCreate(CreateChatMessageCommand command) {
        if (command.clientMessageId() == null) {
            throw new ChatDomainException(ChatErrorCode.CHAT_MESSAGE_CLIENT_ID_REQUIRED);
        }
        if (command.contentType() != MessageContentType.TEXT
            && command.contentType() != MessageContentType.IMAGE) {
            throw new ChatDomainException(ChatErrorCode.CHAT_MESSAGE_INVALID_CONTENT_TYPE);
        }
        validateContentLength(command.content());
        validateMentions(command.mentionedMemberIds());

        if (command.contentType() == MessageContentType.TEXT) {
            validateText(command.content(), command.fileMetadataIds());
        } else {
            validateImage(command.fileMetadataIds());
        }
    }

    public void validateEdit(String content) {
        validateContentLength(content);
    }

    public void validateAttachments(CreateChatMessageCommand command) {
        if (command.contentType() != MessageContentType.IMAGE) {
            return;
        }
        List<FileMetadataInfo> files = getFileUseCase.batchGetUsableByIds(
            command.fileMetadataIds(),
            command.senderMemberId()
        );
        chatAttachmentPolicy.validate(MessageContentType.IMAGE, files);

        long totalSize = 0L;
        for (FileMetadataInfo file : files) {
            if (file.fileSize() == null) {
                throw new ChatDomainException(ChatErrorCode.CHAT_MESSAGE_INVALID_ATTACHMENT);
            }
            if (file.fileSize() > MAX_IMAGE_SIZE) {
                throw new ChatDomainException(ChatErrorCode.CHAT_MESSAGE_ATTACHMENT_TOO_LARGE);
            }
            totalSize += file.fileSize();
        }
        if (totalSize > MAX_TOTAL_IMAGE_SIZE) {
            throw new ChatDomainException(ChatErrorCode.CHAT_MESSAGE_ATTACHMENT_TOO_LARGE);
        }
    }

    private void validateContentLength(String content) {
        if (content != null && content.codePointCount(0, content.length()) > MAX_CONTENT_CODE_POINTS) {
            throw new ChatDomainException(ChatErrorCode.CHAT_MESSAGE_INVALID_CONTENT_LENGTH);
        }
    }

    private void validateMentions(List<Long> mentionedMemberIds) {
        if (mentionedMemberIds.size() > MAX_MENTIONS) {
            throw new ChatDomainException(ChatErrorCode.CHAT_MESSAGE_INVALID_MENTION);
        }
    }

    private void validateText(String content, List<String> fileMetadataIds) {
        if (content == null || content.isBlank()) {
            throw new ChatDomainException(ChatErrorCode.CHAT_MESSAGE_EMPTY);
        }
        if (!fileMetadataIds.isEmpty()) {
            throw new ChatDomainException(ChatErrorCode.CHAT_MESSAGE_ATTACHMENT_NOT_ALLOWED);
        }
    }

    private void validateImage(List<String> fileMetadataIds) {
        if (fileMetadataIds.isEmpty() || fileMetadataIds.size() > MAX_IMAGE_COUNT) {
            throw new ChatDomainException(ChatErrorCode.CHAT_MESSAGE_INVALID_ATTACHMENT_COUNT);
        }
        if (fileMetadataIds.stream().distinct().count() != fileMetadataIds.size()) {
            throw new ChatDomainException(ChatErrorCode.CHAT_MESSAGE_INVALID_ATTACHMENT);
        }
    }
}
