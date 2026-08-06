package com.umc.product.chat.application.policy;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;
import com.umc.product.storage.application.port.in.query.dto.FileMetadataInfo;

@Component
public class ChatAttachmentPolicy {

    private static final Map<String, Set<String>> IMAGE_TYPES = Map.of(
        "jpg", Set.of("image/jpeg"),
        "jpeg", Set.of("image/jpeg"),
        "png", Set.of("image/png"),
        "webp", Set.of("image/webp"),
        "gif", Set.of("image/gif")
    );

    private static final Map<String, Set<String>> FILE_TYPES = Map.of(
        "pdf", Set.of("application/pdf"),
        "doc", Set.of("application/msword"),
        "docx", Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
        "xls", Set.of("application/vnd.ms-excel"),
        "xlsx", Set.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
        "ppt", Set.of("application/vnd.ms-powerpoint"),
        "pptx", Set.of("application/vnd.openxmlformats-officedocument.presentationml.presentation"),
        "zip", Set.of("application/zip", "application/x-zip-compressed")
    );

    public void validate(MessageContentType contentType, Iterable<FileMetadataInfo> files) {
        Map<String, Set<String>> allowedTypes = switch (contentType) {
            case IMAGE -> IMAGE_TYPES;
            case FILE -> FILE_TYPES;
            default -> throw new ChatDomainException(ChatErrorCode.CHAT_MESSAGE_ATTACHMENT_NOT_ALLOWED);
        };

        for (FileMetadataInfo file : files) {
            if (!isAllowed(file, allowedTypes)) {
                throw new ChatDomainException(ChatErrorCode.CHAT_MESSAGE_INVALID_FILE_TYPE);
            }
        }
    }

    private boolean isAllowed(FileMetadataInfo file, Map<String, Set<String>> allowedTypes) {
        if (file.fileExtension() == null || file.contentType() == null) {
            return false;
        }

        String extension = file.fileExtension().trim().toLowerCase(Locale.ROOT);
        String contentType = extractMediaType(file.contentType());
        return allowedTypes.getOrDefault(extension, Set.of()).contains(contentType);
    }

    private String extractMediaType(String contentType) {
        int parameterIndex = contentType.indexOf(';');
        String mediaType = parameterIndex < 0 ? contentType : contentType.substring(0, parameterIndex);
        return mediaType.trim().toLowerCase(Locale.ROOT);
    }
}
