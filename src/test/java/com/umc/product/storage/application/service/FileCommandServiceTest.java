package com.umc.product.storage.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.umc.product.storage.application.port.in.command.ManageFileUseCase;
import com.umc.product.storage.application.port.in.command.StoreGeneratedFileUseCase;
import com.umc.product.storage.application.port.in.command.dto.DeleteFileCommand;
import com.umc.product.storage.application.port.in.command.dto.FileUploadInfo;
import com.umc.product.storage.application.port.in.command.dto.GeneratedFileInfo;
import com.umc.product.storage.application.port.in.command.dto.PrepareFileUploadCommand;
import com.umc.product.storage.application.port.in.command.dto.StoreGeneratedFileCommand;
import com.umc.product.storage.application.port.out.LoadFileMetadataPort;
import com.umc.product.storage.application.port.out.SaveFileMetadataPort;
import com.umc.product.storage.application.port.out.dto.StorageObjectInfo;
import com.umc.product.storage.domain.FileMetadata;
import com.umc.product.storage.domain.enums.FileCategory;
import com.umc.product.storage.domain.enums.StorageProvider;
import com.umc.product.storage.domain.exception.StorageErrorCode;
import com.umc.product.storage.domain.exception.StorageException;
import com.umc.product.support.UseCaseTestSupport;

/**
 * FileCommandService 통합 테스트
 */
class FileCommandServiceTest extends UseCaseTestSupport {

    @Autowired
    private ManageFileUseCase manageFileUseCase;

    @Autowired
    private StoreGeneratedFileUseCase storeGeneratedFileUseCase;

    @Autowired
    private LoadFileMetadataPort loadFileMetadataPort;

    @Autowired
    private SaveFileMetadataPort saveFileMetadataPort;

    @Autowired
    private TransactionOperations transactionOperations;


    @Test
    void 파일_업로드_URL을_생성한다() {
        // given: 파일 업로드 준비 Command
        PrepareFileUploadCommand command = new PrepareFileUploadCommand(
            "profile.jpg",
            "image/jpeg",
            1024L,
            FileCategory.PROFILE_IMAGE,
            1L
        );

        // StoragePort Mock 설정
        given(storagePort.generateStorageKey(any(FileCategory.class), anyString(), anyString()))
            .willReturn("profile/test-id.jpg");

        given(storagePort.generateUploadUrl(anyString(), anyString(), anyLong(), anyLong()))
            .willReturn(new FileUploadInfo(
                "test-file-id",
                "https://storage.googleapis.com/signed-upload-url",
                "PUT",
                Map.of("Content-Type", "image/jpeg"),
                LocalDateTime.now().plusMinutes(15)
            ));

        // when: 업로드 URL 생성
        FileUploadInfo result = manageFileUseCase.getFileUploadUrl(command);

        // then: 결과 검증
        assertThat(result).isNotNull();
        assertThat(result.uploadUrl()).contains("signed-upload-url");
        assertThat(result.uploadMethod()).isEqualTo("PUT");

        // DB에 메타데이터가 저장되었는지 확인
        FileMetadata savedMetadata = loadFileMetadataPort.findByFileId(result.fileId())
            .orElseThrow();
        assertThat(savedMetadata.getOriginalFileName()).isEqualTo("profile.jpg");
        assertThat(savedMetadata.isUploaded()).isFalse(); // 아직 업로드 안됨
    }

    /**
     * ✅ 학습 포인트 9: 검증 로직 테스트 - 비즈니스 규칙 위반 시 예외가 발생하는지 검증
     */
    @Test
    void 허용되지_않는_확장자면_예외가_발생한다() {
        // given: PROFILE_IMAGE는 jpg, jpeg, png, webp만 허용
        PrepareFileUploadCommand command = new PrepareFileUploadCommand(
            "profile.gif",  // GIF는 허용 안됨
            "image/gif",
            1024L,
            FileCategory.PROFILE_IMAGE,
            1L
        );

        // when & then
        assertThatThrownBy(() -> manageFileUseCase.getFileUploadUrl(command))
            .isInstanceOf(StorageException.class);
    }

    @Test
    void 파일_크기가_초과하면_예외가_발생한다() {
        // given: PROFILE_IMAGE는 5MB까지만 허용
        long fileSize = 6 * 1024 * 1024; // 6MB
        PrepareFileUploadCommand command = new PrepareFileUploadCommand(
            "large-profile.jpg",
            "image/jpeg",
            fileSize,
            FileCategory.PROFILE_IMAGE,
            1L
        );

        // when & then
        assertThatThrownBy(() -> manageFileUseCase.getFileUploadUrl(command))
            .isInstanceOf(StorageException.class);
    }

