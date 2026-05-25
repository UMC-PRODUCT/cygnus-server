package com.umc.product.chat.application.service.query;

import com.umc.product.chat.application.port.in.query.GetChatRoomUseCase;
import com.umc.product.chat.application.port.in.query.dto.ChatRoomInfo;
import com.umc.product.chat.application.port.out.LoadChatMemberPort;
import com.umc.product.chat.application.port.out.LoadChatRoomPort;
import com.umc.product.chat.domain.ChatMember;
import com.umc.product.chat.domain.ChatRoom;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomQueryService implements GetChatRoomUseCase {

    private final LoadChatRoomPort loadChatRoomPort;
    private final LoadChatMemberPort loadChatMemberPort;

    @Override
    public ChatRoomInfo getById(Long roomId) {
        ChatRoom chatRoom = loadChatRoomPort.getById(roomId);
        List<Long> memberIds = loadChatMemberPort.listByRoomId(roomId).stream()
            .map(ChatMember::getMemberId)
            .toList();
        return new ChatRoomInfo(chatRoom.getId(), chatRoom.getCreatedAt(), memberIds);
    }
}
