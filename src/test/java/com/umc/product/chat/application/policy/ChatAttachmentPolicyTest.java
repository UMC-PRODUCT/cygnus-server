package com.umc.product.chat.application.policy;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;
import com.umc.product.storage.application.port.in.query.dto.FileMetadataInfo;
import com.umc.product.storage.domain.enums.FileCategory;

@DisplayName("ChatAttachmentPolicy")
class ChatAttachmentPolicyTest {

    private final ChatAttachmentPolicy sut = new ChatAttachmentPolicy();

    @ParameterizedTest
    @CsvSource({
        "jpg,image/jpeg",
        "jpeg,image/jpeg",
        "png,image/png",
        "webp,image/webp",
        "gif,image/gif"
    })
    @DisplayName("IMAGE 메시지는 허용된 이미지 확장자와 MIME 타입을 사용할 수 있다")
    void validate_imageSuccess(String extension, String contentType) {
        assertThatCode(() -> sut.validate(MessageContentType.IMAGE, List.of(file(extension, contentType))))
            .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({
        "pdf,application/pdf",
        "doc,application/msword",
        "docx,application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "xls,application/vnd.ms-excel",
        "xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "ppt,application/vnd.ms-powerpoint",
        "pptx,application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "zip,application/zip",
        "zip,application/x-zip-compressed"
    })
    @DisplayName("FILE 메시지는 허용된 일반 파일 확장자와 MIME 타입을 사용할 수 있다")
    void validate_fileSuccess(String extension, String contentType) {
        assertThatCode(() -> sut.validate(MessageContentType.FILE, List.of(file(extension, contentType))))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("MIME 타입의 파라미터를 제외한 미디어 타입이 일치하면 허용한다")
    void validate_contentTypeParameterSuccess() {
        assertThatCode(() -> sut.validate(
            MessageContentType.IMAGE,
            List.of(file("jpg", "image/jpeg; charset=UTF-8"))
        )).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("IMAGE 메시지에 일반 파일을 첨부할 수 없다")
    void validate_imageRejectsGeneralFile() {
        assertInvalidFileType(MessageContentType.IMAGE, file("pdf", "application/pdf"));
    }

    @Test
    @DisplayName("FILE 메시지에 이미지를 첨부할 수 없다")
    void validate_fileRejectsImage() {
        assertInvalidFileType(MessageContentType.FILE, file("png", "image/png"));
    }

    @Test
    @DisplayName("확장자와 MIME 타입이 일치하지 않으면 첨부할 수 없다")
    void validate_extensionMismatch() {
        assertInvalidFileType(MessageContentType.IMAGE, file("png", "image/jpeg"));
    }

    @Test
    @DisplayName("허용 목록에 없는 이미지 형식은 첨부할 수 없다")
    void validate_unsupportedImage() {
        assertInvalidFileType(MessageContentType.IMAGE, file("heic", "image/heic"));
    }

    private void assertInvalidFileType(MessageContentType contentType, FileMetadataInfo file) {
        assertThatThrownBy(() -> sut.validate(contentType, List.of(file)))
            .isInstanceOf(ChatDomainException.class)
            .extracting(e -> ((ChatDomainException)e).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_MESSAGE_INVALID_FILE_TYPE);
    }

    private FileMetadataInfo file(String extension, String contentType) {
        return new FileMetadataInfo(
            "file-id",
            "file." + extension,
            extension,
            FileCategory.ETC,
            contentType,
            1024L,
            true,
            10L,
            null
        );
    }
}
