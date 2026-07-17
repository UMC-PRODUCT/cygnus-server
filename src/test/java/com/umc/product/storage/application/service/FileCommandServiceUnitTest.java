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
import static org.mockito.Mockito.times;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.storage.application.port.in.command.dto.DeleteFileCommand;
import com.umc.product.storage.application.port.in.command.dto.FileUploadInfo;
import com.umc.product.storage.application.port.in.command.dto.GeneratedFileInfo;
import com.umc.product.storage.application.port.in.command.dto.PrepareFileUploadCommand;
import com.umc.product.storage.application.port.in.command.dto.StoreGeneratedFileCommand;
import com.umc.product.storage.application.port.out.LoadFileMetadataPort;
import com.umc.product.storage.application.port.out.LoadFileUsagePort;
import com.umc.product.storage.application.port.out.LockFileMetadataPort;
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
    LockFileMetadataPort lockFileMetadataPort;

    @Mock
    FileDeletionService fileDeletionService;

    @Mock
    Clock clock;

    FileCommandService sut;

    @BeforeEach
    void setUp() {
        lenient().when(storagePort.generateStorageKey(
            org.mockito.ArgumentMatchers.any(FileCategory.class),
            anyString(),
            anyString()
        )).thenCallRealMethod();
        GeneratedFileMetadataService generatedFileMetadataService = new GeneratedFileMetadataService(
            lockFileMetadataPort,
            loadFileUsagePort,
            saveFileMetadataPort,
            clock
        );
        sut = new FileCommandService(
            storagePort,
            loadFileMetadataPort,
            saveFileMetadataPort,
            loadFileUsagePort,
            fileDeletionService,
            generatedFileMetadataService,
            clock
        );
    }

    @Test
    @DisplayName("파일 삭제와 생성 파일 S3 I/O는 클래스 공통 트랜잭션으로 감싸지 않는다")
    void 파일_삭제와_생성_파일_S3_IO는_클래스_공통_트랜잭션으로_감싸지_않는다() throws NoSuchMethodException {
        // when
        Method getFileUploadUrl = FileCommandService.class.getMethod(
            "getFileUploadUrl",
            com.umc.product.storage.application.port.in.command.dto.PrepareFileUploadCommand.class
        );
        Method storeGeneratedFile = FileCommandService.class.getMethod("store", StoreGeneratedFileCommand.class);
        Method confirmUpload = FileCommandService.class.getMethod("confirmUpload", String.class);
        Method deleteFile = FileCommandService.class.getMethod("deleteFile", DeleteFileCommand.class);
        Method createPending = GeneratedFileMetadataService.class.getMethod("createPending", FileMetadata.class);
        Method confirmGenerated = GeneratedFileMetadataService.class.getMethod("confirmGenerated", String.class);

        // then
        assertThat(FileCommandService.class.getAnnotation(Transactional.class)).isNull();
        assertThat(getFileUploadUrl.getAnnotation(Transactional.class)).isNotNull();
        assertThat(storeGeneratedFile.getAnnotation(Transactional.class).propagation())
            .isEqualTo(Propagation.NOT_SUPPORTED);
        assertThat(confirmUpload.getAnnotation(Transactional.class)).isNotNull();
        assertThat(deleteFile.getAnnotation(Transactional.class)).isNull();
        assertThat(createPending.getAnnotation(Transactional.class).propagation())
            .isEqualTo(Propagation.REQUIRES_NEW);
        assertThat(confirmGenerated.getAnnotation(Transactional.class).propagation())
            .isEqualTo(Propagation.REQUIRES_NEW);
    }

    @Test
    @DisplayName("생성 파일은 pending metadata를 먼저 저장한 뒤 S3 업로드와 lock confirm을 수행한다")
    void 생성_파일은_pending_metadata_S3_upload_lock_confirm_순서로_저장한다() {
        // given
        StoreGeneratedFileCommand command = generatedFileCommand(1L);
        AtomicInteger saveCount = new AtomicInteger();
        AtomicReference<FileMetadata> pending = new AtomicReference<>();
        AtomicReference<FileMetadata> confirmed = new AtomicReference<>();
        given(saveFileMetadataPort.save(org.mockito.ArgumentMatchers.any(FileMetadata.class)))
            .willAnswer(invocation -> {
                FileMetadata metadata = invocation.getArgument(0);
                if (saveCount.incrementAndGet() == 1) {
                    assertThat(metadata.isUploaded()).isFalse();
                    assertThat(metadata.getConfirmedAt()).isNull();
                    pending.set(metadata);
                } else {
                    confirmed.set(metadata);
                }
                return metadata;
            });
        given(lockFileMetadataPort.lockAllByFileIds(org.mockito.ArgumentMatchers.anyList()))
            .willAnswer(invocation -> List.of(copyPending(pending.get())));
        given(loadFileUsagePort.countByFileId(anyString())).willReturn(0L);
        given(clock.instant()).willReturn(NOW);
        org.mockito.BDDMockito.willAnswer(invocation -> {
            assertThat(pending.get()).isNotNull();
            assertThat(pending.get().isUploaded()).isFalse();
            return null;
        }).given(storagePort).uploadObject(anyString(), anyString(), org.mockito.ArgumentMatchers.any(byte[].class));

        // when
        GeneratedFileInfo result = sut.store(command);

        // then
        InOrder order = inOrder(saveFileMetadataPort, storagePort, lockFileMetadataPort);
        order.verify(saveFileMetadataPort).save(org.mockito.ArgumentMatchers.any(FileMetadata.class));
        order.verify(storagePort).uploadObject(
            result.storageKey(),
            "application/pdf",
            command.content()
        );
        order.verify(lockFileMetadataPort).lockAllByFileIds(List.of(result.fileId()));
        order.verify(saveFileMetadataPort).save(org.mockito.ArgumentMatchers.any(FileMetadata.class));
        assertThat(saveCount).hasValue(2);
        assertThat(pending.get().getUploadedMemberId()).isEqualTo(1L);
        assertThat(confirmed.get().getConfirmedAt()).isEqualTo(NOW);
        assertThat(confirmed.get().getUnreferencedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("생성 파일 S3 업로드 실패는 pending metadata를 남기고 confirm을 호출하지 않는다")
    void 생성_파일_S3_upload_실패는_pending_metadata를_남긴다() {
        // given
        StoreGeneratedFileCommand command = generatedFileCommand(1L);
        AtomicReference<FileMetadata> pending = new AtomicReference<>();
        given(saveFileMetadataPort.save(org.mockito.ArgumentMatchers.any(FileMetadata.class)))
            .willAnswer(invocation -> {
                FileMetadata metadata = invocation.getArgument(0);
                pending.set(metadata);
                return metadata;
            });
        willThrow(new StorageException(StorageErrorCode.STORAGE_UPLOAD_FAILED))
            .given(storagePort)
            .uploadObject(anyString(), anyString(), org.mockito.ArgumentMatchers.any(byte[].class));

        // when & then
        assertThatThrownBy(() -> sut.store(command))
            .isInstanceOf(StorageException.class)
            .extracting("baseCode")
            .isEqualTo(StorageErrorCode.STORAGE_UPLOAD_FAILED);
        assertThat(pending.get()).isNotNull();
        assertThat(pending.get().isUploaded()).isFalse();
        assertThat(pending.get().getConfirmedAt()).isNull();
        then(lockFileMetadataPort).shouldHaveNoInteractions();
        then(saveFileMetadataPort).should(times(1)).save(pending.get());
        then(storagePort).should(never()).delete(anyString());
    }

    @Test
    @DisplayName("생성 파일 confirm 실패는 이미 저장한 pending metadata와 S3 객체를 삭제하지 않는다")
    void 생성_파일_confirm_실패는_pending_metadata와_S3_객체를_삭제하지_않는다() {
        // given
        StoreGeneratedFileCommand command = generatedFileCommand(1L);
        AtomicInteger saveCount = new AtomicInteger();
        AtomicReference<FileMetadata> pending = new AtomicReference<>();
        given(saveFileMetadataPort.save(org.mockito.ArgumentMatchers.any(FileMetadata.class)))
            .willAnswer(invocation -> {
                FileMetadata metadata = invocation.getArgument(0);
                if (saveCount.incrementAndGet() == 1) {
                    pending.set(metadata);
                    return metadata;
                }
                throw new IllegalStateException("confirm failed");
            });
        given(lockFileMetadataPort.lockAllByFileIds(org.mockito.ArgumentMatchers.anyList()))
            .willAnswer(invocation -> List.of(copyPending(pending.get())));
        given(loadFileUsagePort.countByFileId(anyString())).willReturn(0L);
        given(clock.instant()).willReturn(NOW);

        // when & then
        assertThatThrownBy(() -> sut.store(command))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("confirm failed");
        assertThat(pending.get().isUploaded()).isFalse();
        assertThat(pending.get().getConfirmedAt()).isNull();
        then(storagePort).should(never()).delete(anyString());
        then(saveFileMetadataPort).should(times(2))
            .save(org.mockito.ArgumentMatchers.any(FileMetadata.class));
    }

    @Test
    @DisplayName("생성자 member ID가 null이면 generated file command 생성을 거부한다")
    void 생성자_member_ID가_null이면_generated_file_command를_거부한다() {
        byte[] content = "pdf-content".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> StoreGeneratedFileCommand.of(
            "certificate.pdf",
            "application/pdf",
            content,
            FileCategory.CERTIFICATE,
            null
        )).isInstanceOf(NullPointerException.class)
            .hasMessage("generatedByMemberId must not be null");
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

    private StoreGeneratedFileCommand generatedFileCommand(Long generatedByMemberId) {
        return StoreGeneratedFileCommand.of(
            "certificate.pdf",
            "application/pdf",
            "pdf-content".getBytes(StandardCharsets.UTF_8),
            FileCategory.CERTIFICATE,
            generatedByMemberId
        );
    }

    private FileMetadata copyPending(FileMetadata source) {
        return FileMetadata.builder()
            .fileId(source.getId())
            .originalFileName(source.getOriginalFileName())
            .category(source.getCategory())
            .contentType(source.getContentType())
            .fileSize(source.getFileSize())
            .storageProvider(source.getStorageProvider())
            .storageKey(source.getStorageKey())
            .uploadedMemberId(source.getUploadedMemberId())
            .build();
    }

}