    @Test
    void 확장자가_없으면_예외가_발생한다() {
        // given: PROFILE_IMAGE는 확장자 필수
        PrepareFileUploadCommand command = new PrepareFileUploadCommand(
            "profile",  // 확장자 없음
            "image/jpeg",
            1024L,
            FileCategory.PROFILE_IMAGE,
            1L
        );

        // when & then
        assertThatThrownBy(() -> manageFileUseCase.getFileUploadUrl(command))
            .isInstanceOf(StorageException.class);
    }

    /**
     * ✅ 학습 포인트 10: 상태 변경 테스트 - 도메인 메서드가 올바르게 호출되었는지 검증
     */
    @Test
    void 업로드를_완료_처리한다() {
        // given: 업로드 대기 중인 파일
        FileMetadata metadata = saveTestFile("test-file-1", "document.pdf", false);

        // StoragePort Mock 설정 - 파일이 스토리지에 존재한다고 가정
        given(storagePort.findObjectInfoByStorageKey(metadata.getStorageKey()))
            .willReturn(java.util.Optional.of(StorageObjectInfo.of(
                metadata.getStorageKey(),
                metadata.getFileSize(),
                metadata.getContentType()
            )));

        // when: 업로드 완료 처리
        manageFileUseCase.confirmUpload(metadata.getId());

        // then: 업로드 상태가 변경되었는지 확인
        FileMetadata updated = loadFileMetadataPort.findByFileId(metadata.getId())
            .orElseThrow();
        assertThat(updated.isUploaded()).isTrue();
        assertThat(updated.getConfirmedAt()).isNotNull();
        assertThat(updated.getUnreferencedAt()).isEqualTo(updated.getConfirmedAt());
    }

    @Test
    void 스토리지에_파일이_없으면_업로드_완료_처리에_실패한다() {
        // given
        FileMetadata metadata = saveTestFile("test-file-2", "document.pdf", false);

        // StoragePort Mock 설정 - 파일이 스토리지에 없음
        given(storagePort.findObjectInfoByStorageKey(metadata.getStorageKey()))
            .willReturn(java.util.Optional.empty());

        // when & then
        assertThatThrownBy(() -> manageFileUseCase.confirmUpload(metadata.getId()))
            .isInstanceOf(StorageException.class);
    }

    @Test
    void 이미_업로드된_파일은_재확인할_수_없다() {
        // given: 이미 업로드된 파일
        FileMetadata metadata = saveTestFile("test-file-3", "document.pdf", true);

        // when & then
        assertThatThrownBy(() -> manageFileUseCase.confirmUpload(metadata.getId()))
            .isInstanceOf(StorageException.class);
    }

