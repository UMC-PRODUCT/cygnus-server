package com.umc.product.storage.adapter.in.graphql.dto;

import com.umc.product.storage.application.port.in.command.dto.PrepareFileUploadCommand;
import com.umc.product.storage.domain.enums.FileCategory;

public record PrepareFileUploadGraphQlRequest(
    String fileName,
    String contentType,
    Long fileSize,
    FileCategory category
) {

    public PrepareFileUploadCommand toCommand(Long uploadedBy) {
        return new PrepareFileUploadCommand(fileName, contentType, fileSize, category, uploadedBy);
    }
}
