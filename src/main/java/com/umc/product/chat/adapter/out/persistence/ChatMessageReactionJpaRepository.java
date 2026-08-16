package com.umc.product.chat.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.chat.domain.ChatMessageReaction;

public interface ChatMessageReactionJpaRepository extends JpaRepository<ChatMessageReaction, Long> {

    @Modifying
    @Query(value = """
        INSERT INTO chat_message_reaction (message_id, member_id, emoji, created_at, updated_at)
        VALUES (:messageId, :memberId, :emoji, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        ON CONFLICT (message_id, member_id, emoji) DO NOTHING
        """, nativeQuery = true)
    int insertIfAbsent(
        @Param("messageId") Long messageId,
        @Param("memberId") Long memberId,
        @Param("emoji") String emoji
    );

    @Modifying
    @Query("""
        DELETE FROM ChatMessageReaction reaction
        WHERE reaction.messageId = :messageId
          AND reaction.memberId = :memberId
          AND reaction.emoji = :emoji
        """)
    int deleteExact(
        @Param("messageId") Long messageId,
        @Param("memberId") Long memberId,
        @Param("emoji") String emoji
    );

    @Modifying
    @Query("DELETE FROM ChatMessageReaction reaction WHERE reaction.messageId = :messageId")
    void deleteAllByMessageId(@Param("messageId") Long messageId);
}
