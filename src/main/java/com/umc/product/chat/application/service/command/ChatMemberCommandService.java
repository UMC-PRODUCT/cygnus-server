package com.umc.product.chat.application.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.chat.application.port.in.command.JoinChatRoomUseCase;
import com.umc.product.chat.application.port.in.command.LeaveChatRoomUseCase;
import com.umc.product.chat.application.port.in.command.dto.JoinChatRoomCommand;
import com.umc.product.chat.application.port.in.command.dto.LeaveChatRoomCommand;
import com.umc.product.chat.application.port.out.LoadChatMemberPort;
import com.umc.product.chat.application.port.out.LoadChatMessagePort;
import com.umc.product.chat.application.port.out.LoadChatRoomPort;
import com.umc.product.chat.application.port.out.SaveChatMemberPort;
import com.umc.product.chat.domain.ChatMember;
import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatMemberCommandService implements JoinChatRoomUseCase, LeaveChatRoomUseCase {

    private final LoadChatRoomPort loadChatRoomPort;
    private final LoadChatMemberPort loadChatMemberPort;
    private final LoadChatMessagePort loadChatMessagePort;
    private final SaveChatMemberPort saveChatMemberPort;

    @Override
    public void joinChatRoom(JoinChatRoomCommand command) {
        loadChatRoomPort.getByIdForUpdate(command.roomId());
        if (loadChatMemberPort.existsByRoomIdAndMemberId(command.roomId(), command.memberId())) {
            throw new ChatDomainException(ChatErrorCode.CHAT_MEMBER_ALREADY_EXISTS);
        }
        if (command.initialLastReadMessageId() != null) {
            loadChatMessagePort.getByIdAndRoomId(
                command.initialLastReadMessageId(),
                command.roomId()
            );
        }
        ChatMember chatMember = ChatMember.of(
            command.roomId(),
            command.memberId(),
            command.initialLastReadMessageId()
        );
        if (!saveChatMemberPort.saveIfAbsent(chatMember)) {
            throw new ChatDomainException(ChatErrorCode.CHAT_MEMBER_ALREADY_EXISTS);
        }
        if (command.initialLastReadMessageId() != null) {
            saveChatMemberPort.bumpLastReadMessageId(
                command.roomId(),
                command.memberId(),
                command.initialLastReadMessageId()
            );
        }
    }

    @Override
    public void leaveChatRoom(LeaveChatRoomCommand command) {
        loadChatRoomPort.getByIdForUpdate(command.roomId());
        if (!loadChatMemberPort.existsByRoomIdAndMemberId(command.roomId(), command.memberId())) {
            throw new ChatDomainException(ChatErrorCode.CHAT_MEMBER_NOT_FOUND);
        }
        saveChatMemberPort.delete(command.roomId(), command.memberId());
    }
}
