package com.umc.product.storage.application.port.out;

import java.util.Optional;

import com.umc.product.storage.application.port.in.command.dto.FileUploadInfo;
import com.umc.product.storage.application.port.out.dto.StorageObjectInfo;
import com.umc.product.storage.domain.enums.FileCategory;

/**
 * 외부 스토리지 접근을 위한 Port
 *
 * <p>실제 구현체는 AWS S3 기반 스토리지를 지원합니다.
 */
public interface StoragePort {

    /**
     * 업로드용 Signed URL을 생성합니다.
     *
     * @param storageKey      스토리지 키 (경로 포함)
     * @param contentType     파일 MIME 타입
     * @param durationMinutes URL 유효 시간 (분)
     * @return 업로드 정보 (URL, 헤더 등)
     */
    FileUploadInfo generateUploadUrl(
            String storageKey,
            String contentType,
            long durationMinutes
    );

    /**
     * 파일 크기를 포함해 업로드용 Signed URL을 생성합니다.
     *
     * <p>구현체가 파일 크기 기반 서명을 지원하지 않으면 기존 URL 생성 방식으로 동작합니다.
     *
     * @param storageKey      스토리지 키 (경로 포함)
     * @param contentType     파일 MIME 타입
     * @param fileSize        클라이언트가 업로드해야 하는 정확한 파일 크기
     * @param durationMinutes URL 유효 시간 (분)
     * @return 업로드 정보 (URL, 헤더 등)
     */
    default FileUploadInfo generateUploadUrl(
            String storageKey,
            String contentType,
            long fileSize,
            long durationMinutes
    ) {
        return generateUploadUrl(storageKey, contentType, durationMinutes);
    }

    /**
     * 파일 접근 URL을 생성합니다.
     *
     * <p>CDN을 통한 접근이 설정된 경우 CDN URL을 반환합니다.
     *
     * @param storageKey      스토리지 키
     * @param durationMinutes URL 유효 시간 (분) - Signed URL 사용 시
     * @return 파일 접근 URL
     */
    String generateAccessUrl(String storageKey, long durationMinutes);

    void uploadObject(String storageKey, String contentType, byte[] content);

    /**
     * 파일이 존재하는지 확인합니다.
     *
     * @param storageKey 스토리지 키
     * @return 존재 여부
     */
    boolean exists(String storageKey);

    /**
     * 실제 스토리지에 저장된 객체 메타데이터를 조회합니다.
     *
     * @param storageKey 스토리지 키
     * @return 객체가 있으면 메타데이터, 없으면 빈 값
     */
    default Optional<StorageObjectInfo> findObjectInfoByStorageKey(String storageKey) {
        return Optional.empty();
    }

    /**
     * 파일을 삭제합니다.
     *
     * @param storageKey 스토리지 키
     */
    void delete(String storageKey);

    /**
     * 스토리지 키를 생성합니다.
     *
     * @param category  파일 카테고리
     * @param fileId    파일 고유 ID
     * @param extension 파일 확장자 (빈 문자열이면 확장자 없이 생성)
     * @return 스토리지 키
     */
    default String generateStorageKey(FileCategory category, String fileId, String extension) {
        if (extension == null || extension.isBlank()) {
            return String.format("%s/%s", category.getPathPrefix(), fileId);
        }
        return String.format("%s/%s.%s", category.getPathPrefix(), fileId, extension);
    }
}
