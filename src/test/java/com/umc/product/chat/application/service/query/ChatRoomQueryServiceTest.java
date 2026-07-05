package com.umc.product.chat.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.chat.application.port.in.query.dto.ChatRoomInfo;
import com.umc.product.chat.application.port.out.LoadChatMemberPort;
import com.umc.product.chat.application.port.out.LoadChatRoomPort;
import com.umc.product.chat.domain.ChatMember;
import com.umc.product.chat.domain.ChatRoom;
import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatRoomQueryService")
class ChatRoomQueryServiceTest {

    @Mock
    LoadChatRoomPort loadChatRoomPort;
    @Mock
    LoadChatMemberPort loadChatMemberPort;

    @InjectMocks
    ChatRoomQueryService sut;

    @Test
    @DisplayName("방 멤버이면 채팅방 상세 정보와 참여자 목록을 조회한다")
    void getById_success() {
        ChatRoom room = room(1L);
        Instant createdAt = Instant.parse("2026-06-13T00:00:00Z");
        ReflectionTestUtils.setField(room, "createdAt", createdAt);

        given(loadChatMemberPort.existsByRoomIdAndMemberId(1L, 10L)).willReturn(true);
        given(loadChatRoomPort.getById(1L)).willReturn(room);
        given(loadChatMemberPort.listByRoomId(1L))
            .willReturn(List.of(member(1L, 10L), member(1L, 20L)));

        ChatRoomInfo result = sut.getById(1L, 10L);

        assertThat(result.roomId()).isEqualTo(1L);
        assertThat(result.createdAt()).isEqualTo(createdAt);
        assertThat(result.memberIds()).containsExactly(10L, 20L);
    }

    @Test
    @DisplayName("방 멤버가 아니면 채팅방 상세 정보를 조회하지 않고 접근 거부 예외를 던진다")
    void getById_accessDenied() {
        given(loadChatMemberPort.existsByRoomIdAndMemberId(1L, 99L)).willReturn(false);

        assertThatThrownBy(() -> sut.getById(1L, 99L))
            .isInstanceOf(ChatDomainException.class)
            .extracting(e -> ((ChatDomainException) e).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);

        then(loadChatRoomPort).shouldHaveNoInteractions();
    }

    private ChatRoom room(Long id) {
        ChatRoom room = ChatRoom.create();
        ReflectionTestUtils.setField(room, "id", id);
        return room;
    }

    private ChatMember member(Long roomId, Long memberId) {
        return ChatMember.of(roomId, memberId);
    }
}
