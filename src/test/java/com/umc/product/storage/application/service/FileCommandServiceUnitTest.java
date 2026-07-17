package com.umc.product.storage.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.storage.application.port.in.command.dto.DeleteFileCommand;
import com.umc.product.storage.application.port.in.command.dto.FileUploadInfo;
import com.umc.product.storage.application.port.in.command.dto.PrepareFileUploadCommand;
import com.umc.product.storage.application.port.out.LoadFileMetadataPort;
import com.umc.product.storage.application.port.out.LoadFileUsagePort;
import com.umc.product.storage.application.port.out.SaveFileMetadataPort;
import com.umc.product.storage.application.port.out.StoragePort;
import com.umc.product.storage.application.port.out.dto.StorageObjectInfo;
import com.umc.product.storage.domain.FileMetadata;
import com.umc.product.storage.domain.enums.FileCategory;
import com.umc.product.storage.domain.enums.StorageProvider;
import com.umc.product.storage.domain.exception.StorageErrorCode;
import com.umc.product.storage.domain.exception.StorageException;

@ExtendWith(MockitoExtension.class)
class FileCommandServiceUnitTest {

    private static final Instant NOW = Instant.parse("2026-07-17T00:00:00Z");

    @Mock
    StoragePort storagePort;

    @Mock
    LoadFileMetadataPort loadFileMetadataPort;

    @Mock
    SaveFileMetadataPort saveFileMetadataPort;

    @Mock
    LoadFileUsagePort loadFileUsagePort;

    @Mock
    FileDeletionService fileDeletionService;

    @Mock
    Clock clock;

    @InjectMocks
    FileCommandService sut;

    @BeforeEach
    void setUp() {
        lenient().when(storagePort.generateStorageKey(
            org.mockito.ArgumentMatchers.any(FileCategory.class),
            anyString(),
            anyString()
        )).thenCallRealMethod();
    }

    @Test
    @DisplayName("파일 삭제는 클래스 공통 트랜잭션으로 외부 스토리지 I/O를 감싸지 않는다")
    void 파일_삭제는_클래스_공통_트랜잭션으로_외부_스토리지_IO를_감싸지_않는다() throws NoSuchMethodException {
        // when
        Method getFileUploadUrl = FileCommandService.class.getMethod(
            "getFileUploadUrl",
            com.umc.product.storage.application.port.in.command.dto.PrepareFileUploadCommand.class
        );
        Method confirmUpload = FileCommandService.class.getMethod("confirmUpload", String.class);
        Method deleteFile = FileCommandService.class.getMethod("deleteFile", DeleteFileCommand.class);

        // then
        assertThat(FileCommandService.class.getAnnotation(Transactional.class)).isNull();
        assertThat(getFileUploadUrl.getAnnotation(Transactional.class)).isNotNull();
        assertThat(confirmUpload.getAnnotation(Transactional.class)).isNotNull();
        assertThat(deleteFile.getAnnotation(Transactional.class)).isNull();
    }

    @Test
    @DisplayName("업로드 URL 생성은 요청 파일 크기를 스토리지 서명에 포함한다")
    void 업로드_URL_생성은_요청_파일_크기를_스토리지_서명에_포함한다() {
        // given
        PrepareFileUploadCommand command = new PrepareFileUploadCommand(
            "portfolio.pdf",
            "application/pdf",
            1024L,
            FileCategory.PORTFOLIO,
            1L
        );
        given(storagePort.generateUploadUrl(
            org.mockito.ArgumentMatchers.matches("private/portfolio/.+\\.pdf"),
            org.mockito.ArgumentMatchers.eq("application/pdf"),
            org.mockito.ArgumentMatchers.eq(1024L),
            org.mockito.ArgumentMatchers.eq(15L)
        )).willReturn(new FileUploadInfo(
            null,
            "https://storage.example.com/upload",
            "PUT",
            Map.of("Content-Type", "application/pdf"),
            LocalDateTime.now().plusMinutes(15)
        ));

        // when
        sut.getFileUploadUrl(command);

        // then
        then(storagePort).should().generateUploadUrl(
            org.mockito.ArgumentMatchers.matches("private/portfolio/.+\\.pdf"),
            org.mockito.ArgumentMatchers.eq("application/pdf"),
            org.mockito.ArgumentMatchers.eq(1024L),
            org.mockito.ArgumentMatchers.eq(15L)
        );
    }

