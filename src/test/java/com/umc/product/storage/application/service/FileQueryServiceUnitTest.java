package com.umc.product.storage.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.storage.application.port.in.query.dto.FileInfo;
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

    @Test
    @DisplayName("단건 조회는 signed URL을 결합하고 미존재는 예외로 변환한다")
    void 단건_조회를_검증한다() {
        FileMetadata metadata = uploadedFile("file-1", "image.jpg", "image/jpeg", 10L);
        given(loadFileMetadataPort.findByFileId("file-1")).willReturn(java.util.Optional.of(metadata));
        given(loadFileMetadataPort.findByFileId("missing")).willReturn(java.util.Optional.empty());
        given(storagePort.generateAccessUrl(metadata.getStorageKey(), 60L)).willReturn("https://cdn/file-1");

        FileInfo result = sut.getById("file-1");

        assertThat(result.fileLink()).isEqualTo("https://cdn/file-1");
        assertStorageError(() -> sut.getById("missing"), StorageErrorCode.FILE_NOT_FOUND);
    }

    @Test
    @DisplayName("batch 링크와 상세 조회는 DB 반환 데이터만 signed URL과 함께 map으로 변환한다")
    void batch_링크와_상세를_변환한다() {
        FileMetadata metadata = uploadedFile("file-1", "image.jpg", "image/jpeg", 10L);
        given(loadFileMetadataPort.findByFileIds(List.of("file-1", "missing"))).willReturn(List.of(metadata));
        given(storagePort.generateAccessUrl(metadata.getStorageKey(), 60L)).willReturn("https://cdn/file-1");

        Map<String, String> links = sut.getFileLinks(List.of("file-1", "missing"));
        Map<String, FileInfo> infos = sut.findAllByIds(List.of("file-1", "missing"));

        assertThat(links).containsOnlyKeys("file-1").containsValue("https://cdn/file-1");
        assertThat(infos.get("file-1").fileLink()).isEqualTo("https://cdn/file-1");
    }

    @Test
    @DisplayName("빈 batch는 DB를 조회하지 않고 중복 ID는 최초 순서로 한 번씩 반환한다")
    void 빈_batch와_중복_ID를_처리한다() {
        assertThat(sut.batchGetUsableByIds(null, 10L)).isEmpty();
        assertThat(sut.batchGetUsableByIds(List.of(), 10L)).isEmpty();
        FileMetadata metadata = uploadedFile("file-1", "image.jpg", "image/jpeg", 10L);
        given(loadFileMetadataPort.findByFileIds(List.of("file-1"))).willReturn(List.of(metadata));

        assertThat(sut.batchGetUsableByIds(List.of("file-1", "file-1"), 10L))
            .extracting(FileMetadataInfo::fileId)
            .containsExactly("file-1");
    }

    @Test
    @DisplayName("batch의 null·blank ID와 null 회원은 fail-closed한다")
    void batch_입력을_fail_closed한다() {
        assertStorageError(() -> sut.batchGetUsableByIds(java.util.Arrays.asList("file-1", null), 10L),
            StorageErrorCode.FILE_NOT_FOUND);
        assertStorageError(() -> sut.batchGetUsableByIds(List.of(" "), 10L), StorageErrorCode.FILE_NOT_FOUND);
        FileMetadata metadata = uploadedFile("file-1", "image.jpg", "image/jpeg", 10L);
        given(loadFileMetadataPort.findByFileIds(List.of("file-1"))).willReturn(List.of(metadata));
        assertStorageError(() -> sut.batchGetUsableByIds(List.of("file-1"), null),
            StorageErrorCode.FILE_USE_FORBIDDEN);
    }

    @Test
    @DisplayName("존재 확인과 throw 계약은 port 결과를 정확히 반영한다")
    void 존재_확인_계약을_검증한다() {
        given(loadFileMetadataPort.existsByFileId("exists")).willReturn(true);
        given(loadFileMetadataPort.existsByFileId("missing")).willReturn(false);

        assertThat(sut.existsById("exists")).isTrue();
        sut.throwIfNotExists("exists");
        assertStorageError(() -> sut.throwIfNotExists("missing"), StorageErrorCode.FILE_NOT_FOUND);
        then(storagePort).should(never()).delete(org.mockito.ArgumentMatchers.anyString());
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
