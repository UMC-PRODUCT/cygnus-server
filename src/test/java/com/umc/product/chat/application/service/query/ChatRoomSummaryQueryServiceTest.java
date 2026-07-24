package com.umc.product.chat.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.chat.application.authorization.ChatPolicyAction;
import com.umc.product.chat.application.policy.ChatRoomAccessPolicy;
import com.umc.product.chat.application.port.in.query.dto.ChatRoomSummaryInfo;
import com.umc.product.chat.application.port.out.LoadChatMemberPort;
import com.umc.product.chat.application.port.out.LoadChatMessagePort;
import com.umc.product.chat.application.port.out.dto.RoomUnreadCount;
import com.umc.product.chat.domain.ChatMessage;
import com.umc.product.chat.domain.MessageContentType;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatMessageQueryService room summaries")
class ChatRoomSummaryQueryServiceTest {

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
    @DisplayName("roomId 집합이 비어 있으면 port를 호출하지 않고 빈 목록을 반환한다")
    void listRoomSummaries_emptyInput() {
        List<ChatRoomSummaryInfo> result = sut.listRoomSummaries(10L, List.of());

        assertThat(result).isEmpty();
        then(loadChatMemberPort).shouldHaveNoInteractions();
        then(loadChatMessagePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("참여 중인 방이 없으면 message query 없이 빈 목록을 반환한다")
    void listRoomSummaries_noMembership() {
        given(loadChatMemberPort.listRoomIdsByMemberIdAndRoomIdIn(10L, List.of(1L, 2L)))
            .willReturn(List.of());

        List<ChatRoomSummaryInfo> result = sut.listRoomSummaries(10L, List.of(1L, 2L));

        assertThat(result).isEmpty();
        then(loadChatMessagePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("참여 중인 방의 last message와 watermark unread를 batch 조립한다")
    void listRoomSummaries_assemble() {
        List<Long> ownedRoomIds = List.of(1L, 2L, 3L);
        given(loadChatMemberPort.listRoomIdsByMemberIdAndRoomIdIn(10L, ownedRoomIds))
            .willReturn(List.of(1L, 2L, 3L));
        given(chatRoomAccessPolicy.authorizeDerivedMembership(
            ChatPolicyAction.ROOM_SUMMARY_READ,
            true)).willReturn(true);
        given(loadChatMessagePort.listLatestPerRoom(List.of(1L, 2L, 3L)))
            .willReturn(List.of(message(100L, 1L), message(90L, 2L)));
        given(loadChatMessagePort.countUnreadByRooms(10L, List.of(1L, 2L, 3L)))
            .willReturn(List.of(new RoomUnreadCount(1L, 5L)));

        List<ChatRoomSummaryInfo> result = sut.listRoomSummaries(10L, ownedRoomIds);

        assertThat(result).extracting("roomId").containsExactly(1L, 2L, 3L);
        assertThat(result.get(0).lastMessage().messageId()).isEqualTo(100L);
        assertThat(result.get(0).unreadCount()).isEqualTo(5L);
        assertThat(result.get(1).lastMessage().messageId()).isEqualTo(90L);
        assertThat(result.get(1).unreadCount()).isZero();
        assertThat(result.get(2).lastMessage()).isNull();
        assertThat(result.get(2).unreadCount()).isZero();
        then(loadChatMessagePort).should(times(1)).listLatestPerRoom(List.of(1L, 2L, 3L));
        then(loadChatMessagePort).should(times(1)).countUnreadByRooms(10L, List.of(1L, 2L, 3L));
    }

    @Test
    @DisplayName("consumer가 전달한 roomId 밖의 다른 domain 방은 결과에서 격리한다")
    void listRoomSummaries_isolatesConsumerScopes() {
        List<Long> inquiryRoomIds = List.of(1L);
        given(loadChatMemberPort.listRoomIdsByMemberIdAndRoomIdIn(10L, inquiryRoomIds))
            .willReturn(List.of(1L));
        given(chatRoomAccessPolicy.authorizeDerivedMembership(
            ChatPolicyAction.ROOM_SUMMARY_READ,
            true)).willReturn(true);
        given(loadChatMessagePort.listLatestPerRoom(List.of(1L)))
            .willReturn(List.of(message(100L, 1L)));
        given(loadChatMessagePort.countUnreadByRooms(10L, List.of(1L)))
            .willReturn(List.of());

        List<ChatRoomSummaryInfo> result = sut.listRoomSummaries(10L, inquiryRoomIds);

        assertThat(result).extracting("roomId").containsExactly(1L);
        then(loadChatMessagePort).should(times(1)).listLatestPerRoom(List.of(1L));
        then(loadChatMessagePort).should(times(1)).countUnreadByRooms(10L, List.of(1L));
    }

    private ChatMessage message(Long id, Long roomId) {
        ChatMessage message = ChatMessage.create(roomId, 99L, MessageContentType.TEXT, "msg", null);
        ReflectionTestUtils.setField(message, "id", id);
        return message;
    }
}
