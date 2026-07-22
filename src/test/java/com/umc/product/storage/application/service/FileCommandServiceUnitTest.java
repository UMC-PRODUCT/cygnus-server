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
import com.umc.product.storage.application.port.in.command.dto.GeneratedFileInfo;
import com.umc.product.storage.application.port.in.command.dto.PrepareFileUploadCommand;
import com.umc.product.storage.application.port.in.command.dto.StoreGeneratedFileCommand;
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

    @Test
    @DisplayName("서버 생성 파일은 객체 저장 후 업로드 완료 metadata를 저장한다")
    void 서버_생성_파일을_저장한다() {
        StoreGeneratedFileCommand command = StoreGeneratedFileCommand.of(
            "certificate.pdf", "application/pdf", new byte[]{1, 2, 3}, FileCategory.CERTIFICATE, 10L);

        GeneratedFileInfo result = sut.store(command);

        assertThat(result.fileSize()).isEqualTo(3L);
        assertThat(result.storageKey()).matches("private/certificate/.+\\.pdf");
        then(storagePort).should().uploadObject(result.storageKey(), "application/pdf", command.content());
        ArgumentCaptor<FileMetadata> captor = ArgumentCaptor.forClass(FileMetadata.class);
        then(saveFileMetadataPort).should().save(captor.capture());
        assertThat(captor.getValue().isUploaded()).isTrue();
        assertThat(captor.getValue().getUploadedMemberId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("서버 생성 파일은 확장자와 실제 byte 크기 제한을 외부 저장 전에 검증한다")
    void 서버_생성_파일의_확장자와_크기를_검증한다() {
        StoreGeneratedFileCommand noExtension = StoreGeneratedFileCommand.of(
            "certificate", "application/pdf", new byte[]{1}, FileCategory.CERTIFICATE, null);
        StoreGeneratedFileCommand blankName = StoreGeneratedFileCommand.of(
            " ", "application/pdf", new byte[]{1}, FileCategory.CERTIFICATE, null);
        StoreGeneratedFileCommand invalidExtension = StoreGeneratedFileCommand.of(
            "certificate.png", "image/png", new byte[]{1}, FileCategory.CERTIFICATE, null);
        StoreGeneratedFileCommand oversized = StoreGeneratedFileCommand.of(
            "image.png", "image/png", new byte[5 * 1024 * 1024 + 1], FileCategory.PROFILE_IMAGE, null);

        assertStorageError(() -> sut.store(noExtension), StorageErrorCode.INVALID_FILE_EXTENSION);
        assertStorageError(() -> sut.store(blankName), StorageErrorCode.INVALID_FILE_EXTENSION);
        assertStorageError(() -> sut.store(invalidExtension), StorageErrorCode.INVALID_FILE_EXTENSION);
        assertStorageError(() -> sut.store(oversized), StorageErrorCode.FILE_SIZE_EXCEEDED);
        then(storagePort).should(never()).uploadObject(
            anyString(), anyString(), org.mockito.ArgumentMatchers.any(byte[].class));
    }

    @Test
    @DisplayName("확장자가 선택인 ETC 파일은 점 없는 이름으로도 업로드 URL을 생성한다")
    void ETC는_확장자_없이_업로드_URL을_생성한다() {
        PrepareFileUploadCommand command =
            new PrepareFileUploadCommand("README", "text/plain", 10L, FileCategory.ETC, 1L);
        given(storagePort.generateUploadUrl(
            org.mockito.ArgumentMatchers.matches("public/etc/.+"),
            org.mockito.ArgumentMatchers.eq("text/plain"),
            org.mockito.ArgumentMatchers.eq(10L),
            org.mockito.ArgumentMatchers.eq(15L)
        )).willReturn(new FileUploadInfo(
            null, "https://upload", "PUT", Map.of(), LocalDateTime.now().plusMinutes(15)));

        assertThat(sut.getFileUploadUrl(command).uploadUrl()).isEqualTo("https://upload");
    }

    @Test
    @DisplayName("필수 확장자 누락·미허용 확장자·크기 초과는 metadata 저장 전에 거부한다")
    void 업로드_URL_요청의_파일_정책을_검증한다() {
        assertStorageError(() -> sut.getFileUploadUrl(new PrepareFileUploadCommand(
            ".hidden", "image/png", 1L, FileCategory.PROFILE_IMAGE, 1L)),
            StorageErrorCode.INVALID_FILE_EXTENSION);
        assertStorageError(() -> sut.getFileUploadUrl(new PrepareFileUploadCommand(
            "image.exe", "application/octet-stream", 1L, FileCategory.PROFILE_IMAGE, 1L)),
            StorageErrorCode.INVALID_FILE_EXTENSION);
        assertStorageError(() -> sut.getFileUploadUrl(new PrepareFileUploadCommand(
            "image.png", "image/png", 5 * 1024 * 1024 + 1L, FileCategory.PROFILE_IMAGE, 1L)),
            StorageErrorCode.FILE_SIZE_EXCEEDED);
        then(saveFileMetadataPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("업로드 확인은 metadata 미존재와 이미 완료된 중복 요청을 거부한다")
    void 업로드_확인의_미존재와_멱등성_경계를_검증한다() {
        given(loadFileMetadataPort.findByFileId("missing")).willReturn(Optional.empty());
        FileMetadata uploaded = uploadedFile("uploaded", 1L);
        given(loadFileMetadataPort.findByFileId("uploaded")).willReturn(Optional.of(uploaded));

        assertStorageError(() -> sut.confirmUpload("missing"), StorageErrorCode.FILE_NOT_FOUND);
        assertStorageError(() -> sut.confirmUpload("uploaded"), StorageErrorCode.FILE_ALREADY_UPLOADED);
    }

    @Test
    @DisplayName("검증 실패 객체 정리까지 실패해도 최초 검증 예외를 보존한다")
    void 잘못된_객체_정리_실패에도_최초_예외를_보존한다() {
        FileMetadata metadata = pendingFile("file-id", FileCategory.PORTFOLIO, 1024L, "application/pdf");
        given(loadFileMetadataPort.findByFileId("file-id")).willReturn(Optional.of(metadata));
        given(storagePort.findObjectInfoByStorageKey(metadata.getStorageKey()))
            .willReturn(Optional.of(StorageObjectInfo.of(metadata.getStorageKey(), 2048L, "application/pdf")));
        willThrow(new StorageException(StorageErrorCode.STORAGE_DELETE_FAILED))
            .given(storagePort)
            .delete(metadata.getStorageKey());

        assertStorageError(() -> sut.confirmUpload("file-id"), StorageErrorCode.FILE_SIZE_MISMATCH);
    }

    @Test
    @DisplayName("파일 삭제는 미존재를 거부하고 소유자는 관리자 조회 없이 삭제한다")
    void 삭제의_미존재와_소유자_단축을_검증한다() {
        given(loadFileMetadataPort.findByFileId("missing")).willReturn(Optional.empty());
        FileMetadata owned = uploadedFile("owned", 1L);
        given(loadFileMetadataPort.findByFileId("owned")).willReturn(Optional.of(owned));

        assertStorageError(() -> sut.deleteFile(deleteCommand("missing", 1L)), StorageErrorCode.FILE_NOT_FOUND);
        sut.deleteFile(deleteCommand("owned", 1L));

        then(getChallengerRoleUseCase).shouldHaveNoInteractions();
        then(storagePort).should().delete(owned.getStorageKey());
    }

    private void assertStorageError(Runnable action, StorageErrorCode errorCode) {
        assertThatThrownBy(action::run)
            .isInstanceOf(StorageException.class)
            .extracting("baseCode")
            .isEqualTo(errorCode);
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
