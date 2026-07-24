package com.umc.product.chat.application.service.command;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.chat.application.authorization.ChatPolicyAction;
import com.umc.product.chat.application.policy.ChatRoomAccessPolicy;
import com.umc.product.chat.application.policy.CommunityChatMessagePolicy;
import com.umc.product.chat.application.port.in.command.dto.CreateChatMessageCommand;
import com.umc.product.chat.application.port.out.LoadChatMemberPort;
import com.umc.product.chat.application.port.out.LoadChatMessageMentionPort;
import com.umc.product.chat.application.port.out.LoadChatMessagePort;
import com.umc.product.chat.application.port.out.LoadChatRoomPort;
import com.umc.product.chat.application.port.out.SaveChatMemberPort;
import com.umc.product.chat.application.port.out.SaveChatMessageMentionPort;
import com.umc.product.chat.application.port.out.SaveChatMessagePort;
import com.umc.product.chat.application.port.out.SaveChatMessageReactionPort;
import com.umc.product.chat.application.service.query.ChatMessageInfoAssembler;
import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatMessage lifecycle reply 검증")
class ChatMessageLifecycleReplyValidationTest {

    @Mock
    SaveChatMessagePort saveChatMessagePort;
    @Mock
    LoadChatMessagePort loadChatMessagePort;
    @Mock
    LoadChatRoomPort loadChatRoomPort;
    @Mock
    LoadChatMemberPort loadChatMemberPort;
    @Mock
    SaveChatMemberPort saveChatMemberPort;
    @Mock
    LoadChatMessageMentionPort loadChatMessageMentionPort;
    @Mock
    SaveChatMessageMentionPort saveChatMessageMentionPort;
    @Mock
    SaveChatMessageReactionPort saveChatMessageReactionPort;
    @Mock
    CommunityChatMessagePolicy communityChatMessagePolicy;
    @Mock
    ChatRoomAccessPolicy chatRoomAccessPolicy;
    @Mock
    ChatMessageInfoAssembler chatMessageInfoAssembler;
    @Mock
    DomainEventPublisher domainEventPublisher;

    @InjectMocks
    ChatMessageLifecycleCommandService sut;

    @Test
    @DisplayName("잠금 아래 멤버십 재검증 후 다른 방 reply를 전용 오류로 거부한다")
    void create_invalidReplyTarget() {
        UUID clientMessageId = UUID.fromString("0c758b91-197a-49c4-ac14-d2bb2b8760a6");
        CreateChatMessageCommand command = new CreateChatMessageCommand(
            1L,
            10L,
            clientMessageId,
            MessageContentType.TEXT,
            "답장",
            List.of(),
            List.of(),
            90L
        );
        given(loadChatMessagePort.existsByIdAndRoomId(90L, 1L)).willReturn(false);

        assertThatThrownBy(() -> sut.create(command))
            .isInstanceOf(ChatDomainException.class)
            .extracting(error -> ((ChatDomainException) error).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_MESSAGE_INVALID_REPLY_TARGET);

        InOrder validationOrder = inOrder(loadChatRoomPort, chatRoomAccessPolicy, loadChatMessagePort);
        validationOrder.verify(loadChatRoomPort).getByIdForUpdate(1L);
        validationOrder.verify(chatRoomAccessPolicy)
            .verifyMember(ChatPolicyAction.MESSAGE_CREATE, 1L, 10L);
        validationOrder.verify(loadChatMessagePort).existsByIdAndRoomId(90L, 1L);
        then(loadChatMessagePort).should(never())
            .findByRoomIdAndSenderMemberIdAndClientMessageId(1L, 10L, clientMessageId);
        then(saveChatMessagePort).shouldHaveNoInteractions();
    }
}
