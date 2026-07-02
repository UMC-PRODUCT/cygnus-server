package com.umc.product.storage.application.port.in.command.dto;

import java.util.Objects;

import com.umc.product.storage.domain.enums.FileCategory;

public record StoreGeneratedFileCommand(
    String fileName,
    String contentType,
    byte[] content,
    FileCategory category,
    Long generatedByMemberId
) {

    public StoreGeneratedFileCommand {
        Objects.requireNonNull(fileName, "fileName must not be null");
        Objects.requireNonNull(contentType, "contentType must not be null");
        Objects.requireNonNull(content, "content must not be null");
        Objects.requireNonNull(category, "category must not be null");
        if (content.length == 0) {
            throw new IllegalArgumentException("content must not be empty");
        }
    }

    public static StoreGeneratedFileCommand of(
        String fileName,
        String contentType,
        byte[] content,
        FileCategory category,
        Long generatedByMemberId
    ) {
        return new StoreGeneratedFileCommand(fileName, contentType, content, category, generatedByMemberId);
    }

    public long fileSize() {
        return content.length;
    }
}
