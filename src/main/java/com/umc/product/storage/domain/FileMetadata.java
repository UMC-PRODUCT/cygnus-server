package com.umc.product.storage.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.umc.product.common.BaseEntity;
import com.umc.product.storage.domain.enums.FileCategory;
import com.umc.product.storage.domain.enums.StorageProvider;
import com.umc.product.storage.domain.exception.StorageErrorCode;
import com.umc.product.storage.domain.exception.StorageException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 파일 메타데이터 엔티티
 *
 * <p>실제 파일은 AWS S3에 저장되며,
 * 이 엔티티는 파일의 메타 정보와 접근 경로를 관리합니다.
 */
@Entity
@Table(name = "file_metadata")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FileMetadata extends BaseEntity {

    /**
     * 파일 고유 ID (외부 스토리지 key 생성에 사용)
     */
    @Id
    private String id;

    /**
     * 원본 파일명
     */
    @Column(nullable = false)
    private String originalFileName;

    /**
     * 파일 카테고리 (프로필 이미지, 게시글 이미지 등)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private FileCategory category;

    /**
     * Content-Type (MIME type)
     */
    @Column(nullable = false, length = 100)
    private String contentType;

    /**
     * 파일 크기 (bytes)
     */
    @Column(nullable = false)
    private Long fileSize;


    /**
     * 스토리지 제공자
     */
    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private StorageProvider storageProvider;

    /**
     * 스토리지 키 (S3 key)
     */
    @Column(nullable = false, unique = true, length = 500)
    private String storageKey;

    /**
     * 업로드한 사용자 ID (선택)
     */
    @Column
    private Long uploadedMemberId;

    /**
     * 업로드 완료 여부
     */
    @Column(nullable = false)
    private boolean isUploaded = false;

    /**
     * 업로드가 서버에서 확인된 시각. Legacy writer가 만든 row는 업로드 완료여도 null일 수 있습니다.
     */
    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    /**
     * 마지막 usage가 제거되어 미참조 상태가 시작된 시각
     */
    @Column(name = "unreferenced_at")
    private Instant unreferencedAt;

    /**
     * 물리 삭제 claim을 식별하는 CAS token
     */
    @Column(name = "cleanup_claim_token")
    private UUID cleanupClaimToken;

    @Column(name = "cleanup_claimed_at")
    private Instant cleanupClaimedAt;

    @Column(name = "cleanup_attempts", nullable = false)
    private int cleanupAttempts = 0;

    @Column(name = "cleanup_next_attempt_at")
    private Instant cleanupNextAttemptAt;

    @Column(name = "cleanup_failed_at")
    private Instant cleanupFailedAt;

    @Builder
    private FileMetadata(
        String fileId,
        String originalFileName,
        FileCategory category,
        String contentType,
        Long fileSize,
        StorageProvider storageProvider,
        String storageKey,
        Long uploadedMemberId
    ) {
        this.id = fileId;
        this.originalFileName = originalFileName;
        this.category = category;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.storageProvider = storageProvider;
        this.storageKey = storageKey;
        this.uploadedMemberId = uploadedMemberId;
        this.isUploaded = false;
    }

    /**
     * 업로드 완료 처리
     */
    public void markAsUploaded() {
        markAsUploaded(Instant.now());
    }

    /**
     * 호환 상태와 canonical 완료 시각을 원자적으로 변경합니다.
     */
    public void markAsUploaded(Instant confirmedAt) {
        if (confirmedAt == null) {
            throw new IllegalArgumentException("업로드 확인 시각은 필수입니다.");
        }
        this.isUploaded = true;
        this.confirmedAt = confirmedAt;
    }

    /**
     * Registry READY 전 audit mode에서 legacy 완료 row를 보호하기 위한 호환 판정입니다.
     */
    public boolean isConfirmedForAudit() {
        return confirmedAt != null || isUploaded;
    }

    /**
     * 실제 스토리지 객체 정보가 요청 메타데이터와 일치하는지 검증하고 업로드 완료 처리합니다.
     */
    public void confirmUploaded(long actualFileSize, String actualContentType) {
        if (!category.isAllowedSize(actualFileSize)) {
            throw new StorageException(StorageErrorCode.FILE_SIZE_EXCEEDED);
        }

        if (fileSize == null || !Objects.equals(fileSize, actualFileSize)) {
            throw new StorageException(StorageErrorCode.FILE_SIZE_MISMATCH);
        }

        if (!isSameContentType(actualContentType)) {
            throw new StorageException(StorageErrorCode.INVALID_CONTENT_TYPE);
        }

        markAsUploaded();
    }

    /**
     * 파일 확장자 추출
     */
    public String getFileExtension() {
        int lastDotIndex = originalFileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < originalFileName.length() - 1) {
            return originalFileName.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }

    private boolean isSameContentType(String actualContentType) {
        if (contentType == null || actualContentType == null) {
            return Objects.equals(contentType, actualContentType);
        }
        return extractMediaType(contentType).equalsIgnoreCase(extractMediaType(actualContentType));
    }

    private String extractMediaType(String value) {
        int parameterIndex = value.indexOf(';');
        if (parameterIndex < 0) {
            return value.trim();
        }
        return value.substring(0, parameterIndex).trim();
    }
}
