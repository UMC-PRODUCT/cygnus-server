package com.umc.product.chat.application.service.query;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.chat.application.port.in.query.CheckChatRoomAccessUseCase;
import com.umc.product.chat.application.port.in.query.GetChatRoomUseCase;
import com.umc.product.chat.application.port.in.query.dto.ChatRoomInfo;
import com.umc.product.chat.application.port.out.LoadChatMemberPort;
import com.umc.product.chat.application.port.out.LoadChatRoomPort;
import com.umc.product.chat.domain.ChatMember;
import com.umc.product.chat.domain.ChatRoom;
import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomQueryService implements GetChatRoomUseCase, CheckChatRoomAccessUseCase {

    private final LoadChatRoomPort loadChatRoomPort;
    private final LoadChatMemberPort loadChatMemberPort;

    @Override
    public ChatRoomInfo getById(Long roomId, Long memberId) {
        if (!loadChatMemberPort.existsByRoomIdAndMemberId(roomId, memberId)) {
            throw new ChatDomainException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }

        ChatRoom chatRoom = loadChatRoomPort.getById(roomId);
        List<Long> memberIds = loadChatMemberPort.listByRoomId(roomId).stream()
            .map(ChatMember::getMemberId)
            .toList();
        return new ChatRoomInfo(chatRoom.getId(), chatRoom.getCreatedAt(), memberIds);
    }

    @Override
    public boolean hasChatRoomAccess(Long memberId, Long chatRoomId) {
        return loadChatMemberPort.existsByRoomIdAndMemberId(chatRoomId, memberId);
    }
}
