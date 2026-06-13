package com.umc.product.chat.adapter.out.persistence;

import com.umc.product.chat.application.port.out.LoadChatMessagePort;
import com.umc.product.chat.application.port.out.dto.RoomUnreadCount;
import com.umc.product.chat.application.port.out.SaveChatMessagePort;
import com.umc.product.chat.domain.ChatMessage;
import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatMessagePersistenceAdapter implements
    SaveChatMessagePort,
    LoadChatMessagePort {

    private final ChatMessageJpaRepository chatMessageJpaRepository;
    private final ChatMessageQueryRepository chatMessageQueryRepository;

    @Override
    public ChatMessage save(ChatMessage chatMessage) {
        return chatMessageJpaRepository.save(chatMessage);
    }

    @Override
    public ChatMessage getById(Long messageId) {
        return chatMessageJpaRepository.findById(messageId)
            .orElseThrow(() -> new ChatDomainException(ChatErrorCode.CHAT_MESSAGE_NOT_FOUND));
    }

    @Override
    public List<ChatMessage> listByRoomId(Long roomId, Long cursorId, int size) {
        return chatMessageQueryRepository.listByRoomId(roomId, cursorId, size);
    }

    @Override
    public List<ChatMessage> listLatestPerRoom(List<Long> roomIds) {
        return chatMessageQueryRepository.listLatestPerRoom(roomIds);
    }

    @Override
    public Optional<Long> findLatestMessageId(Long roomId) {
        return chatMessageQueryRepository.findLatestMessageId(roomId);
    }

    @Override
    public List<RoomUnreadCount> countUnreadByRooms(Long memberId, List<Long> roomIds) {
        return chatMessageQueryRepository.countUnreadByRooms(memberId, roomIds);
    }
}
