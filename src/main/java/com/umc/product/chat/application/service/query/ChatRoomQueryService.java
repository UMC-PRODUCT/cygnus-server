package com.umc.product.chat.application.service.query;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.chat.application.port.in.query.CheckChatRoomAccessUseCase;
import com.umc.product.chat.application.port.in.query.GetChatRoomUseCase;
import com.umc.product.chat.application.port.in.query.dto.ChatMessageInfo;
import com.umc.product.chat.application.port.in.query.dto.ChatRoomInfo;
import com.umc.product.chat.application.port.out.LoadChatMemberPort;
import com.umc.product.chat.application.port.out.LoadChatMessagePort;
import com.umc.product.chat.application.port.out.LoadChatRoomPort;
import com.umc.product.chat.application.service.ChatRoomOwnershipAccessService;
import com.umc.product.chat.domain.ChatMember;
import com.umc.product.chat.domain.ChatRoom;
import com.umc.product.chat.domain.ChatRoomActorContext;
import com.umc.product.chat.domain.ChatRoomOperation;
import com.umc.product.chat.domain.ChatRoomOwnerReference;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomQueryService implements GetChatRoomUseCase, CheckChatRoomAccessUseCase {

    private final LoadChatRoomPort loadChatRoomPort;
    private final LoadChatMemberPort loadChatMemberPort;
    private final LoadChatMessagePort loadChatMessagePort;
    private final ChatRoomOwnershipAccessService ownershipAccessService;

    @Override
    public ChatRoomInfo getById(ChatRoomOwnerReference expectedOwner, ChatRoomActorContext actorContext) {
        ownershipAccessService.verify(expectedOwner, ChatRoomOperation.READ, actorContext);
        Long roomId = expectedOwner.roomId();
        ChatRoom chatRoom = loadChatRoomPort.getById(roomId);
        List<Long> memberIds = loadChatMemberPort.listByRoomId(roomId).stream()
            .map(ChatMember::getMemberId)
            .toList();
        return new ChatRoomInfo(chatRoom.getId(), chatRoom.getCreatedAt(), getPinnedMessage(chatRoom), memberIds);
    }

    @Override
    public boolean hasChatRoomAccess(ChatRoomOwnerReference expectedOwner, ChatRoomActorContext actorContext) {
        return ownershipAccessService.isAllowed(expectedOwner, ChatRoomOperation.READ, actorContext);
    }

    private ChatMessageInfo getPinnedMessage(ChatRoom chatRoom) {
        if (chatRoom.getPinnedMessageId() == null) {
            return null;
        }
        return ChatMessageInfo.from(
            loadChatMessagePort.getByIdAndRoomId(chatRoom.getPinnedMessageId(), chatRoom.getId())
        );
    }
}
