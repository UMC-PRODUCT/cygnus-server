package com.umc.product.chat.application.policy;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.chat.application.port.out.LoadChatMemberPort;
import com.umc.product.chat.application.port.out.LoadChatRoomPort;
import com.umc.product.chat.domain.ChatRoom;
import com.umc.product.chat.domain.ChatRoomReadScope;
import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatRoomAccessPolicy")
class ChatRoomAccessPolicyTest {

    private static final Long ROOM_ID = 1L;
    private static final Long MEMBER_ID = 10L;

    @Mock
    LoadChatMemberPort loadChatMemberPort;
    @Mock
    LoadChatRoomPort loadChatRoomPort;

    @InjectMocks
    ChatRoomAccessPolicy sut;

    @Test
    @DisplayName("멤버는 방을 조회할 수 있고 조회 범위를 확인하지 않는다")
    void verifyReadable_memberSkipsRoomLookup() {
        given(loadChatMemberPort.existsByRoomIdAndMemberId(ROOM_ID, MEMBER_ID)).willReturn(true);

        assertThatCode(() -> sut.verifyReadable(ROOM_ID, MEMBER_ID)).doesNotThrowAnyException();

        then(loadChatRoomPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("PUBLIC 방은 비멤버도 조회할 수 있다")
    void verifyReadable_nonMemberOnPublicRoom() {
        given(loadChatMemberPort.existsByRoomIdAndMemberId(ROOM_ID, MEMBER_ID)).willReturn(false);
        given(loadChatRoomPort.findById(ROOM_ID))
            .willReturn(Optional.of(ChatRoom.create(ChatRoomReadScope.PUBLIC)));

        assertThatCode(() -> sut.verifyReadable(ROOM_ID, MEMBER_ID)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("MEMBER_ONLY 방은 비멤버 조회를 거부한다")
    void verifyReadable_nonMemberOnMemberOnlyRoom() {
        given(loadChatMemberPort.existsByRoomIdAndMemberId(ROOM_ID, MEMBER_ID)).willReturn(false);
        given(loadChatRoomPort.findById(ROOM_ID))
            .willReturn(Optional.of(ChatRoom.create(ChatRoomReadScope.MEMBER_ONLY)));

        assertAccessDenied(() -> sut.verifyReadable(ROOM_ID, MEMBER_ID));
    }

    @Test
    @DisplayName("존재하지 않는 방은 존재 여부를 노출하지 않고 접근 거부로 막는다")
    void verifyReadable_missingRoomFailsClosed() {
        given(loadChatMemberPort.existsByRoomIdAndMemberId(ROOM_ID, MEMBER_ID)).willReturn(false);
        given(loadChatRoomPort.findById(ROOM_ID)).willReturn(Optional.empty());

        assertAccessDenied(() -> sut.verifyReadable(ROOM_ID, MEMBER_ID));
    }

    @Test
    @DisplayName("변경 검증은 조회 범위와 무관하게 비멤버를 거부한다")
    void verifyMember_rejectsNonMemberRegardlessOfReadScope() {
        given(loadChatMemberPort.existsByRoomIdAndMemberId(ROOM_ID, MEMBER_ID)).willReturn(false);

        assertAccessDenied(() -> sut.verifyMember(ROOM_ID, MEMBER_ID));
        then(loadChatRoomPort).shouldHaveNoInteractions();
    }

    private void assertAccessDenied(Runnable action) {
        assertThatThrownBy(action::run)
            .isInstanceOf(ChatDomainException.class)
            .extracting(error -> ((ChatDomainException) error).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
    }
}
