package com.umc.product.chat.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.chat.application.port.in.command.dto.JoinChatRoomCommand;
import com.umc.product.chat.application.port.in.command.dto.LeaveChatRoomCommand;
import com.umc.product.chat.application.port.out.LoadChatMemberPort;
import com.umc.product.chat.application.port.out.LoadChatMessagePort;
import com.umc.product.chat.application.port.out.LoadChatRoomPort;
import com.umc.product.chat.application.port.out.SaveChatMemberPort;
import com.umc.product.chat.domain.ChatMember;
import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatMemberCommandService")
class ChatMemberCommandServiceTest {

    @Mock
    LoadChatRoomPort loadChatRoomPort;
    @Mock
    LoadChatMemberPort loadChatMemberPort;
    @Mock
    LoadChatMessagePort loadChatMessagePort;
    @Mock
    SaveChatMemberPort saveChatMemberPort;

    @InjectMocks
    ChatMemberCommandService sut;

    @Test
    @DisplayName("채팅방 참여 시 원자적 삽입에 성공하면 정상 종료한다")
    void joinChatRoom_success() {
        JoinChatRoomCommand command = new JoinChatRoomCommand(1L, 10L);
        given(saveChatMemberPort.saveIfAbsent(any(ChatMember.class))).willReturn(true);

        sut.joinChatRoom(command);

        ArgumentCaptor<ChatMember> captor = ArgumentCaptor.forClass(ChatMember.class);
        then(saveChatMemberPort).should().saveIfAbsent(captor.capture());
        assertThat(captor.getValue().getRoomId()).isEqualTo(1L);
        assertThat(captor.getValue().getMemberId()).isEqualTo(10L);
        then(loadChatRoomPort).should().getByIdForUpdate(1L);
        then(loadChatMemberPort).should().existsByRoomIdAndMemberId(1L, 10L);
        then(loadChatMessagePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("초기 읽음 위치가 같은 채팅방 메시지인지 검증하고 새 멤버에 저장한다")
    void joinChatRoom_initialReadWatermark() {
        JoinChatRoomCommand command = new JoinChatRoomCommand(1L, 10L, 50L);
        given(saveChatMemberPort.saveIfAbsent(any(ChatMember.class))).willReturn(true);

        sut.joinChatRoom(command);

        ArgumentCaptor<ChatMember> captor = ArgumentCaptor.forClass(ChatMember.class);
        InOrder order = Mockito.inOrder(
            loadChatRoomPort,
            loadChatMemberPort,
            loadChatMessagePort,
            saveChatMemberPort
        );
        order.verify(loadChatRoomPort).getByIdForUpdate(1L);
        order.verify(loadChatMemberPort).existsByRoomIdAndMemberId(1L, 10L);
        order.verify(loadChatMessagePort).getByIdAndRoomId(50L, 1L);
        order.verify(saveChatMemberPort).saveIfAbsent(captor.capture());
        assertThat(captor.getValue().getLastReadMessageId()).isEqualTo(50L);
        order.verify(saveChatMemberPort).bumpLastReadMessageId(1L, 10L, 50L);
    }

    @Test
    @DisplayName("초기 읽음 위치가 같은 채팅방 메시지가 아니면 멤버를 저장하지 않는다")
    void joinChatRoom_initialReadWatermarkFromAnotherRoom() {
        JoinChatRoomCommand command = new JoinChatRoomCommand(1L, 10L, 50L);
        willThrow(new ChatDomainException(ChatErrorCode.CHAT_MESSAGE_NOT_FOUND))
            .given(loadChatMessagePort).getByIdAndRoomId(50L, 1L);

        assertThatThrownBy(() -> sut.joinChatRoom(command))
            .isInstanceOf(ChatDomainException.class)
            .extracting(e -> ((ChatDomainException) e).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_MESSAGE_NOT_FOUND);

        then(saveChatMemberPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("채팅방 잠금 뒤 이미 참여 중인지 다시 확인한다")
    void joinChatRoom_alreadyExists() {
        JoinChatRoomCommand command = new JoinChatRoomCommand(1L, 10L);
        given(loadChatMemberPort.existsByRoomIdAndMemberId(1L, 10L)).willReturn(true);

        assertThatThrownBy(() -> sut.joinChatRoom(command))
            .isInstanceOf(ChatDomainException.class)
            .extracting(e -> ((ChatDomainException) e).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_MEMBER_ALREADY_EXISTS);

        then(loadChatRoomPort).should().getByIdForUpdate(1L);
        then(saveChatMemberPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("사전 조회 뒤 동시 삽입이 발생하면 원자적 save 결과로 중복 참여를 거절한다")
    void joinChatRoom_concurrentInsertRejected() {
        JoinChatRoomCommand command = new JoinChatRoomCommand(1L, 10L);
        given(loadChatMemberPort.existsByRoomIdAndMemberId(1L, 10L)).willReturn(false);
        given(saveChatMemberPort.saveIfAbsent(any(ChatMember.class))).willReturn(false);

        assertThatThrownBy(() -> sut.joinChatRoom(command))
            .isInstanceOf(ChatDomainException.class)
            .extracting(error -> ((ChatDomainException) error).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_MEMBER_ALREADY_EXISTS);
        then(saveChatMemberPort).should().saveIfAbsent(any(ChatMember.class));
    }

    @Test
    @DisplayName("채팅방이 없으면 멤버를 저장하지 않는다")
    void joinChatRoom_roomNotFound() {
        JoinChatRoomCommand command = new JoinChatRoomCommand(1L, 10L);
        willThrow(new ChatDomainException(ChatErrorCode.CHAT_ROOM_NOT_FOUND))
            .given(loadChatRoomPort).getByIdForUpdate(1L);

        assertThatThrownBy(() -> sut.joinChatRoom(command))
            .isInstanceOf(ChatDomainException.class)
            .extracting(e -> ((ChatDomainException) e).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_ROOM_NOT_FOUND);

        then(saveChatMemberPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("채팅방 잠금 뒤 멤버십을 다시 확인하고 퇴장한다")
    void leaveChatRoom_success() {
        given(loadChatMemberPort.existsByRoomIdAndMemberId(1L, 10L)).willReturn(true);

        sut.leaveChatRoom(LeaveChatRoomCommand.of(1L, 10L));

        then(loadChatRoomPort).should().getByIdForUpdate(1L);
        then(saveChatMemberPort).should().delete(1L, 10L);
    }

    @Test
    @DisplayName("잠금 획득 전에 참여했더라도 최종 재검증에서 없으면 퇴장하지 않는다")
    void leaveChatRoom_memberMissingAfterLock() {
        given(loadChatMemberPort.existsByRoomIdAndMemberId(1L, 10L)).willReturn(false);

        assertThatThrownBy(() -> sut.leaveChatRoom(LeaveChatRoomCommand.of(1L, 10L)))
            .isInstanceOf(ChatDomainException.class)
            .extracting(e -> ((ChatDomainException) e).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_MEMBER_NOT_FOUND);

        then(loadChatRoomPort).should().getByIdForUpdate(1L);
        then(saveChatMemberPort).shouldHaveNoInteractions();
    }
}
