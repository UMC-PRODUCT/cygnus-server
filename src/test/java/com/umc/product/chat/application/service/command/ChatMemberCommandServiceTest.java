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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.chat.application.port.in.command.dto.JoinChatRoomCommand;
import com.umc.product.chat.application.port.in.command.dto.LeaveChatRoomCommand;
import com.umc.product.chat.application.port.out.LoadChatMemberPort;
import com.umc.product.chat.application.port.out.LoadChatRoomPort;
import com.umc.product.chat.application.port.out.SaveChatMemberPort;
import com.umc.product.chat.application.service.ChatRoomOwnershipAccessService;
import com.umc.product.chat.domain.ChatMember;
import com.umc.product.chat.domain.ChatRoomActorContext;
import com.umc.product.chat.domain.ChatRoomOperation;
import com.umc.product.chat.domain.ChatRoomOwnerReference;
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
    SaveChatMemberPort saveChatMemberPort;
    @Mock
    ChatRoomOwnershipAccessService ownershipAccessService;

    @InjectMocks
    ChatMemberCommandService sut;

    @Test
    @DisplayName("채팅방 참여 시 원자적 삽입에 성공하면 정상 종료한다")
    void joinChatRoom_success() {
        JoinChatRoomCommand command = joinCommand(10L, 10L);
        given(saveChatMemberPort.saveIfAbsent(any(ChatMember.class))).willReturn(true);

        sut.joinChatRoom(command);

        ArgumentCaptor<ChatMember> captor = ArgumentCaptor.forClass(ChatMember.class);
        then(saveChatMemberPort).should().saveIfAbsent(captor.capture());
        assertThat(captor.getValue().getRoomId()).isEqualTo(1L);
        assertThat(captor.getValue().getMemberId()).isEqualTo(10L);
        then(ownershipAccessService).should().verifyForUpdate(
            owner(), ChatRoomOperation.JOIN, ChatRoomActorContext.actorAndTarget(10L, 10L));
        then(loadChatMemberPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("이미 참여 중이면 원자적 삽입 결과로 중복 멤버 예외를 던진다")
    void joinChatRoom_alreadyExists() {
        JoinChatRoomCommand command = joinCommand(10L, 10L);
        given(saveChatMemberPort.saveIfAbsent(any(ChatMember.class))).willReturn(false);

        assertThatThrownBy(() -> sut.joinChatRoom(command))
            .isInstanceOf(ChatDomainException.class)
            .extracting(e -> ((ChatDomainException) e).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_MEMBER_ALREADY_EXISTS);

        then(loadChatMemberPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("채팅방이 없으면 멤버를 저장하지 않는다")
    void joinChatRoom_roomNotFound() {
        JoinChatRoomCommand command = joinCommand(10L, 10L);
        willThrow(new ChatDomainException(ChatErrorCode.CHAT_ROOM_NOT_FOUND))
            .given(loadChatRoomPort).getById(1L);

        assertThatThrownBy(() -> sut.joinChatRoom(command))
            .isInstanceOf(ChatDomainException.class)
            .extracting(e -> ((ChatDomainException) e).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_ROOM_NOT_FOUND);

        then(saveChatMemberPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("자기 퇴장은 ownership LEAVE 검증 후 기존 membership을 삭제한다")
    void leaveChatRoom_self_success() {
        LeaveChatRoomCommand command = new LeaveChatRoomCommand(
            owner(), ChatRoomActorContext.actorAndTarget(10L, 10L));
        given(loadChatMemberPort.existsByRoomIdAndMemberId(1L, 10L)).willReturn(true);

        sut.leaveChatRoom(command);

        then(ownershipAccessService).should().verifyForUpdate(
            owner(), ChatRoomOperation.LEAVE, ChatRoomActorContext.actorAndTarget(10L, 10L));
        then(saveChatMemberPort).should().delete(1L, 10L);
    }

    @Test
    @DisplayName("requester와 target이 다른 참여 변경은 MEMBERSHIP_MANAGE 거부 후 저장하지 않는다")
    void joinChatRoom_otherTarget_denied() {
        JoinChatRoomCommand command = joinCommand(10L, 20L);
        willThrow(new ChatDomainException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED))
            .given(ownershipAccessService).verifyForUpdate(
                owner(), ChatRoomOperation.MEMBERSHIP_MANAGE, ChatRoomActorContext.actorAndTarget(10L, 20L));

        assertThatThrownBy(() -> sut.joinChatRoom(command))
            .isInstanceOf(ChatDomainException.class)
            .extracting(error -> ((ChatDomainException) error).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);

        then(loadChatRoomPort).shouldHaveNoInteractions();
        then(saveChatMemberPort).shouldHaveNoInteractions();
    }

    private JoinChatRoomCommand joinCommand(Long actorId, Long targetId) {
        return new JoinChatRoomCommand(owner(), ChatRoomActorContext.actorAndTarget(actorId, targetId));
    }

    private ChatRoomOwnerReference owner() {
        return ChatRoomOwnerReference.standalone(1L);
    }
}
