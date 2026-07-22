package com.umc.product.chat.adapter.out.persistence;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.chat.domain.ChatMessageMention;

public interface ChatMessageMentionJpaRepository extends JpaRepository<ChatMessageMention, Long> {

    List<ChatMessageMention> findAllByMessageIdInOrderByMessageIdAscMemberIdAsc(Collection<Long> messageIds);

    List<ChatMessageMention> findAllByMessageIdOrderByMemberIdAsc(Long messageId);

    @Modifying
    @Query("DELETE FROM ChatMessageMention mention WHERE mention.messageId = :messageId")
    void deleteAllByMessageId(@Param("messageId") Long messageId);
}