    @Test
    void 생성_파일은_S3_upload_전에_committed_pending_metadata가_조회된다() {
        // given
        AtomicReference<String> generatedFileId = new AtomicReference<>();
        given(storagePort.generateStorageKey(
            org.mockito.ArgumentMatchers.eq(FileCategory.CERTIFICATE),
            anyString(),
            org.mockito.ArgumentMatchers.eq("pdf")
        )).willAnswer(invocation -> {
            String fileId = invocation.getArgument(1);
            generatedFileId.set(fileId);
            return "private/certificate/" + fileId + ".pdf";
        });
        org.mockito.BDDMockito.willAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            FileMetadata pending = loadFileMetadataPort.findByFileId(generatedFileId.get()).orElseThrow();
            assertThat(pending.isUploaded()).isFalse();
            assertThat(pending.getConfirmedAt()).isNull();
            assertThat(pending.getUnreferencedAt()).isNull();
            return null;
        }).given(storagePort).uploadObject(
            anyString(),
            org.mockito.ArgumentMatchers.eq("application/pdf"),
            org.mockito.ArgumentMatchers.any(byte[].class)
        );

        // when
        AtomicReference<GeneratedFileInfo> storedFile = new AtomicReference<>();
        transactionOperations.executeWithoutResult(status -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            storedFile.set(storeGeneratedFileUseCase.store(generatedFileCommand()));
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
        });

        // then
        GeneratedFileInfo result = storedFile.get();
        FileMetadata confirmed = loadFileMetadataPort.findByFileId(result.fileId()).orElseThrow();
        assertThat(confirmed.getUploadedMemberId()).isEqualTo(1L);
        assertThat(confirmed.getConfirmedAt()).isNotNull();
        assertThat(confirmed.getUnreferencedAt()).isEqualTo(confirmed.getConfirmedAt());
    }

    @Test
    void 생성_파일_S3_upload_실패는_committed_pending_metadata를_보존한다() {
        // given
        AtomicReference<String> generatedFileId = new AtomicReference<>();
        given(storagePort.generateStorageKey(
            org.mockito.ArgumentMatchers.eq(FileCategory.CERTIFICATE),
            anyString(),
            org.mockito.ArgumentMatchers.eq("pdf")
        )).willAnswer(invocation -> {
            String fileId = invocation.getArgument(1);
            generatedFileId.set(fileId);
            return "private/certificate/" + fileId + ".pdf";
        });
        willThrow(new StorageException(StorageErrorCode.STORAGE_UPLOAD_FAILED))
            .given(storagePort)
            .uploadObject(
                anyString(),
                org.mockito.ArgumentMatchers.eq("application/pdf"),
                org.mockito.ArgumentMatchers.any(byte[].class)
            );

        // when & then
        assertThatThrownBy(() -> storeGeneratedFileUseCase.store(generatedFileCommand()))
            .isInstanceOf(StorageException.class)
            .extracting("baseCode")
            .isEqualTo(StorageErrorCode.STORAGE_UPLOAD_FAILED);
        FileMetadata pending = loadFileMetadataPort.findByFileId(generatedFileId.get()).orElseThrow();
        assertThat(pending.isUploaded()).isFalse();
        assertThat(pending.getConfirmedAt()).isNull();
        assertThat(pending.getUnreferencedAt()).isNull();
    }

    /**
     * ✅ 학습 포인트 11: 삭제 동작 검증 - verify()로 Mock 메서드가 호출되었는지 확인
     */
    @Test
    void READY_전에는_업로드된_파일도_삭제하지_않는다() {
        // given
        FileMetadata metadata = saveTestFile("test-file-4", "document.pdf", true);
        String fileId = metadata.getId();

        // when & then
        assertThatThrownBy(() -> manageFileUseCase.deleteFile(deleteCommand(fileId, 1L)))
            .isInstanceOf(StorageException.class)
            .extracting("baseCode")
            .isEqualTo(StorageErrorCode.FILE_USAGE_REGISTRY_NOT_READY);
        verify(storagePort, never()).delete(metadata.getStorageKey());
        assertThat(loadFileMetadataPort.findByFileId(fileId)).isPresent();
    }

    @Test
    void 존재하지_않는_파일을_삭제하면_예외가_발생한다() {
        // given
        String nonExistentFileId = "non-existent-id";

        // when & then
        assertThatThrownBy(() -> manageFileUseCase.deleteFile(deleteCommand(nonExistentFileId, 1L)))
            .isInstanceOf(StorageException.class)
            .extracting("baseCode")
            .isEqualTo(StorageErrorCode.FILE_USAGE_REGISTRY_NOT_READY);
    }

    private DeleteFileCommand deleteCommand(String fileId, Long requesterMemberId) {
        return DeleteFileCommand.builder()
            .fileId(fileId)
            .requesterMemberId(requesterMemberId)
            .build();
    }

    private StoreGeneratedFileCommand generatedFileCommand() {
        return StoreGeneratedFileCommand.of(
            "certificate.pdf",
            "application/pdf",
            "pdf-content".getBytes(StandardCharsets.UTF_8),
            FileCategory.CERTIFICATE,
            1L
        );
    }

    /**
     * 테스트용 파일 메타데이터 생성 헬퍼
     */
    private FileMetadata saveTestFile(String fileId, String fileName, boolean isUploaded) {
        FileMetadata metadata = FileMetadata.builder()
            .fileId(fileId)
            .originalFileName(fileName)
            .category(FileCategory.PROFILE_IMAGE)
            .contentType("application/pdf")
            .fileSize(1024L)
            .storageProvider(StorageProvider.AWS_S3)
            .storageKey("test/" + fileId + ".pdf")
            .uploadedMemberId(1L)
            .build();

        if (isUploaded) {
            metadata.markAsUploaded();
        }

        return saveFileMetadataPort.save(metadata);
    }
}
