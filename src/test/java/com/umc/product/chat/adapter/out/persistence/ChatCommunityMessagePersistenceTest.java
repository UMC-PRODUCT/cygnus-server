package com.umc.product.chat.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import com.umc.product.chat.application.port.out.dto.ChatReactionSummary;
import com.umc.product.chat.domain.ChatMessage;
import com.umc.product.chat.domain.ChatMessageMention;
import com.umc.product.chat.domain.ChatMessageReaction;
import com.umc.product.chat.domain.ChatRoom;
import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.global.config.JpaConfig;
import com.umc.product.global.config.QueryDslConfig;
import com.umc.product.support.TestContainersConfig;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    JpaConfig.class,
    QueryDslConfig.class,
    TestContainersConfig.class,
    ChatMessageReactionQueryRepository.class,
    ChatMessageMentionPersistenceAdapter.class,
    ChatMessageReactionPersistenceAdapter.class
})
@DisplayName("Community Chat message persistence")
class ChatCommunityMessagePersistenceTest {

    @Autowired
    TestEntityManager em;
    @Autowired
    ChatMessageJpaRepository chatMessageRepository;
    @Autowired
    ChatMessageMentionJpaRepository mentionRepository;
    @Autowired
    ChatMessageReactionJpaRepository reactionRepository;
    @Autowired
    ChatMessageReactionQueryRepository reactionQueryRepository;
    @Autowired
    ChatMessageMentionPersistenceAdapter mentionAdapter;
    @Autowired
    ChatMessageReactionPersistenceAdapter reactionAdapter;

    private Long roomId;

    @BeforeEach
    void setUp() {
        roomId = em.persist(ChatRoom.create()).getId();
    }

