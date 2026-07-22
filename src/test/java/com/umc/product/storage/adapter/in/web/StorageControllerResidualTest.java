package com.umc.product.storage.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.storage.adapter.in.web.dto.FileResponse;
import com.umc.product.storage.adapter.in.web.dto.PrepareUploadRequest;
import com.umc.product.storage.application.port.in.command.ManageFileUseCase;
import com.umc.product.storage.application.port.in.command.dto.DeleteFileCommand;
import com.umc.product.storage.application.port.in.command.dto.FileUploadInfo;
import com.umc.product.storage.application.port.in.command.dto.PrepareFileUploadCommand;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;
import com.umc.product.storage.application.port.in.query.dto.FileInfo;
import com.umc.product.storage.domain.enums.FileCategory;

@DisplayName("StorageController 잔여 변환 계약")
class StorageControllerResidualTest {

    @Test
    @DisplayName("업로드 준비 요청에 인증 회원을 결합하고 usecase 응답을 반환한다")
    void 업로드_준비_요청을_변환한다() {
        ManageFileUseCase manageUseCase = mock(ManageFileUseCase.class);
        StorageController sut = new StorageController(manageUseCase, mock(GetFileUseCase.class));
        FileUploadInfo info = new FileUploadInfo(
            "file-id", "https://upload", "PUT", Map.of("Content-Type", "image/png"), LocalDateTime.now());
        given(manageUseCase.getFileUploadUrl(any())).willReturn(info);

        var response = sut.prepareUpload(
            new MemberPrincipal(10L), new PrepareUploadRequest("image.png", "image/png", 10L, FileCategory.POST_IMAGE));

        assertThat(response.getResult().fileId()).isEqualTo("file-id");
        ArgumentCaptor<PrepareFileUploadCommand> captor = ArgumentCaptor.forClass(PrepareFileUploadCommand.class);
        then(manageUseCase).should().getFileUploadUrl(captor.capture());
        assertThat(captor.getValue().uploadedBy()).isEqualTo(10L);
    }

    @Test
    @DisplayName("업로드 확인과 삭제는 file ID와 인증 회원을 usecase에 전달한다")
    void 업로드_확인과_삭제를_위임한다() {
        ManageFileUseCase manageUseCase = mock(ManageFileUseCase.class);
        StorageController sut = new StorageController(manageUseCase, mock(GetFileUseCase.class));

        assertThat(sut.confirmUpload("file-id").getResult()).isNull();
        assertThat(sut.deleteFile(new MemberPrincipal(10L), "file-id").getResult()).isNull();

        then(manageUseCase).should().confirmUpload("file-id");
        then(manageUseCase).should().deleteFile(new DeleteFileCommand("file-id", 10L));
    }

    @Test
    @DisplayName("파일 조회 DTO는 API의 legacy fileUrl 필드까지 보존한다")
    void 파일_조회_DTO를_변환한다() {
        FileInfo info = new FileInfo(
            "file-id", "image.png", FileCategory.POST_IMAGE, "image/png", 10L,
            "https://cdn/image.png", true, 10L, Instant.parse("2026-07-22T00:00:00Z"));

        FileResponse response = FileResponse.from(info);

        assertThat(response.fileId()).isEqualTo("file-id");
        assertThat(response.fileUrl()).isEqualTo("https://cdn/image.png");
    }
}
