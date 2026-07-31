package com.umc.product.community.application.service.query;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerBasicInfo;
import com.umc.product.community.application.port.in.query.thread.GetCommunityThreadDetailUseCase;
import com.umc.product.community.application.port.in.query.thread.GetCommunityThreadMembersByIdsUseCase;
import com.umc.product.community.application.port.in.query.thread.GetCommunityThreadMutationDetailUseCase;
import com.umc.product.community.application.port.in.query.thread.ListCommunityThreadMembersUseCase;
import com.umc.product.community.application.port.in.query.thread.ListCommunityThreadsUseCase;
import com.umc.product.community.application.port.in.query.thread.SearchCommunityThreadInvitableUseCase;
import com.umc.product.community.application.port.in.query.thread.dto.GetThreadDetailQuery;
import com.umc.product.community.application.port.in.query.thread.dto.GetThreadMembersByIdsQuery;
import com.umc.product.community.application.port.in.query.thread.dto.ListThreadMembersQuery;
import com.umc.product.community.application.port.in.query.thread.dto.ListThreadsQuery;
import com.umc.product.community.application.port.in.query.thread.dto.SearchThreadInvitableQuery;
import com.umc.product.community.application.port.in.query.thread.dto.ThreadDetailInfo;
import com.umc.product.community.application.port.in.query.thread.dto.ThreadInvitableInfo;
import com.umc.product.community.application.port.in.query.thread.dto.ThreadInvitablePageInfo;
import com.umc.product.community.application.port.in.query.thread.dto.ThreadLastMessageInfo;
import com.umc.product.community.application.port.in.query.thread.dto.ThreadListFilter;
import com.umc.product.community.application.port.in.query.thread.dto.ThreadListInfo;
import com.umc.product.community.application.port.in.query.thread.dto.ThreadMemberInfo;
import com.umc.product.community.application.port.in.query.thread.dto.ThreadMemberPageInfo;
import com.umc.product.community.application.port.in.query.thread.dto.ThreadSummaryInfo;
import com.umc.product.community.application.port.out.thread.CommunityThreadQueryPort;
import com.umc.product.community.application.port.out.thread.dto.CommunityThreadListCondition;
import com.umc.product.community.application.port.out.thread.dto.CommunityThreadListRows;
import com.umc.product.community.application.port.out.thread.dto.CommunityThreadMemberRow;
import com.umc.product.community.application.port.out.thread.dto.CommunityThreadQueryRow;
import com.umc.product.community.domain.CommunityThreadProperties;
import com.umc.product.community.domain.enums.CommunityThreadCategory;
import com.umc.product.community.domain.enums.CommunityThreadMemberRole;
import com.umc.product.community.domain.enums.CommunityThreadMemberState;
import com.umc.product.community.domain.exception.CommunityDomainException;
import com.umc.product.community.domain.exception.CommunityErrorCode;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.SearchMemberInvitationUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.member.application.port.in.query.dto.MemberInvitationSearchResult;
import com.umc.product.member.application.port.in.query.dto.SearchMemberInvitationQuery;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityThreadQueryService implements
    ListCommunityThreadsUseCase,
    GetCommunityThreadDetailUseCase,
    GetCommunityThreadMembersByIdsUseCase,
    GetCommunityThreadMutationDetailUseCase,
    ListCommunityThreadMembersUseCase,
    SearchCommunityThreadInvitableUseCase {

    private static final String UNKNOWN_MEMBER_NAME = "알 수 없음";
    private static final String SHARE_PATH_PREFIX = "/api/v1/community/threads/";

    private final CommunityThreadQueryPort threadQueryPort;
    private final GetMemberUseCase getMemberUseCase;
    private final GetChallengerUseCase getChallengerUseCase;
    private final GetGisuUseCase getGisuUseCase;
    private final SearchMemberInvitationUseCase searchInvitationUseCase;
    private final CommunityThreadProperties threadProperties;

    @Override
    public ThreadListInfo listThreads(ListThreadsQuery query) {
        CommunityThreadListRows rows = threadQueryPort.searchThreads(new CommunityThreadListCondition(
            query.requesterMemberId(),
            categoryOf(query.filter()),
            query.filter() == ThreadListFilter.UNREAD,
            query.q(),
            query.offset(),
            query.limit()
        ));
        Map<Long, MemberInfo> senders = loadVisibleSenders(rows.pinned(), rows.unpinned());
        List<ThreadSummaryInfo> pinned = rows.pinned().stream()
            .map(row -> toSummary(row, senders))
            .toList();
        List<ThreadSummaryInfo> threads = rows.unpinned().stream()
            .map(row -> toSummary(row, senders))
            .toList();
        int consumed = Math.addExact(query.offset(), threads.size());
        Integer nextOffset = consumed < rows.unpinnedTotal() ? consumed : null;
        return new ThreadListInfo(pinned, threads, nextOffset, rows.unpinnedTotal());
    }

    @Override
    public ThreadDetailInfo getThread(GetThreadDetailQuery query) {
        CommunityThreadQueryRow row = getReadableThread(query.threadId(), query.requesterMemberId());
        Map<Long, MemberInfo> senders = loadVisibleSenders(List.of(row), List.of());
        return ThreadDetailInfo.from(
            toSummary(row, senders),
            SHARE_PATH_PREFIX + row.threadId(),
            row.deletedAt()
        );
    }

    @Override
    public ThreadDetailInfo getMutationDetail(GetThreadDetailQuery query) {
        CommunityThreadQueryRow row = getMutationReadableThread(
            query.threadId(),
            query.requesterMemberId()
        );
        Map<Long, MemberInfo> senders = loadVisibleSenders(List.of(row), List.of());
        return ThreadDetailInfo.from(
            toSummary(row, senders),
            SHARE_PATH_PREFIX + row.threadId(),
            row.deletedAt()
        );
    }

    @Override
    public List<ThreadMemberInfo> getMembersByIds(GetThreadMembersByIdsQuery query) {
        CommunityThreadQueryRow thread = getReadableThread(query.threadId(), query.requesterMemberId());
        if (query.memberIds().size() > threadProperties.maxMembers()) {
            throw new CommunityDomainException(CommunityErrorCode.THREAD_CAPACITY_EXCEEDED);
        }

        Map<Long, CommunityThreadMemberRow> rowsByMemberId = threadQueryPort
            .listActiveThreadMembers(query.threadId())
            .stream()
            .filter(row -> row.state() == CommunityThreadMemberState.ACTIVE)
            .collect(Collectors.toMap(
                CommunityThreadMemberRow::memberId,
                row -> row,
                (first, ignored) -> first
            ));
        if (!rowsByMemberId.keySet().containsAll(query.memberIds())) {
            throw new CommunityDomainException(CommunityErrorCode.THREAD_MEMBER_NOT_FOUND);
        }

        List<CommunityThreadMemberRow> orderedRows = query.memberIds().stream()
            .map(rowsByMemberId::get)
            .toList();
        return assembleMemberInfos(orderedRows);
    }

    @Override
    public ThreadMemberPageInfo listMembers(ListThreadMembersQuery query) {
        CommunityThreadQueryRow thread = getReadableThread(query.threadId(), query.requesterMemberId());
        List<CommunityThreadMemberRow> rows = threadQueryPort.listActiveThreadMembers(query.threadId());
        if (rows.isEmpty()) {
            return new ThreadMemberPageInfo(List.of(), null, 0L);
        }

        List<ThreadMemberInfo> memberInfos = assembleMemberInfos(rows);
        String keyword = query.q() == null ? null : query.q().toLowerCase(Locale.ROOT);
        List<ThreadMemberInfo> filtered = memberInfos.stream()
            .filter(info -> keyword == null || info.name().toLowerCase(Locale.ROOT).contains(keyword))
            .filter(info -> query.role() == null || info.role() == query.role())
            .filter(info -> query.part() == null || info.part() == query.part())
            .filter(info -> query.generation() == null || query.generation().equals(info.generation()))
            .sorted(memberOrder())
            .toList();

        int from = Math.min(query.offset(), filtered.size());
        int to = Math.min(Math.addExact(from, query.limit()), filtered.size());
        Integer nextOffset = to < filtered.size() ? to : null;
        return new ThreadMemberPageInfo(filtered.subList(from, to), nextOffset, filtered.size());
    }

    @Override
    public ThreadInvitablePageInfo searchInvitable(SearchThreadInvitableQuery query) {
        CommunityThreadQueryRow thread = getReadableThread(query.threadId(), query.requesterMemberId());
        if (thread.requesterRole() != CommunityThreadMemberRole.OWNER
            && thread.requesterRole() != CommunityThreadMemberRole.ADMIN) {
            throw new CommunityDomainException(CommunityErrorCode.THREAD_ACCESS_DENIED);
        }

        Set<Long> blockedMemberIds = Set.copyOf(
            threadQueryPort.listInvitationBlockedMemberIds(query.threadId())
        );
        MemberInvitationSearchResult result = searchInvitationUseCase.search(
            new SearchMemberInvitationQuery(
                query.q(),
                blockedMemberIds,
                query.offset(),
                query.limit()
            )
        );
        List<ThreadInvitableInfo> items = result.items().stream()
            .map(item -> new ThreadInvitableInfo(
                item.memberId(),
                item.challengerId(),
                item.name(),
                item.part(),
                item.generation()
            ))
            .toList();
        return new ThreadInvitablePageInfo(items, result.nextOffset(), result.total());
    }

    private CommunityThreadQueryRow getReadableThread(Long threadId, Long requesterMemberId) {
        CommunityThreadQueryRow row = threadQueryPort.findThread(threadId, requesterMemberId)
            .orElseThrow(() -> new CommunityDomainException(CommunityErrorCode.THREAD_NOT_FOUND));
        if (row.deletedAt() != null) {
            throw new CommunityDomainException(CommunityErrorCode.THREAD_DELETED);
        }
        if (row.requesterState() != CommunityThreadMemberState.ACTIVE) {
            throw new CommunityDomainException(CommunityErrorCode.THREAD_ACCESS_DENIED);
        }
        return row;
    }

    private CommunityThreadQueryRow getMutationReadableThread(Long threadId, Long requesterMemberId) {
        CommunityThreadQueryRow row = threadQueryPort.findThread(threadId, requesterMemberId)
            .orElseThrow(() -> new CommunityDomainException(CommunityErrorCode.THREAD_NOT_FOUND));
        if (row.requesterState() != CommunityThreadMemberState.ACTIVE) {
            throw new CommunityDomainException(CommunityErrorCode.THREAD_ACCESS_DENIED);
        }
        return row;
    }

    private Map<Long, MemberInfo> loadVisibleSenders(
        List<CommunityThreadQueryRow> pinned,
        List<CommunityThreadQueryRow> unpinned
    ) {
        Set<Long> senderIds = Stream.concat(pinned.stream(), unpinned.stream())
            .map(CommunityThreadQueryRow::lastMessageSenderMemberId)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toSet());
        return senderIds.isEmpty() ? Map.of() : getMemberUseCase.findAllByIds(senderIds);
    }

    private List<ThreadMemberInfo> assembleMemberInfos(List<CommunityThreadMemberRow> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Set<Long> memberIds = rows.stream()
            .map(CommunityThreadMemberRow::memberId)
            .collect(Collectors.toSet());
        Map<Long, MemberInfo> members = getMemberUseCase.findAllByIds(memberIds);
        Map<Long, LatestChallenger> latestChallengers = loadLatestChallengers(memberIds);
        return rows.stream()
            .map(row -> {
                MemberInfo member = members.get(row.memberId());
                if (member == null) {
                    throw new CommunityDomainException(CommunityErrorCode.THREAD_MEMBER_NOT_FOUND);
                }
                LatestChallenger latest = latestChallengers.get(row.memberId());
                return new ThreadMemberInfo(
                    row.memberId(),
                    member.name(),
                    latest == null ? null : latest.challenger().part(),
                    latest == null ? null : latest.generation(),
                    row.role(), row.joinedAt(), row.state()
                );
            })
            .toList();
    }

    private Map<Long, LatestChallenger> loadLatestChallengers(Set<Long> memberIds) {
        Map<Long, List<ChallengerBasicInfo>> historiesByMemberId = getChallengerUseCase
            .getAllBasicByMemberIds(memberIds);
        List<ChallengerBasicInfo> allChallengers = historiesByMemberId.values().stream()
            .flatMap(List::stream)
            .toList();
        if (allChallengers.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> generationByGisuId = loadGenerationByGisuId(allChallengers);
        return historiesByMemberId.entrySet().stream()
            .map(entry -> latestChallenger(entry.getValue(), generationByGisuId))
            .flatMap(Optional::stream)
            .collect(Collectors.toUnmodifiableMap(
                latest -> latest.challenger().memberId(),
                Function.identity()
            ));
    }

    private Optional<LatestChallenger> latestChallenger(
        List<ChallengerBasicInfo> challengers,
        Map<Long, Long> generationByGisuId
    ) {
        return challengers.stream()
            .filter(challenger -> generationByGisuId.containsKey(challenger.gisuId()))
            .max(Comparator.comparing(
                    (ChallengerBasicInfo challenger) -> generationByGisuId.get(challenger.gisuId())
                )
                .thenComparing(ChallengerBasicInfo::gisuId)
                .thenComparing(ChallengerBasicInfo::challengerId))
            .map(challenger -> new LatestChallenger(
                challenger,
                generationByGisuId.get(challenger.gisuId())
            ));
    }

    private Map<Long, Long> loadGenerationByGisuId(List<ChallengerBasicInfo> challengers) {
        Set<Long> gisuIds = challengers.stream()
            .map(ChallengerBasicInfo::gisuId)
            .collect(Collectors.toSet());
        return getGisuUseCase.getByIds(gisuIds).stream()
            .collect(Collectors.toUnmodifiableMap(GisuInfo::gisuId, GisuInfo::generation));
    }

    private ThreadSummaryInfo toSummary(CommunityThreadQueryRow row, Map<Long, MemberInfo> senders) {
        ThreadLastMessageInfo lastMessage = null;
        if (row.lastMessageSenderMemberId() != null) {
            MemberInfo sender = senders.get(row.lastMessageSenderMemberId());
            String senderName = sender == null ? UNKNOWN_MEMBER_NAME : sender.name();
            lastMessage = new ThreadLastMessageInfo(
                row.lastMessagePreview(),
                senderName,
                row.lastMessageCreatedAt()
            );
        }
        boolean joined = row.requesterState() == CommunityThreadMemberState.ACTIVE;
        return new ThreadSummaryInfo(
            row.threadId(), row.title(), row.description(), row.category(), row.icon(),
            row.memberCount(), row.unreadCount(), threadProperties.maxMembers(),
            row.pinned(), row.muted(), joined, row.requesterRole(), lastMessage,
            row.creatorMemberId(), row.createdAt(), row.updatedAt()
        );
    }

    private Comparator<ThreadMemberInfo> memberOrder() {
        return Comparator.comparingInt((ThreadMemberInfo info) -> info.role().ordinal())
            .thenComparing(ThreadMemberInfo::name, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(ThreadMemberInfo::memberId);
    }

    private CommunityThreadCategory categoryOf(ThreadListFilter filter) {
        return switch (filter) {
            case ALL, UNREAD -> null;
            case STUDY -> CommunityThreadCategory.STUDY;
            case QNA -> CommunityThreadCategory.QNA;
            case PROJECT -> CommunityThreadCategory.PROJECT;
            case FREE -> CommunityThreadCategory.FREE;
        };
    }

    private record LatestChallenger(
        ChallengerBasicInfo challenger,
        Long generation
    ) {
    }
}
