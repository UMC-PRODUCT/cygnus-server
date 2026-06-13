package com.umc.product.chat.adapter.out.persistence;

import static com.umc.product.chat.domain.QChatMember.chatMember;
import static com.umc.product.chat.domain.QChatMessage.chatMessage;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.product.chat.application.port.out.dto.RoomUnreadCount;
import com.umc.product.chat.domain.ChatMessage;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ChatMessageQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 방 단위 메시지 내역을 최신순으로 커서 페이지네이션 조회한다.
     */
    public List<ChatMessage> listByRoomId(Long roomId, Long cursorId, int size) {
        return queryFactory.selectFrom(chatMessage)
            .where(
                chatMessage.roomId.eq(roomId),
                cursorLt(cursorId)
            )
            .orderBy(chatMessage.id.desc())
            .limit(size)
            .fetch();
    }

    /**
     * 각 방의 마지막 메시지를 조회한다.
     * <p>
     * 1) 방별 최대 메시지 id를 집계한 뒤, 2) 해당 id들의 메시지를 조회한다. 방 개수와 무관하게 쿼리 2회.
     */
    public List<ChatMessage> listLatestPerRoom(List<Long> roomIds) {
        if (roomIds == null || roomIds.isEmpty()) {
            return List.of();
        }

        List<Long> latestMessageIds = queryFactory
            .select(chatMessage.id.max())
            .from(chatMessage)
            .where(chatMessage.roomId.in(roomIds))
            .groupBy(chatMessage.roomId)
            .fetch();

        if (latestMessageIds.isEmpty()) {
            return List.of();
        }

        return queryFactory.selectFrom(chatMessage)
            .where(chatMessage.id.in(latestMessageIds))
            .fetch();
    }

    /**
     * 방의 가장 최신 메시지 id. 메시지가 없으면 {@code Optional.empty()}.
     */
    public Optional<Long> findLatestMessageId(Long roomId) {
        return Optional.ofNullable(
            queryFactory.select(chatMessage.id.max())
                .from(chatMessage)
                .where(chatMessage.roomId.eq(roomId))
                .fetchOne());
    }

    /**
     * 멤버 기준 방별 안 읽은 메시지 수.
     * <p>
     * 각 방의 {@code chat_member.last_read_message_id}를 임계값으로, 그보다 큰 id이면서
     * 본인이 보내지 않은 메시지(시스템 메시지 포함)를 카운트한다.
     */
    public List<RoomUnreadCount> countUnreadByRooms(Long memberId, List<Long> roomIds) {
        if (roomIds == null || roomIds.isEmpty()) {
            return List.of();
        }

        return queryFactory
            .select(Projections.constructor(RoomUnreadCount.class,
                chatMessage.roomId,
                chatMessage.count()))
            .from(chatMessage)
            .join(chatMember).on(
                chatMember.roomId.eq(chatMessage.roomId)
                    .and(chatMember.memberId.eq(memberId)))
            .where(
                chatMessage.roomId.in(roomIds),
                chatMessage.id.gt(chatMember.lastReadMessageId.coalesce(0L)),
                notSentByMe(memberId)
            )
            .groupBy(chatMessage.roomId)
            .fetch();
    }

    private BooleanExpression cursorLt(Long cursorId) {
        return cursorId != null ? chatMessage.id.lt(cursorId) : null;
    }

    // 시스템 메시지(senderMemberId IS NULL)도 안 읽은 메시지로 포함한다.
    private BooleanExpression notSentByMe(Long memberId) {
        return chatMessage.senderMemberId.isNull()
            .or(chatMessage.senderMemberId.ne(memberId));
    }
}
