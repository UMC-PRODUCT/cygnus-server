package com.umc.product.storage.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.storage.application.port.in.command.dto.DeleteFileCommand;
import com.umc.product.storage.application.port.in.command.dto.FileUploadInfo;
import com.umc.product.storage.application.port.in.command.dto.PrepareFileUploadCommand;
import com.umc.product.storage.application.port.out.LoadFileMetadataPort;
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

    @Mock
    StoragePort storagePort;

    @Mock
    LoadFileMetadataPort loadFileMetadataPort;

    @Mock
    SaveFileMetadataPort saveFileMetadataPort;

    @Mock
    GetChallengerRoleUseCase getChallengerRoleUseCase;

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

        // when
        sut.confirmUpload("file-id");

        // then
        ArgumentCaptor<FileMetadata> captor = ArgumentCaptor.forClass(FileMetadata.class);
        then(saveFileMetadataPort).should().save(captor.capture());
        assertThat(captor.getValue().isUploaded()).isTrue();
        then(storagePort).should(never()).delete(anyString());
    }

    @Test
    @DisplayName("작성자가 아니어도 SUPER_ADMIN이면 파일을 삭제한다")
    void 작성자가_아니어도_SUPER_ADMIN이면_파일을_삭제한다() {
        // given
        FileMetadata metadata = uploadedFile("file-id", 1L);
        given(loadFileMetadataPort.findByFileId("file-id")).willReturn(Optional.of(metadata));
        given(getChallengerRoleUseCase.isSuperAdmin(2L)).willReturn(true);

        // when
        sut.deleteFile(deleteCommand("file-id", 2L));

        // then
        then(storagePort).should().delete(metadata.getStorageKey());
        then(saveFileMetadataPort).should().deleteByFileId("file-id");
    }

    @Test
    @DisplayName("작성자도 SUPER_ADMIN도 아니면 파일을 삭제할 수 없다")
    void 작성자도_SUPER_ADMIN도_아니면_파일을_삭제할_수_없다() {
        // given
        FileMetadata metadata = uploadedFile("file-id", 1L);
        given(loadFileMetadataPort.findByFileId("file-id")).willReturn(Optional.of(metadata));
        given(getChallengerRoleUseCase.isSuperAdmin(2L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> sut.deleteFile(deleteCommand("file-id", 2L)))
            .isInstanceOf(StorageException.class)
            .extracting("baseCode")
            .isEqualTo(StorageErrorCode.FILE_DELETE_FORBIDDEN);

        then(storagePort).should(never()).delete(anyString());
        then(saveFileMetadataPort).should(never()).deleteByFileId(anyString());
    }

    @Test
    @DisplayName("S3 삭제가 실패하면 파일 메타데이터를 삭제하지 않는다")
    void S3_삭제가_실패하면_파일_메타데이터를_삭제하지_않는다() {
        // given
        FileMetadata metadata = uploadedFile("file-id", 1L);
        given(loadFileMetadataPort.findByFileId("file-id")).willReturn(Optional.of(metadata));
        willThrow(new StorageException(StorageErrorCode.STORAGE_DELETE_FAILED))
            .given(storagePort)
            .delete(metadata.getStorageKey());

        // when & then
        assertThatThrownBy(() -> sut.deleteFile(deleteCommand("file-id", 1L)))
            .isInstanceOf(StorageException.class)
            .extracting("baseCode")
            .isEqualTo(StorageErrorCode.STORAGE_DELETE_FAILED);

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

    private FileMetadata uploadedFile(String fileId, Long uploadedMemberId) {
        FileMetadata metadata = FileMetadata.builder()
            .fileId(fileId)
            .originalFileName("document.pdf")
            .category(FileCategory.ETC)
            .contentType("application/pdf")
            .fileSize(1024L)
            .storageProvider(StorageProvider.AWS_S3)
            .storageKey("test/" + fileId + ".pdf")
            .uploadedMemberId(uploadedMemberId)
            .build();
        metadata.markAsUploaded();
        return metadata;
    }
}
