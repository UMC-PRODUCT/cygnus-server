package com.umc.product.chat.adapter.out.persistence;

import static com.umc.product.chat.domain.QChatMessageReaction.chatMessageReaction;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.product.chat.application.port.out.dto.ChatReactionSummary;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ChatMessageReactionQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<ChatReactionSummary> summarizeByMessageIds(List<Long> messageIds, Long viewerMemberId) {
        if (messageIds.isEmpty()) {
            return List.of();
        }

        NumberExpression<Long> reactionCount = chatMessageReaction.count();
        NumberExpression<Integer> viewerReactionCount = new CaseBuilder()
            .when(chatMessageReaction.memberId.eq(viewerMemberId))
            .then(1)
            .otherwise(0)
            .sum();

        return queryFactory
            .select(
                chatMessageReaction.messageId,
                chatMessageReaction.emoji,
                reactionCount,
                viewerReactionCount
            )
            .from(chatMessageReaction)
            .where(chatMessageReaction.messageId.in(messageIds))
            .groupBy(chatMessageReaction.messageId, chatMessageReaction.emoji)
            .orderBy(chatMessageReaction.messageId.asc(), chatMessageReaction.emoji.asc())
            .fetch()
            .stream()
            .map(row -> toSummary(row, reactionCount, viewerReactionCount))
            .toList();
    }

    public Map<Long, List<ChatReactionSummary>> summarizeByMessageIdsForViewers(
        List<Long> messageIds,
        List<Long> viewerMemberIds
    ) {
        if (messageIds.isEmpty() || viewerMemberIds.isEmpty()) {
            return Map.of();
        }

        List<ReactionCount> reactionCounts = loadReactionCounts(messageIds);
        Map<ReactionKey, Set<Long>> reactedViewers = loadReactedViewers(messageIds, viewerMemberIds);
        Map<Long, List<ChatReactionSummary>> result = new LinkedHashMap<>();
        for (Long viewerMemberId : viewerMemberIds) {
            List<ChatReactionSummary> summaries = reactionCounts.stream()
                .map(reaction -> new ChatReactionSummary(
                    reaction.messageId(),
                    reaction.emoji(),
                    reaction.count(),
                    reactedViewers.getOrDefault(reaction.key(), Set.of()).contains(viewerMemberId)
                ))
                .toList();
            result.put(viewerMemberId, summaries);
        }
        return Collections.unmodifiableMap(result);
    }

    private List<ReactionCount> loadReactionCounts(List<Long> messageIds) {
        NumberExpression<Long> reactionCount = chatMessageReaction.count();
        return queryFactory
            .select(chatMessageReaction.messageId, chatMessageReaction.emoji, reactionCount)
            .from(chatMessageReaction)
            .where(chatMessageReaction.messageId.in(messageIds))
            .groupBy(chatMessageReaction.messageId, chatMessageReaction.emoji)
            .orderBy(chatMessageReaction.messageId.asc(), chatMessageReaction.emoji.asc())
            .fetch()
            .stream()
            .map(row -> new ReactionCount(
                row.get(chatMessageReaction.messageId),
                row.get(chatMessageReaction.emoji),
                row.get(reactionCount) == null ? 0L : row.get(reactionCount)
            ))
            .toList();
    }

    private Map<ReactionKey, Set<Long>> loadReactedViewers(
        List<Long> messageIds,
        List<Long> viewerMemberIds
    ) {
        return queryFactory
            .select(
                chatMessageReaction.messageId,
                chatMessageReaction.emoji,
                chatMessageReaction.memberId
            )
            .from(chatMessageReaction)
            .where(
                chatMessageReaction.messageId.in(messageIds),
                chatMessageReaction.memberId.in(viewerMemberIds)
            )
            .orderBy(
                chatMessageReaction.messageId.asc(),
                chatMessageReaction.emoji.asc(),
                chatMessageReaction.memberId.asc()
            )
            .fetch()
            .stream()
            .collect(Collectors.groupingBy(
                row -> new ReactionKey(
                    row.get(chatMessageReaction.messageId),
                    row.get(chatMessageReaction.emoji)
                ),
                LinkedHashMap::new,
                Collectors.mapping(
                    row -> row.get(chatMessageReaction.memberId),
                    Collectors.toCollection(LinkedHashSet::new)
                )
            ));
    }

    private ChatReactionSummary toSummary(
        Tuple row,
        NumberExpression<Long> reactionCount,
        NumberExpression<Integer> viewerReactionCount
    ) {
        Long count = row.get(reactionCount);
        Integer reacted = row.get(viewerReactionCount);
        return new ChatReactionSummary(
            row.get(chatMessageReaction.messageId),
            row.get(chatMessageReaction.emoji),
            count == null ? 0L : count,
            reacted != null && reacted > 0
        );
    }

    private record ReactionCount(Long messageId, String emoji, long count) {

        ReactionKey key() {
            return new ReactionKey(messageId, emoji);
        }
    }

    private record ReactionKey(Long messageId, String emoji) {
    }
}
