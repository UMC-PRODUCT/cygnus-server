package com.umc.product.chat.application.policy;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.UUID;
import java.util.stream.LongStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.chat.application.port.in.command.dto.CreateChatMessageCommand;
import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;
import com.umc.product.storage.application.port.in.query.dto.FileMetadataInfo;
import com.umc.product.storage.domain.enums.FileCategory;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommunityChatMessagePolicy")
class CommunityChatMessagePolicyTest {

    private static final long TEN_MEBIBYTES = 10L * 1024L * 1024L;

    @Mock
    GetFileUseCase getFileUseCase;
    @Mock
    ChatAttachmentPolicy chatAttachmentPolicy;

    @InjectMocks
    CommunityChatMessagePolicy sut;

    @Test
    @DisplayName("TEXT 본문은 정확히 2,000 code point까지 허용한다")
    void text_exactBoundary() {
        CreateChatMessageCommand command = text("가".repeat(2_000));

        assertThatCode(() -> sut.validateCreate(command)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("TEXT 본문이 2,000 code point를 넘으면 거부한다")
    void text_overBoundary() {
        CreateChatMessageCommand command = text("가".repeat(2_001));

        assertError(() -> sut.validateCreate(command), ChatErrorCode.CHAT_MESSAGE_INVALID_CONTENT_LENGTH);
    }

    @Test
    @DisplayName("IMAGE는 4개 파일과 파일당 10MiB 경계를 허용한다")
    void image_exactBoundary() {
        List<String> fileIds = List.of("file-1", "file-2", "file-3", "file-4");
        CreateChatMessageCommand command = image(fileIds);
        List<FileMetadataInfo> files = fileIds.stream()
            .map(fileId -> file(fileId, TEN_MEBIBYTES))
            .toList();
        given(getFileUseCase.batchGetUsableByIds(fileIds, 10L)).willReturn(files);

        assertThatCode(() -> {
            sut.validateCreate(command);
            sut.validateAttachments(command);
        }).doesNotThrowAnyException();

        then(chatAttachmentPolicy).should().validate(MessageContentType.IMAGE, files);
    }

    @Test
    @DisplayName("IMAGE 파일이 4개를 넘으면 거부한다")
    void image_tooManyFiles() {
        CreateChatMessageCommand command = image(List.of("1", "2", "3", "4", "5"));

        assertError(() -> sut.validateCreate(command), ChatErrorCode.CHAT_MESSAGE_INVALID_ATTACHMENT_COUNT);
    }

    @Test
    @DisplayName("IMAGE 한 파일이 10MiB를 넘으면 거부한다")
    void image_fileTooLarge() {
        CreateChatMessageCommand command = image(List.of("file-1"));
        given(getFileUseCase.batchGetUsableByIds(List.of("file-1"), 10L))
            .willReturn(List.of(file("file-1", TEN_MEBIBYTES + 1)));

        sut.validateCreate(command);

        assertError(
            () -> sut.validateAttachments(command),
            ChatErrorCode.CHAT_MESSAGE_ATTACHMENT_TOO_LARGE
        );
    }

    @Test
    @DisplayName("멘션은 정규화 후 최대 100명까지 허용한다")
    void mentions_maximum() {
        List<Long> mentions = LongStream.rangeClosed(1L, 100L).boxed().toList();
        CreateChatMessageCommand command = new CreateChatMessageCommand(
            1L,
            10L,
            UUID.randomUUID(),
            MessageContentType.TEXT,
            "본문",
            List.of(),
            mentions,
            null
        );

        assertThatCode(() -> sut.validateCreate(command)).doesNotThrowAnyException();
    }

    private CreateChatMessageCommand text(String content) {
        return new CreateChatMessageCommand(
            1L,
            10L,
            UUID.randomUUID(),
            MessageContentType.TEXT,
            content,
            List.of(),
            List.of(),
            null
        );
    }

    private CreateChatMessageCommand image(List<String> fileIds) {
        return new CreateChatMessageCommand(
            1L,
            10L,
            UUID.randomUUID(),
            MessageContentType.IMAGE,
            null,
            fileIds,
            List.of(),
            null
        );
    }

    private FileMetadataInfo file(String fileId, long size) {
        return new FileMetadataInfo(
            fileId,
            fileId + ".png",
            "png",
            FileCategory.ETC,
            "image/png",
            size,
            true,
            10L,
            null
        );
    }

    private void assertError(Runnable action, ChatErrorCode expected) {
        assertThatThrownBy(action::run)
            .isInstanceOf(ChatDomainException.class)
            .extracting(error -> ((ChatDomainException) error).getBaseCode())
            .isEqualTo(expected);
    }
}
