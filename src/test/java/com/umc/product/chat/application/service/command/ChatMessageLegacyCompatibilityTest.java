package com.umc.product.chat.application.service.command;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.chat.application.authorization.ChatPolicyAction;
import com.umc.product.chat.application.policy.ChatAttachmentPolicy;
import com.umc.product.chat.application.policy.ChatRoomAccessPolicy;
import com.umc.product.chat.application.port.in.command.UpdateChatReadUseCase;
import com.umc.product.chat.application.port.in.command.dto.MarkChatRoomReadCommand;
import com.umc.product.chat.application.port.in.command.dto.SendChatMessageCommand;
import com.umc.product.chat.application.port.in.command.dto.UpdateChatReadCommand;
import com.umc.product.chat.application.port.out.LoadChatMessagePort;
import com.umc.product.chat.application.port.out.LoadChatRoomPort;
import com.umc.product.chat.application.port.out.SaveChatMemberPort;
import com.umc.product.chat.application.port.out.SaveChatMessagePort;
import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;

@ExtendWith(MockitoExtension.class)
@DisplayName("legacy Chat message compatibility")
class ChatMessageLegacyCompatibilityTest {

    @Mock
    SaveChatMessagePort saveChatMessagePort;
    @Mock
    LoadChatMessagePort loadChatMessagePort;
    @Mock
    LoadChatRoomPort loadChatRoomPort;
    @Mock
    SaveChatMemberPort saveChatMemberPort;
    @Mock
    GetFileUseCase getFileUseCase;
    @Mock
    ChatAttachmentPolicy chatAttachmentPolicy;
    @Mock
    ChatRoomAccessPolicy chatRoomAccessPolicy;
    @Mock
    UpdateChatReadUseCase updateChatReadUseCase;
    @Mock
    DomainEventPublisher domainEventPublisher;

    @InjectMocks
    ChatMessageCommandService sut;

    @Test
    @DisplayName("legacy 첨부 send도 room lock 뒤 멤버십 검증 전에는 storage를 조회하지 않는다")
    void send_notMemberAfterLock() {
        SendChatMessageCommand command = new SendChatMessageCommand(
            1L,
            10L,
            MessageContentType.IMAGE,
            "캡션",
            List.of("file-1")
        );
        willThrow(new ChatDomainException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED))
            .given(chatRoomAccessPolicy).verifyMember(ChatPolicyAction.MESSAGE_CREATE, 1L, 10L);

        assertThatThrownBy(() -> sut.send(command))
            .isInstanceOf(ChatDomainException.class)
            .extracting(error -> ((ChatDomainException) error).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);

        then(loadChatRoomPort).should().getByIdForUpdate(1L);
        then(getFileUseCase).shouldHaveNoInteractions();
        then(saveChatMessagePort).shouldHaveNoInteractions();
        then(domainEventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("legacy markRead는 exact read UseCase에 동일한 식별자를 전달한다")
    void markRead_delegates() {
        sut.markRead(MarkChatRoomReadCommand.of(1L, 10L, 40L));

        then(updateChatReadUseCase).should().update(new UpdateChatReadCommand(1L, 10L, 40L));
    }

    @Test
    @DisplayName("legacy markRead는 exact read 오류를 그대로 전달한다")
    void markRead_propagatesError() {
        UpdateChatReadCommand update = new UpdateChatReadCommand(1L, 10L, 40L);
        willThrow(new ChatDomainException(ChatErrorCode.CHAT_MESSAGE_NOT_FOUND))
            .given(updateChatReadUseCase).update(update);

        assertThatThrownBy(() -> sut.markRead(MarkChatRoomReadCommand.of(1L, 10L, 40L)))
            .isInstanceOf(ChatDomainException.class)
            .extracting(error -> ((ChatDomainException) error).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_MESSAGE_NOT_FOUND);
    }
}