    @Test
    @DisplayName("업로드 완료 확인 시 S3 객체가 없으면 완료 처리하지 않는다")
    void 업로드_완료_확인_시_S3_객체가_없으면_완료_처리하지_않는다() {
        // given
        FileMetadata metadata = pendingFile("file-id", FileCategory.PORTFOLIO, 1024L, "application/pdf");
        given(loadFileMetadataPort.findByFileId("file-id")).willReturn(Optional.of(metadata));
        given(storagePort.findObjectInfoByStorageKey(metadata.getStorageKey())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> sut.confirmUpload("file-id"))
            .isInstanceOf(StorageException.class)
            .extracting("baseCode")
            .isEqualTo(StorageErrorCode.FILE_UPLOAD_NOT_COMPLETED);

        then(saveFileMetadataPort).should(never()).save(org.mockito.ArgumentMatchers.any(FileMetadata.class));
        then(storagePort).should(never()).delete(anyString());
    }

    @Test
    @DisplayName("실제 S3 객체 크기가 카테고리 제한을 초과하면 업로드 완료 처리하지 않고 객체를 삭제한다")
    void 실제_S3_객체_크기가_카테고리_제한을_초과하면_업로드_완료_처리하지_않고_객체를_삭제한다() {
        // given
        long actualSize = 310L * 1024 * 1024;
        FileMetadata metadata = pendingFile("file-id", FileCategory.PORTFOLIO, 1024L, "application/pdf");
        given(loadFileMetadataPort.findByFileId("file-id")).willReturn(Optional.of(metadata));
        given(storagePort.findObjectInfoByStorageKey(metadata.getStorageKey()))
            .willReturn(Optional.of(StorageObjectInfo.of(metadata.getStorageKey(), actualSize, "application/pdf")));

        // when & then
        assertThatThrownBy(() -> sut.confirmUpload("file-id"))
            .isInstanceOf(StorageException.class)
            .extracting("baseCode")
            .isEqualTo(StorageErrorCode.FILE_SIZE_EXCEEDED);

        then(saveFileMetadataPort).should(never()).save(org.mockito.ArgumentMatchers.any(FileMetadata.class));
        then(storagePort).should().delete(metadata.getStorageKey());
    }

    @Test
    @DisplayName("실제 S3 객체 크기가 요청 크기와 다르면 업로드 완료 처리하지 않고 객체를 삭제한다")
    void 실제_S3_객체_크기가_요청_크기와_다르면_업로드_완료_처리하지_않고_객체를_삭제한다() {
        // given
        FileMetadata metadata = pendingFile("file-id", FileCategory.PORTFOLIO, 1024L, "application/pdf");
        given(loadFileMetadataPort.findByFileId("file-id")).willReturn(Optional.of(metadata));
        given(storagePort.findObjectInfoByStorageKey(metadata.getStorageKey()))
            .willReturn(Optional.of(StorageObjectInfo.of(metadata.getStorageKey(), 2048L, "application/pdf")));

        // when & then
        assertThatThrownBy(() -> sut.confirmUpload("file-id"))
            .isInstanceOf(StorageException.class)
            .extracting("baseCode")
            .isEqualTo(StorageErrorCode.FILE_SIZE_MISMATCH);

        then(saveFileMetadataPort).should(never()).save(org.mockito.ArgumentMatchers.any(FileMetadata.class));
        then(storagePort).should().delete(metadata.getStorageKey());
    }

    @Test
    @DisplayName("실제 S3 객체 Content-Type이 요청값과 다르면 업로드 완료 처리하지 않고 객체를 삭제한다")
    void 실제_S3_객체_Content_Type이_요청값과_다르면_업로드_완료_처리하지_않고_객체를_삭제한다() {
        // given
        FileMetadata metadata = pendingFile("file-id", FileCategory.PORTFOLIO, 1024L, "application/pdf");
        given(loadFileMetadataPort.findByFileId("file-id")).willReturn(Optional.of(metadata));
        given(storagePort.findObjectInfoByStorageKey(metadata.getStorageKey()))
            .willReturn(Optional.of(StorageObjectInfo.of(metadata.getStorageKey(), 1024L, "text/plain")));

        // when & then
        assertThatThrownBy(() -> sut.confirmUpload("file-id"))
            .isInstanceOf(StorageException.class)
            .extracting("baseCode")
            .isEqualTo(StorageErrorCode.INVALID_CONTENT_TYPE);

        then(saveFileMetadataPort).should(never()).save(org.mockito.ArgumentMatchers.any(FileMetadata.class));
        then(storagePort).should().delete(metadata.getStorageKey());
    }

