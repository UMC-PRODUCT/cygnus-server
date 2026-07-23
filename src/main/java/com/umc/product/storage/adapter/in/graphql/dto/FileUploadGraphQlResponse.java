package com.umc.product.storage.adapter.in.graphql.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.umc.product.storage.application.port.in.command.dto.FileUploadInfo;

public record FileUploadGraphQlResponse(
    String fileId,
    String uploadUrl,
    String uploadMethod,
    List<Header> headers,
    LocalDateTime expiresAt
) {

    public static FileUploadGraphQlResponse from(FileUploadInfo info) {
        List<Header> headers = info.headers().entrySet().stream()
            .sorted(java.util.Map.Entry.comparingByKey())
            .map(entry -> new Header(entry.getKey(), entry.getValue()))
            .toList();
        return new FileUploadGraphQlResponse(
            info.fileId(),
            info.uploadUrl(),
            info.uploadMethod(),
            headers,
            info.expiresAt()
        );
    }

    public record Header(String name, String value) {
    }
}
