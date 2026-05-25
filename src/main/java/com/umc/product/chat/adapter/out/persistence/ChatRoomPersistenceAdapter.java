package com.umc.product.chat.adapter.out.persistence;

import com.umc.product.chat.application.port.out.LoadChatMemberPort;
import com.umc.product.chat.application.port.out.LoadChatRoomPort;
import com.umc.product.chat.application.port.out.SaveChatMemberPort;
import com.umc.product.chat.application.port.out.SaveChatRoomPort;
import com.umc.product.chat.domain.ChatMember;
import com.umc.product.chat.domain.ChatRoom;
import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatRoomPersistenceAdapter implements
    SaveChatRoomPort,
    LoadChatRoomPort,
    SaveChatMemberPort,
    LoadChatMemberPort {

    private final ChatRoomJpaRepository chatRoomJpaRepository;
    private final ChatRoomQueryRepository chatRoomQueryRepository;
    private final ChatMemberJpaRepository chatMemberJpaRepository;

    // ========== ChatRoom ====================

    @Override
    public ChatRoom save(ChatRoom chatRoom) {
        return chatRoomJpaRepository.save(chatRoom);
    }

    @Override
    public void delete(ChatRoom chatRoom) {
        chatRoomJpaRepository.delete(chatRoom);
    }

    @Override
    public ChatRoom getById(Long roomId) {
        return chatRoomJpaRepository.findById(roomId)
            .orElseThrow(() -> new ChatDomainException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    @Override
    public List<ChatRoom> listByMemberId(Long memberId) {
        return chatRoomQueryRepository.findAllByMemberId(memberId);
    }

    // ===== ChatMember ====================================

    @Override
    public ChatMember save(ChatMember chatMember) {
        return chatMemberJpaRepository.save(chatMember);
    }

    @Override
    public void delete(Long roomId, Long memberId) {
        chatMemberJpaRepository.deleteByRoomIdAndMemberId(roomId, memberId);
    }

    @Override
    public boolean existsByRoomIdAndMemberId(Long roomId, Long memberId) {
        return chatMemberJpaRepository.existsByRoomIdAndMemberId(roomId, memberId);
    }

    @Override
    public List<ChatMember> listByRoomId(Long roomId) {
        return chatMemberJpaRepository.findAllByRoomId(roomId);
    }
}