    @Test
    @DisplayName("실제 S3 객체 정보가 요청값과 일치하면 업로드 완료 처리한다")
    void 실제_S3_객체_정보가_요청값과_일치하면_업로드_완료_처리한다() {
        // given
        FileMetadata metadata = pendingFile("file-id", FileCategory.PORTFOLIO, 1024L, "application/pdf");
        given(loadFileMetadataPort.findByFileId("file-id")).willReturn(Optional.of(metadata));
        given(storagePort.findObjectInfoByStorageKey(metadata.getStorageKey()))
            .willReturn(Optional.of(StorageObjectInfo.of(metadata.getStorageKey(), 1024L, "application/pdf")));
        given(loadFileUsagePort.countByFileId("file-id")).willReturn(0L);
        given(clock.instant()).willReturn(NOW);

        // when
        sut.confirmUpload("file-id");

        // then
        ArgumentCaptor<FileMetadata> captor = ArgumentCaptor.forClass(FileMetadata.class);
        then(saveFileMetadataPort).should().save(captor.capture());
        assertThat(captor.getValue().isUploaded()).isTrue();
        assertThat(captor.getValue().getConfirmedAt()).isEqualTo(NOW);
        assertThat(captor.getValue().getUnreferencedAt()).isEqualTo(NOW);
        then(storagePort).should(never()).delete(anyString());
    }

    @Test
    void 파일_삭제는_claim_S3_finalize_순서로_호출한다() {
        // given
        DeleteFileCommand command = deleteCommand("file-id", 1L);
        FileDeletionService.DeletionClaim claim = new FileDeletionService.DeletionClaim(
            "file-id",
            "test/file-id.pdf",
            UUID.fromString("00000000-0000-0000-0000-000000000001")
        );
        given(fileDeletionService.claim(command)).willReturn(claim);

        // when
        sut.deleteFile(command);

        // then
        InOrder order = inOrder(fileDeletionService, storagePort);
        order.verify(fileDeletionService).claim(command);
        order.verify(storagePort).delete(claim.storageKey());
        order.verify(fileDeletionService).finalizeDeletion(claim);
        then(saveFileMetadataPort).should(never()).deleteByFileId(anyString());
    }

    @Test
    void claim이_거부되면_S3를_호출하지_않는다() {
        // given
        DeleteFileCommand command = deleteCommand("file-id", 2L);
        given(fileDeletionService.claim(command))
            .willThrow(new StorageException(StorageErrorCode.FILE_DELETE_FORBIDDEN));

        // when & then
        assertThatThrownBy(() -> sut.deleteFile(command))
            .isInstanceOf(StorageException.class)
            .extracting("baseCode")
            .isEqualTo(StorageErrorCode.FILE_DELETE_FORBIDDEN);

        then(storagePort).should(never()).delete(anyString());
        then(fileDeletionService).should(never()).finalizeDeletion(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void S3_삭제_실패는_claim을_finalize하지_않는다() {
        // given
        DeleteFileCommand command = deleteCommand("file-id", 1L);
        FileDeletionService.DeletionClaim claim = new FileDeletionService.DeletionClaim(
            "file-id",
            "test/file-id.pdf",
            UUID.fromString("00000000-0000-0000-0000-000000000001")
        );
        given(fileDeletionService.claim(command)).willReturn(claim);
        willThrow(new StorageException(StorageErrorCode.STORAGE_DELETE_FAILED))
            .given(storagePort)
            .delete(claim.storageKey());

        // when & then
        assertThatThrownBy(() -> sut.deleteFile(command))
            .isInstanceOf(StorageException.class)
            .extracting("baseCode")
            .isEqualTo(StorageErrorCode.STORAGE_DELETE_FAILED);

        then(fileDeletionService).should(never()).finalizeDeletion(claim);
        then(saveFileMetadataPort).should(never()).deleteByFileId(anyString());
    }

    private DeleteFileCommand deleteCommand(String fileId, Long requesterMemberId) {
        return DeleteFileCommand.builder()
            .fileId(fileId)
            .requesterMemberId(requesterMemberId)
            .build();
    }

    private FileMetadata pendingFile(String fileId, FileCategory category, Long fileSize, String contentType) {
        return FileMetadata.builder()
            .fileId(fileId)
            .originalFileName("document.pdf")
            .category(category)
            .contentType(contentType)
            .fileSize(fileSize)
            .storageProvider(StorageProvider.AWS_S3)
            .storageKey(category.getPathPrefix() + "/" + fileId + ".pdf")
            .uploadedMemberId(1L)
            .build();
    }

}
