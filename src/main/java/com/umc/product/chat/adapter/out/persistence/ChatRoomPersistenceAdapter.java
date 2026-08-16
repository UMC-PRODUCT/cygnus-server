package com.umc.product.chat.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.umc.product.chat.application.port.out.LoadChatMemberPort;
import com.umc.product.chat.application.port.out.LoadChatRoomPort;
import com.umc.product.chat.application.port.out.SaveChatMemberPort;
import com.umc.product.chat.application.port.out.SaveChatRoomPort;
import com.umc.product.chat.domain.ChatMember;
import com.umc.product.chat.domain.ChatRoom;
import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChatRoomPersistenceAdapter implements
    SaveChatRoomPort,
    LoadChatRoomPort,
    SaveChatMemberPort,
    LoadChatMemberPort {

    private final ChatRoomJpaRepository chatRoomJpaRepository;
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
        return findById(roomId)
            .orElseThrow(() -> new ChatDomainException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    @Override
    public Optional<ChatRoom> findById(Long roomId) {
        return chatRoomJpaRepository.findById(roomId);
    }

    @Override
    public ChatRoom getByIdForUpdate(Long roomId) {
        return chatRoomJpaRepository.findByIdForUpdate(roomId)
            .orElseThrow(() -> new ChatDomainException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    // ===== ChatMember ====================================

    @Override
    public ChatMember save(ChatMember chatMember) {
        return chatMemberJpaRepository.save(chatMember);
    }

    @Override
    public boolean saveIfAbsent(ChatMember chatMember) {
        return chatMemberJpaRepository.insertIfAbsent(chatMember.getRoomId(), chatMember.getMemberId()) > 0;
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
    public ChatMember getByRoomIdAndMemberId(Long roomId, Long memberId) {
        return chatMemberJpaRepository.findByRoomIdAndMemberId(roomId, memberId)
            .orElseThrow(() -> new ChatDomainException(ChatErrorCode.CHAT_MEMBER_NOT_FOUND));
    }

    @Override
    public List<ChatMember> listByRoomId(Long roomId) {
        return chatMemberJpaRepository.findAllByRoomId(roomId);
    }

    @Override
    public List<Long> listRoomIdsByMemberIdAndRoomIdIn(Long memberId, List<Long> roomIds) {
        return chatMemberJpaRepository.findAllByMemberIdAndRoomIdIn(memberId, roomIds).stream()
            .map(ChatMember::getRoomId)
            .toList();
    }

    @Override
    public void bumpLastReadMessageId(Long roomId, Long memberId, long candidateMessageId) {
        chatMemberJpaRepository.bumpLastReadMessageId(roomId, memberId, candidateMessageId);
    }
}
