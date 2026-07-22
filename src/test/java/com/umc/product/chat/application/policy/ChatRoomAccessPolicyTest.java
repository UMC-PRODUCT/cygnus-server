package com.umc.product.chat.application.policy;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.chat.application.port.out.LoadChatMemberPort;
import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;

@DisplayName("ChatRoomAccessPolicy")
class ChatRoomAccessPolicyTest {

    private final LoadChatMemberPort loadChatMemberPort = org.mockito.Mockito.mock(LoadChatMemberPort.class);
    private final ChatRoomAccessPolicy sut = new ChatRoomAccessPolicy(loadChatMemberPort);

    @Test
    @DisplayName("방 멤버는 접근할 수 있고 비멤버는 fail-closed 처리한다")
    void verify_member() {
        given(loadChatMemberPort.existsByRoomIdAndMemberId(1L, 10L)).willReturn(true);

        assertThatCode(() -> sut.verifyMember(1L, 10L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> sut.verifyMember(1L, 20L))
            .isInstanceOf(ChatDomainException.class)
            .extracting(error -> ((ChatDomainException) error).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
    }
}
