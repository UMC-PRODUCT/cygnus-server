package com.umc.product.community.adapter.out.persistence;

import static com.umc.product.community.domain.QCommunityThread.communityThread;
import static com.umc.product.community.domain.QCommunityThreadMember.communityThreadMember;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.product.community.application.port.out.thread.dto.CommunityThreadListCondition;
import com.umc.product.community.application.port.out.thread.dto.CommunityThreadListRows;
import com.umc.product.community.application.port.out.thread.dto.CommunityThreadMemberRow;
import com.umc.product.community.application.port.out.thread.dto.CommunityThreadQueryRow;
import com.umc.product.community.domain.QCommunityThreadMember;
import com.umc.product.community.domain.enums.CommunityThreadMemberState;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class CommunityThreadQueryRepository {

    private final JPAQueryFactory queryFactory;

    public CommunityThreadListRows searchThreads(CommunityThreadListCondition condition) {
        List<CommunityThreadQueryRow> pinned = fetchThreadRows(condition, true);
        List<CommunityThreadQueryRow> unpinned = fetchThreadRows(condition, false);
        long unpinnedTotal = countUnpinned(condition);
        return new CommunityThreadListRows(pinned, unpinned, unpinnedTotal);
    }

    public Optional<CommunityThreadQueryRow> findThread(Long threadId, Long requesterMemberId) {
        QCommunityThreadMember requesterMembership = new QCommunityThreadMember("detailRequesterMembership");
        JPQLQuery<Long> memberCount = activeMemberCount("detailActiveMembership");

        Tuple row = queryFactory
            .select(threadProjection(requesterMembership, memberCount))
            .from(communityThread)
            .leftJoin(requesterMembership).on(
                requesterMembership.threadId.eq(communityThread.id),
                requesterMembership.memberId.eq(requesterMemberId)
            )
            .where(communityThread.id.eq(threadId))
            .fetchOne();

        return Optional.ofNullable(row)
            .map(tuple -> toThreadRow(tuple, requesterMembership, memberCount));
    }

    public List<CommunityThreadMemberRow> listActiveThreadMembers(Long threadId) {
        return queryFactory
            .select(
                communityThreadMember.memberId,
                communityThreadMember.role,
                communityThreadMember.state,
                communityThreadMember.joinedAt
            )
            .from(communityThreadMember)
            .where(
                communityThreadMember.threadId.eq(threadId),
                communityThreadMember.state.eq(CommunityThreadMemberState.ACTIVE)
            )
            .orderBy(communityThreadMember.memberId.asc())
            .fetch()
            .stream()
            .map(row -> new CommunityThreadMemberRow(
                row.get(communityThreadMember.memberId),
                row.get(communityThreadMember.role),
                row.get(communityThreadMember.state),
                row.get(communityThreadMember.joinedAt)
            ))
            .toList();
    }

    public List<Long> listActiveMemberIdsByThreadId(Long threadId, int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be positive");
        }
        return queryFactory
            .select(communityThreadMember.memberId)
            .from(communityThreadMember)
            .where(
                communityThreadMember.threadId.eq(threadId),
                communityThreadMember.state.eq(CommunityThreadMemberState.ACTIVE)
            )
            .orderBy(communityThreadMember.memberId.asc())
            .limit(limit)
            .fetch();
    }

    public List<Long> listInvitationBlockedMemberIds(Long threadId) {
        return queryFactory
            .select(communityThreadMember.memberId)
            .from(communityThreadMember)
            .where(
                communityThreadMember.threadId.eq(threadId),
                communityThreadMember.state.in(
                    CommunityThreadMemberState.ACTIVE,
                    CommunityThreadMemberState.KICKED
                )
            )
            .orderBy(communityThreadMember.memberId.asc())
            .fetch();
    }

    private List<CommunityThreadQueryRow> fetchThreadRows(
        CommunityThreadListCondition condition,
        boolean pinned
    ) {
        QCommunityThreadMember requesterMembership = new QCommunityThreadMember("listRequesterMembership");
        JPQLQuery<Long> memberCount = activeMemberCount("listActiveMembership");
        BooleanBuilder where = listCondition(condition, requesterMembership)
            .and(requesterMembership.pinned.eq(pinned));

        JPAQuery<Tuple> query = queryFactory
            .select(threadProjection(requesterMembership, memberCount))
            .from(communityThread)
            .join(requesterMembership).on(
                requesterMembership.threadId.eq(communityThread.id),
                requesterMembership.memberId.eq(condition.requesterMemberId()),
                requesterMembership.state.eq(CommunityThreadMemberState.ACTIVE)
            )
            .where(where)
            .orderBy(communityThread.lastActivityAt.desc(), communityThread.id.desc());

        if (!pinned) {
            query.offset(condition.offset()).limit(condition.limit());
        }
        return query.fetch().stream()
            .map(row -> toThreadRow(row, requesterMembership, memberCount))
            .toList();
    }

    private long countUnpinned(CommunityThreadListCondition condition) {
        QCommunityThreadMember requesterMembership = new QCommunityThreadMember("countRequesterMembership");
        Long count = queryFactory
            .select(communityThread.id.count())
            .from(communityThread)
            .join(requesterMembership).on(
                requesterMembership.threadId.eq(communityThread.id),
                requesterMembership.memberId.eq(condition.requesterMemberId()),
                requesterMembership.state.eq(CommunityThreadMemberState.ACTIVE)
            )
            .where(
                listCondition(condition, requesterMembership),
                requesterMembership.pinned.isFalse()
            )
            .fetchOne();
        return count == null ? 0L : count;
    }

    private BooleanBuilder listCondition(
        CommunityThreadListCondition condition,
        QCommunityThreadMember requesterMembership
    ) {
        BooleanBuilder where = new BooleanBuilder()
            .and(communityThread.deletedAt.isNull());
        if (condition.category() != null) {
            where.and(communityThread.category.eq(condition.category()));
        }
        if (condition.unreadOnly()) {
            where.and(requesterMembership.unreadCount.gt(0L));
        }
        if (condition.keyword() != null) {
            where.and(keywordContains(condition.keyword()));
        }
        return where;
    }

    private BooleanExpression keywordContains(String keyword) {
        return communityThread.title.containsIgnoreCase(keyword)
            .or(communityThread.description.containsIgnoreCase(keyword));
    }

    private JPQLQuery<Long> activeMemberCount(String alias) {
        QCommunityThreadMember activeMembership = new QCommunityThreadMember(alias);
        return JPAExpressions
            .select(activeMembership.id.count())
            .from(activeMembership)
            .where(
                activeMembership.threadId.eq(communityThread.id),
                activeMembership.state.eq(CommunityThreadMemberState.ACTIVE)
            );
    }

    private com.querydsl.core.types.Expression<?>[] threadProjection(
        QCommunityThreadMember requesterMembership,
        JPQLQuery<Long> memberCount
    ) {
        return new com.querydsl.core.types.Expression<?>[]{
            communityThread.id,
            communityThread.title,
            communityThread.description,
            communityThread.category,
            communityThread.icon,
            memberCount,
            requesterMembership.unreadCount,
            requesterMembership.pinned,
            requesterMembership.muted,
            requesterMembership.role,
            requesterMembership.state,
            communityThread.lastMessagePreview,
            communityThread.lastMessageSenderMemberId,
            communityThread.lastMessageCreatedAt,
            communityThread.creatorMemberId,
            communityThread.lastActivityAt,
            communityThread.deletedAt,
            communityThread.createdAt,
            communityThread.updatedAt
        };
    }

    private CommunityThreadQueryRow toThreadRow(
        Tuple row,
        QCommunityThreadMember requesterMembership,
        JPQLQuery<Long> memberCount
    ) {
        Long activeMemberCount = row.get(memberCount);
        Long unreadCount = row.get(requesterMembership.unreadCount);
        return new CommunityThreadQueryRow(
            row.get(communityThread.id),
            row.get(communityThread.title),
            row.get(communityThread.description),
            row.get(communityThread.category),
            row.get(communityThread.icon),
            activeMemberCount == null ? 0L : activeMemberCount,
            unreadCount == null ? 0L : unreadCount,
            Boolean.TRUE.equals(row.get(requesterMembership.pinned)),
            Boolean.TRUE.equals(row.get(requesterMembership.muted)),
            row.get(requesterMembership.role),
            row.get(requesterMembership.state),
            row.get(communityThread.lastMessagePreview),
            row.get(communityThread.lastMessageSenderMemberId),
            row.get(communityThread.lastMessageCreatedAt),
            row.get(communityThread.creatorMemberId),
            row.get(communityThread.lastActivityAt),
            row.get(communityThread.deletedAt),
            row.get(communityThread.createdAt),
            row.get(communityThread.updatedAt)
        );
    }
}
