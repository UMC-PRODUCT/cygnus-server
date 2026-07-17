package com.umc.product.chat.application.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.chat.application.port.in.command.JoinChatRoomUseCase;
import com.umc.product.chat.application.port.in.command.LeaveChatRoomUseCase;
import com.umc.product.chat.application.port.in.command.dto.JoinChatRoomCommand;
import com.umc.product.chat.application.port.in.command.dto.LeaveChatRoomCommand;
import com.umc.product.chat.application.port.out.LoadChatMemberPort;
import com.umc.product.chat.application.port.out.LoadChatRoomPort;
import com.umc.product.chat.application.port.out.SaveChatMemberPort;
import com.umc.product.chat.application.service.ChatRoomOwnershipAccessService;
import com.umc.product.chat.domain.ChatMember;
import com.umc.product.chat.domain.ChatRoomActorContext;
import com.umc.product.chat.domain.ChatRoomOperation;
import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatMemberCommandService implements JoinChatRoomUseCase, LeaveChatRoomUseCase {

    private final LoadChatRoomPort loadChatRoomPort;
    private final LoadChatMemberPort loadChatMemberPort;
    private final SaveChatMemberPort saveChatMemberPort;
    private final ChatRoomOwnershipAccessService ownershipAccessService;

    @Override
    public void joinChatRoom(JoinChatRoomCommand command) {
        ChatRoomActorContext actorContext = command.actorContext();
        ChatRoomOperation operation = membershipOperation(actorContext, ChatRoomOperation.JOIN);
        ownershipAccessService.verifyForUpdate(command.expectedOwner(), operation, actorContext);
        Long roomId = command.expectedOwner().roomId();
        Long targetMemberId = targetMemberId(actorContext);
        loadChatRoomPort.getById(roomId);
        if (!saveChatMemberPort.saveIfAbsent(ChatMember.of(roomId, targetMemberId))) {
            throw new ChatDomainException(ChatErrorCode.CHAT_MEMBER_ALREADY_EXISTS);
        }
    }

    @Override
    public void leaveChatRoom(LeaveChatRoomCommand command) {
        ChatRoomActorContext actorContext = command.actorContext();
        ChatRoomOperation operation = membershipOperation(actorContext, ChatRoomOperation.LEAVE);
        ownershipAccessService.verifyForUpdate(command.expectedOwner(), operation, actorContext);
        Long roomId = command.expectedOwner().roomId();
        Long targetMemberId = targetMemberId(actorContext);
        if (!loadChatMemberPort.existsByRoomIdAndMemberId(roomId, targetMemberId)) {
            throw new ChatDomainException(ChatErrorCode.CHAT_MEMBER_NOT_FOUND);
        }
        saveChatMemberPort.delete(roomId, targetMemberId);
    }

    private ChatRoomOperation membershipOperation(
        ChatRoomActorContext actorContext,
        ChatRoomOperation selfOperation
    ) {
        return actorContext != null && actorContext.isSelfTarget()
            ? selfOperation
            : ChatRoomOperation.MEMBERSHIP_MANAGE;
    }

    private Long targetMemberId(ChatRoomActorContext actorContext) {
        Long targetMemberId = actorContext.targetMemberId();
        if (targetMemberId == null || targetMemberId <= 0) {
            throw new ChatDomainException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }
        return targetMemberId;
    }
}
