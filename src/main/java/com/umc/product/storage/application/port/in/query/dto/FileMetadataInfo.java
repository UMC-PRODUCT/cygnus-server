package com.umc.product.storage.application.port.in.query.dto;

import java.time.Instant;

import com.umc.product.storage.domain.FileMetadata;
import com.umc.product.storage.domain.enums.FileCategory;

/**
 * 접근 URL을 생성하지 않는 파일 메타데이터 조회 모델.
 */
public record FileMetadataInfo(
    String fileId,
    String originalFileName,
    String fileExtension,
    FileCategory category,
    String contentType,
    Long fileSize,
    Boolean isUploaded,
    Long uploadedMemberId,
    Instant createdAt
) {

    public static FileMetadataInfo from(FileMetadata fileMetadata) {
        return new FileMetadataInfo(
            fileMetadata.getId(),
            fileMetadata.getOriginalFileName(),
            fileMetadata.getFileExtension(),
            fileMetadata.getCategory(),
            fileMetadata.getContentType(),
            fileMetadata.getFileSize(),
            fileMetadata.isUploaded(),
            fileMetadata.getUploadedMemberId(),
            fileMetadata.getCreatedAt()
        );
    }
}