    @Test
    @DisplayName("room, sender, clientMessageId 조합은 유일하다")
    void clientMessageId_uniquePerRoomAndSender() {
        UUID clientMessageId = UUID.fromString("5bd63e4a-38b8-4628-a0dd-e86c45f240a6");
        chatMessageRepository.saveAndFlush(message(10L, clientMessageId, "첫 메시지"));

        assertThatThrownBy(() -> {
            chatMessageRepository.saveAndFlush(message(10L, clientMessageId, "재사용"));
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("clientMessageId가 null인 legacy 메시지는 같은 방과 발신자에 여러 건 저장된다")
    void clientMessageId_nullableForLegacy() {
        em.persist(message(10L, null, "legacy-1"));
        em.persist(message(10L, null, "legacy-2"));
        flushAndClear();

        assertThat(chatMessageRepository.count()).isEqualTo(2L);
    }

    @Test
    @DisplayName("한 메시지의 동일 멤버 mention은 유일하다")
    void mention_unique() {
        Long messageId = em.persist(message(10L, UUID.randomUUID(), "본문")).getId();
        mentionRepository.saveAndFlush(ChatMessageMention.of(messageId, 20L));

        assertThatThrownBy(() -> {
            mentionRepository.saveAndFlush(ChatMessageMention.of(messageId, 20L));
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("한 메시지의 멤버별 동일 emoji reaction은 유일하다")
    void reaction_unique() {
        Long messageId = em.persist(message(10L, UUID.randomUUID(), "본문")).getId();
        reactionRepository.saveAndFlush(ChatMessageReaction.of(messageId, 20L, "👍"));

        assertThatThrownBy(() -> {
            reactionRepository.saveAndFlush(ChatMessageReaction.of(messageId, 20L, "👍"));
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("reaction projection은 emoji별 count와 viewer 반응 여부를 집계한다")
    void reaction_summaryProjection() {
        Long messageId = em.persist(message(10L, UUID.randomUUID(), "본문")).getId();
        em.persist(ChatMessageReaction.of(messageId, 10L, "👍"));
        em.persist(ChatMessageReaction.of(messageId, 20L, "👍"));
        em.persist(ChatMessageReaction.of(messageId, 20L, "🎉"));
        flushAndClear();

        List<ChatReactionSummary> result = reactionQueryRepository
            .summarizeByMessageIds(List.of(messageId), 10L);

        assertThat(result).containsExactly(
            new ChatReactionSummary(messageId, "🎉", 1L, false),
            new ChatReactionSummary(messageId, "👍", 2L, true)
        );
    }

    @Test
    @DisplayName("reaction audience projection은 count를 공유하고 viewer별 반응 여부를 batch로 구분한다")
    void reaction_audienceSummaryProjection() {
        Long messageId = em.persist(message(10L, UUID.randomUUID(), "본문")).getId();
        em.persist(ChatMessageReaction.of(messageId, 10L, "👍"));
        em.persist(ChatMessageReaction.of(messageId, 20L, "👍"));
        em.persist(ChatMessageReaction.of(messageId, 20L, "🎉"));
        flushAndClear();

        Map<Long, List<ChatReactionSummary>> result = reactionQueryRepository
            .summarizeByMessageIdsForViewers(List.of(messageId), List.of(10L, 30L));

        assertThat(result.get(10L)).containsExactly(
            new ChatReactionSummary(messageId, "🎉", 1L, false),
            new ChatReactionSummary(messageId, "👍", 2L, true)
        );
        assertThat(result.get(30L)).containsExactly(
            new ChatReactionSummary(messageId, "🎉", 1L, false),
            new ChatReactionSummary(messageId, "👍", 2L, false)
        );
    }

    @Test
    @DisplayName("동일 reaction 재시도는 insert 한 건과 duplicate no-op으로 귀결된다")
    void reaction_insertIfAbsent() {
        Long messageId = em.persist(message(10L, UUID.randomUUID(), "본문")).getId();
        em.flush();

        boolean first = reactionAdapter.addIfAbsent(messageId, 20L, "👍");
        boolean duplicate = reactionAdapter.addIfAbsent(messageId, 20L, "👍");

        assertThat(first).isTrue();
        assertThat(duplicate).isFalse();
        assertThat(reactionRepository.count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("mention adapter는 정렬된 member id projection을 제공하고 일괄 삭제한다")
    void mention_projectionAndDelete() {
        Long messageId = em.persist(message(10L, UUID.randomUUID(), "본문")).getId();
        em.flush();
        mentionAdapter.saveAll(messageId, List.of(30L, 20L));
        em.flush();

        assertThat(mentionAdapter.listMemberIdsByMessageId(messageId)).containsExactly(20L, 30L);

        mentionAdapter.deleteByMessageId(messageId);
        assertThat(mentionRepository.count()).isZero();
    }

    @Test
    @DisplayName("tombstone은 message row를 보존하고 file, mention, reaction만 비운다")
    void tombstone_preservesMessageRow() {
        ChatMessage message = ChatMessage.create(
            roomId,
            10L,
            MessageContentType.IMAGE,
            "캡션",
            List.of("file-1"),
            null,
            UUID.randomUUID(),
            "0".repeat(64)
        );
        Long messageId = em.persist(message).getId();
        em.persist(ChatMessageMention.of(messageId, 20L));
        em.persist(ChatMessageReaction.of(messageId, 20L, "👍"));
        em.flush();

        message.tombstone();
        mentionRepository.deleteAllByMessageId(messageId);
        reactionRepository.deleteAllByMessageId(messageId);
        flushAndClear();

        ChatMessage persisted = chatMessageRepository.findById(messageId).orElseThrow();
        assertThat(persisted.getContentType()).isEqualTo(MessageContentType.SYSTEM);
        assertThat(persisted.getContent()).isEqualTo(ChatMessage.DELETED_CONTENT);
        assertThat(persisted.getFileMetadataIds()).isEmpty();
        assertThat(persisted.getDeletedAt()).isNotNull();
        assertThat(mentionRepository.count()).isZero();
        assertThat(reactionRepository.count()).isZero();
    }

    private ChatMessage message(Long senderMemberId, UUID clientMessageId, String content) {
        return ChatMessage.create(
            roomId,
            senderMemberId,
            MessageContentType.TEXT,
            content,
            List.of(),
            null,
            clientMessageId,
            clientMessageId == null ? null : "0".repeat(64)
        );
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }
}
