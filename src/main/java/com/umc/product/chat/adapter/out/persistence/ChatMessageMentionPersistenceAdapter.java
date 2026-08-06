package com.umc.product.chat.adapter.out.persistence;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.umc.product.chat.application.port.out.LoadChatMessageMentionPort;
import com.umc.product.chat.application.port.out.SaveChatMessageMentionPort;
import com.umc.product.chat.domain.ChatMessageMention;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChatMessageMentionPersistenceAdapter implements
    LoadChatMessageMentionPort,
    SaveChatMessageMentionPort {

    private final ChatMessageMentionJpaRepository repository;

    @Override
    public List<Long> listMemberIdsByMessageId(Long messageId) {
        return repository.findAllByMessageIdOrderByMemberIdAsc(messageId).stream()
            .map(ChatMessageMention::getMemberId)
            .toList();
    }

    @Override
    public Map<Long, List<Long>> listMemberIdsByMessageIds(List<Long> messageIds) {
        if (messageIds.isEmpty()) {
            return Map.of();
        }
        return repository.findAllByMessageIdInOrderByMessageIdAscMemberIdAsc(messageIds).stream()
            .collect(Collectors.groupingBy(
                ChatMessageMention::getMessageId,
                LinkedHashMap::new,
                Collectors.mapping(ChatMessageMention::getMemberId, Collectors.toList())
            ));
    }

    @Override
    public void saveAll(Long messageId, List<Long> memberIds) {
        repository.saveAll(memberIds.stream()
            .map(memberId -> ChatMessageMention.of(messageId, memberId))
            .toList());
    }

    @Override
    public void deleteByMessageId(Long messageId) {
        repository.deleteAllByMessageId(messageId);
    }
}
