package com.umc.product.storage.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.storage.application.port.in.query.dto.FileMetadataInfo;
import com.umc.product.storage.application.port.out.LoadFileMetadataPort;
import com.umc.product.storage.application.port.out.StoragePort;
import com.umc.product.storage.domain.FileMetadata;
import com.umc.product.storage.domain.enums.FileCategory;
import com.umc.product.storage.domain.enums.StorageProvider;
import com.umc.product.storage.domain.exception.StorageErrorCode;
import com.umc.product.storage.domain.exception.StorageException;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileQueryService")
class FileQueryServiceUnitTest {

    @Mock
    StoragePort storagePort;

    @Mock
    LoadFileMetadataPort loadFileMetadataPort;

    @InjectMocks
    FileQueryService sut;

    @Test
    @DisplayName("회원이 업로드를 완료한 파일은 접근 URL 생성 없이 일괄 조회한다")
    void batchGetUsableByIds_success() {
        FileMetadata first = uploadedFile("file-1", "image.jpg", "image/jpeg", 10L);
        FileMetadata second = uploadedFile("file-2", "document.pdf", "application/pdf", 10L);
        given(loadFileMetadataPort.findByFileIds(List.of("file-1", "file-2")))
            .willReturn(List.of(second, first));

        List<FileMetadataInfo> result = sut.batchGetUsableByIds(List.of("file-1", "file-2"), 10L);

        assertThat(result).extracting(FileMetadataInfo::fileId).containsExactly("file-1", "file-2");
        assertThat(result).extracting(FileMetadataInfo::fileExtension).containsExactly("jpg", "pdf");
        then(storagePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("요청한 파일 중 하나라도 없으면 사용할 수 없다")
    void batchGetUsableByIds_notFound() {
        FileMetadata first = uploadedFile("file-1", "image.jpg", "image/jpeg", 10L);
        given(loadFileMetadataPort.findByFileIds(List.of("file-1", "missing"))).willReturn(List.of(first));

        assertStorageError(
            () -> sut.batchGetUsableByIds(List.of("file-1", "missing"), 10L),
            StorageErrorCode.FILE_NOT_FOUND
        );
    }

    @Test
    @DisplayName("업로드를 완료하지 않은 파일은 사용할 수 없다")
    void batchGetUsableByIds_notUploaded() {
        FileMetadata pending = file("file-1", "image.jpg", "image/jpeg", 10L);
        given(loadFileMetadataPort.findByFileIds(List.of("file-1"))).willReturn(List.of(pending));

        assertStorageError(
            () -> sut.batchGetUsableByIds(List.of("file-1"), 10L),
            StorageErrorCode.FILE_UPLOAD_NOT_COMPLETED
        );
    }

    @Test
    @DisplayName("다른 회원이 업로드한 파일은 사용할 수 없다")
    void batchGetUsableByIds_forbiddenOwner() {
        FileMetadata metadata = uploadedFile("file-1", "image.jpg", "image/jpeg", 20L);
        given(loadFileMetadataPort.findByFileIds(List.of("file-1"))).willReturn(List.of(metadata));

        assertStorageError(
            () -> sut.batchGetUsableByIds(List.of("file-1"), 10L),
            StorageErrorCode.FILE_USE_FORBIDDEN
        );
    }

    @Test
    @DisplayName("업로드한 회원 정보가 없는 파일은 사용할 수 없다")
    void batchGetUsableByIds_missingOwner() {
        FileMetadata metadata = uploadedFile("file-1", "image.jpg", "image/jpeg", null);
        given(loadFileMetadataPort.findByFileIds(List.of("file-1"))).willReturn(List.of(metadata));

        assertStorageError(
            () -> sut.batchGetUsableByIds(List.of("file-1"), 10L),
            StorageErrorCode.FILE_USE_FORBIDDEN
        );
    }

    private void assertStorageError(Runnable action, StorageErrorCode errorCode) {
        assertThatThrownBy(action::run)
            .isInstanceOf(StorageException.class)
            .extracting(e -> ((StorageException)e).getBaseCode())
            .isEqualTo(errorCode);
    }

    private FileMetadata uploadedFile(
        String fileId,
        String fileName,
        String contentType,
        Long uploadedMemberId
    ) {
        FileMetadata metadata = file(fileId, fileName, contentType, uploadedMemberId);
        metadata.markAsUploaded();
        return metadata;
    }

    private FileMetadata file(String fileId, String fileName, String contentType, Long uploadedMemberId) {
        return FileMetadata.builder()
            .fileId(fileId)
            .originalFileName(fileName)
            .category(FileCategory.ETC)
            .contentType(contentType)
            .fileSize(1024L)
            .storageProvider(StorageProvider.AWS_S3)
            .storageKey("test/" + fileId)
            .uploadedMemberId(uploadedMemberId)
            .build();
    }
}
