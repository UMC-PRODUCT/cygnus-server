package com.umc.product.chat.adapter.out.persistence;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.umc.product.chat.application.port.out.LoadChatMessageReactionPort;
import com.umc.product.chat.application.port.out.SaveChatMessageReactionPort;
import com.umc.product.chat.application.port.out.dto.ChatReactionSummary;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChatMessageReactionPersistenceAdapter implements
    LoadChatMessageReactionPort,
    SaveChatMessageReactionPort {

    private final ChatMessageReactionJpaRepository repository;
    private final ChatMessageReactionQueryRepository queryRepository;

    @Override
    public List<ChatReactionSummary> summarizeByMessageIds(List<Long> messageIds, Long viewerMemberId) {
        return queryRepository.summarizeByMessageIds(messageIds, viewerMemberId);
    }

    @Override
    public Map<Long, List<ChatReactionSummary>> summarizeByMessageIdsForViewers(
        List<Long> messageIds,
        List<Long> viewerMemberIds
    ) {
        return queryRepository.summarizeByMessageIdsForViewers(messageIds, viewerMemberIds);
    }

    @Override
    public boolean addIfAbsent(Long messageId, Long memberId, String emoji) {
        return repository.insertIfAbsent(messageId, memberId, emoji) > 0;
    }

    @Override
    public boolean remove(Long messageId, Long memberId, String emoji) {
        return repository.deleteExact(messageId, memberId, emoji) > 0;
    }

    @Override
    public void deleteByMessageId(Long messageId) {
        repository.deleteAllByMessageId(messageId);
    }
}
