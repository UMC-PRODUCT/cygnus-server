package com.umc.product.chat.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.chat.application.port.out.LoadChatMessagePort;
import com.umc.product.chat.domain.ChatMessage;
import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.storage.application.port.in.command.ManageFileUsageUseCase;
import com.umc.product.storage.application.port.in.command.dto.BulkRemoveFileUsagesCommand;
import com.umc.product.storage.application.port.in.command.dto.ReplaceFileUsagesCommand;
import com.umc.product.storage.domain.FileUsageCoordinate;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatMessage attachment usage 동기화")
class ChatMessageAttachmentUsageServiceTest {

    @Mock
    ManageFileUsageUseCase manageFileUsageUseCase;
    @Mock
    LoadChatMessagePort loadChatMessagePort;

    @InjectMocks
    ChatMessageAttachmentUsageService sut;

    @Test
    @DisplayName("message 저장 직후 message ID exact snapshot을 set으로 등록한다")
    void synchronizeMessage() {
        ChatMessage message = ChatMessage.create(
            1L, 7L, MessageContentType.IMAGE, null, List.of("file-b", "file-a"));
        ReflectionTestUtils.setField(message, "id", 41L);

        sut.synchronize(message, 7L);

        ArgumentCaptor<ReplaceFileUsagesCommand> captor =
            ArgumentCaptor.forClass(ReplaceFileUsagesCommand.class);
        then(manageFileUsageUseCase).should().replaceUsages(captor.capture());
        assertThat(captor.getValue().owner()).isEqualTo(
            FileUsageCoordinate.of("chat.message", "41", "attachments"));
        assertThat(captor.getValue().fileIds()).containsExactlyInAnyOrder("file-a", "file-b");
        assertThat(message.getFileMetadataIds()).containsExactly("file-b", "file-a");
    }

    @Test
    @DisplayName("room 삭제 전 message ID를 한 번에 detach한다")
    void detachByRoomId() {
        given(loadChatMessagePort.listIdsByRoomId(1L)).willReturn(List.of(43L, 41L));

        sut.detachByRoomId(1L);

        ArgumentCaptor<BulkRemoveFileUsagesCommand> captor =
            ArgumentCaptor.forClass(BulkRemoveFileUsagesCommand.class);
        then(manageFileUsageUseCase).should().removeAll(captor.capture());
        assertThat(captor.getValue().owners()).containsExactly(
            FileUsageCoordinate.of("chat.message", "43", "attachments"),
            FileUsageCoordinate.of("chat.message", "41", "attachments")
        );
    }
}
