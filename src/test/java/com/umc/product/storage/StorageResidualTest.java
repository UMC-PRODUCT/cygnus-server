package com.umc.product.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.storage.application.port.in.command.dto.CreateFileUploadUrlCommand;
import com.umc.product.storage.application.port.in.command.dto.DeleteFileCommand;
import com.umc.product.storage.application.port.in.command.dto.FileUploadInfo;
import com.umc.product.storage.application.port.in.command.dto.GeneratedFileInfo;
import com.umc.product.storage.application.port.in.command.dto.PrepareFileUploadCommand;
import com.umc.product.storage.application.port.in.command.dto.StoreGeneratedFileCommand;
import com.umc.product.storage.application.port.out.StoragePort;
import com.umc.product.storage.domain.enums.FileCategory;

@DisplayName("storage DTO와 port 잔여 계약")
class StorageResidualTest {

    @Test
    @DisplayName("생성 파일 command는 유효 기본값과 정확한 byte 크기를 제공한다")
    void 생성_파일_command의_팩토리와_크기를_검증한다() {
        StoreGeneratedFileCommand command = StoreGeneratedFileCommand.of(
            "certificate.pdf", "application/pdf", new byte[]{1, 2, 3}, FileCategory.CERTIFICATE, 10L);
        GeneratedFileInfo info = GeneratedFileInfo.of("file-id", "private/certificate/file-id.pdf", 3L);
        CreateFileUploadUrlCommand legacy =
            new CreateFileUploadUrlCommand("certificate.pdf", "application/pdf", 3L);

        assertThat(command.fileSize()).isEqualTo(3L);
        assertThat(command.generatedByMemberId()).isEqualTo(10L);
        assertThat(info.fileSize()).isEqualTo(3L);
        assertThat(legacy.fileName()).isEqualTo("certificate.pdf");
    }

    @Test
    @DisplayName("생성 파일 command는 null 필드와 빈 content를 즉시 거부한다")
    void 생성_파일_command의_필수값을_검증한다() {
        byte[] content = {1};

        assertThatThrownBy(() -> StoreGeneratedFileCommand.of(
            null, "application/pdf", content, FileCategory.CERTIFICATE, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> StoreGeneratedFileCommand.of(
            "a.pdf", null, content, FileCategory.CERTIFICATE, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> StoreGeneratedFileCommand.of(
            "a.pdf", "application/pdf", null, FileCategory.CERTIFICATE, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> StoreGeneratedFileCommand.of(
            "a.pdf", "application/pdf", content, null, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> StoreGeneratedFileCommand.of(
            "a.pdf", "application/pdf", new byte[0], FileCategory.CERTIFICATE, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("업로드 준비 command는 보조 생성자와 null·blank·0 경계를 검증한다")
    void 업로드_준비_command의_경계를_검증한다() {
        PrepareFileUploadCommand valid =
            new PrepareFileUploadCommand("a.pdf", "application/pdf", 1L, FileCategory.CERTIFICATE);

        assertThat(valid.uploadedBy()).isNull();
        assertThatThrownBy(() -> new PrepareFileUploadCommand(
            null, "application/pdf", 1L, FileCategory.CERTIFICATE)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PrepareFileUploadCommand(
            "a.pdf", null, 1L, FileCategory.CERTIFICATE)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PrepareFileUploadCommand(
            "a.pdf", "application/pdf", null, FileCategory.CERTIFICATE)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PrepareFileUploadCommand(
            "a.pdf", "application/pdf", 1L, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PrepareFileUploadCommand(
            " ", "application/pdf", 1L, FileCategory.CERTIFICATE)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PrepareFileUploadCommand(
            "a.pdf", "application/pdf", 0L, FileCategory.CERTIFICATE)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("삭제 command는 null과 blank 식별자를 거부한다")
    void 삭제_command의_필수값을_검증한다() {
        assertThatThrownBy(() -> new DeleteFileCommand(null, 1L)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new DeleteFileCommand("file", null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new DeleteFileCommand(" ", 1L)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("업로드 정보는 만료 전후를 현재 시각 기준으로 판단한다")
    void 업로드_정보_만료를_판단한다() {
        FileUploadInfo expired = new FileUploadInfo("1", "url", "PUT", Map.of(), LocalDateTime.now().minusSeconds(1));
        FileUploadInfo active = new FileUploadInfo("2", "url", "PUT", Map.of(), LocalDateTime.now().plusMinutes(1));

        assertThat(expired.isExpired()).isTrue();
        assertThat(active.isExpired()).isFalse();
    }

    @Test
    @DisplayName("기본 storage port는 크기 없는 URL 생성에 위임하고 빈 metadata와 안정적인 key를 제공한다")
    void storage_port_기본_계약을_검증한다() {
        StoragePort port = mock(StoragePort.class, org.mockito.Mockito.CALLS_REAL_METHODS);
        FileUploadInfo info = new FileUploadInfo("1", "url", "PUT", Map.of(), LocalDateTime.now());
        given(port.generateUploadUrl("key", "type", 15L)).willReturn(info);

        assertThat(port.generateUploadUrl("key", "type", 10L, 15L)).isSameAs(info);
        assertThat(port.findObjectInfoByStorageKey("missing")).isEmpty();
        assertThat(port.generateStorageKey(FileCategory.ETC, "id", null)).isEqualTo("public/etc/id");
        assertThat(port.generateStorageKey(FileCategory.ETC, "id", " ")).isEqualTo("public/etc/id");
        assertThat(port.generateStorageKey(FileCategory.ETC, "id", "pdf")).isEqualTo("public/etc/id.pdf");
        then(port).should().generateUploadUrl("key", "type", 15L);
    }
}
