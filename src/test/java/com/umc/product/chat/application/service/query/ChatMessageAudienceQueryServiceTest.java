package com.umc.product.chat.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.chat.application.policy.ChatRoomAccessPolicy;
import com.umc.product.chat.application.port.in.query.dto.ChatMessageInfo;
import com.umc.product.chat.application.port.in.query.dto.GetChatMessageForViewersQuery;
import com.umc.product.chat.application.port.out.LoadChatMemberPort;
import com.umc.product.chat.application.port.out.LoadChatMessagePort;
import com.umc.product.chat.domain.ChatMember;
import com.umc.product.chat.domain.ChatMessage;
import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("Chat message audience query service")
class ChatMessageAudienceQueryServiceTest {

    @Mock
    LoadChatMessagePort loadChatMessagePort;
    @Mock
    LoadChatMemberPort loadChatMemberPort;
    @Mock
    ChatRoomAccessPolicy chatRoomAccessPolicy;
    @Mock
    ChatMessageInfoAssembler chatMessageInfoAssembler;

    @InjectMocks
    ChatMessageQueryService sut;

    @Test
    @DisplayName("여러 viewer의 접근을 한 번에 검증하고 메시지를 한 번만 조회해 개인화 projection을 반환한다")
    void getMessageForViewers_usesFixedBatchReads() {
        ChatMessage message = message();
        List<Long> viewers = List.of(10L, 20L);
        Map<Long, ChatMessageInfo> expected = new LinkedHashMap<>();
        expected.put(10L, ChatMessageInfo.from(message));
        expected.put(20L, ChatMessageInfo.from(message));
        given(loadChatMemberPort.listByRoomId(1L))
            .willReturn(List.of(ChatMember.of(1L, 10L), ChatMember.of(1L, 20L)));
        given(loadChatMessagePort.getByIdAndRoomId(100L, 1L)).willReturn(message);
        given(chatMessageInfoAssembler.assembleForViewers(message, viewers)).willReturn(expected);

        Map<Long, ChatMessageInfo> result = sut.getMessageForViewers(
            new GetChatMessageForViewersQuery(1L, 100L, viewers)
        );

        assertThat(result).isSameAs(expected);
        then(loadChatMemberPort).should().listByRoomId(1L);
        then(loadChatMessagePort).should().getByIdAndRoomId(100L, 1L);
        then(chatMessageInfoAssembler).should().assembleForViewers(message, viewers);
    }

    @Test
    @DisplayName("viewer 중 한 명이라도 Chat room 멤버가 아니면 메시지 조회 전에 접근을 거절한다")
    void getMessageForViewers_rejectsUnauthorizedViewerBeforeMessageRead() {
        given(loadChatMemberPort.listByRoomId(1L)).willReturn(List.of(ChatMember.of(1L, 10L)));

        assertThatThrownBy(() -> sut.getMessageForViewers(
            new GetChatMessageForViewersQuery(1L, 100L, List.of(10L, 20L))
        ))
            .isInstanceOf(ChatDomainException.class)
            .extracting(error -> ((ChatDomainException) error).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);

        then(loadChatMessagePort).shouldHaveNoInteractions();
        then(chatMessageInfoAssembler).shouldHaveNoInteractions();
    }

    private ChatMessage message() {
        ChatMessage message = ChatMessage.create(1L, 30L, MessageContentType.TEXT, "메시지", List.of());
        ReflectionTestUtils.setField(message, "id", 100L);
        return message;
    }
}
