package com.umc.product.member.application.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerBasicInfo;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.SearchActiveChallengerInvitationUseCase;
import com.umc.product.member.application.port.in.query.dto.ActiveChallengerInvitationInfo;
import com.umc.product.member.application.port.in.query.dto.ActiveChallengerInvitationSearchResult;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.member.application.port.in.query.dto.SearchActiveChallengerInvitationQuery;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActiveChallengerInvitationQueryService implements SearchActiveChallengerInvitationUseCase {

    private final GetGisuUseCase getGisuUseCase;
    private final GetChallengerUseCase getChallengerUseCase;
    private final GetMemberUseCase getMemberUseCase;

    /**
     * 활성 기수의 후보를 public Query UseCase로 한 번 materialize한 뒤
     * Member 서비스 경계에서 필터·정렬·페이지합니다.
     * 활성 기수 후보 수는 운영 상한 안에 있다는 전제에서,
     * 외부 도메인 저장소를 Member adapter에 끌어오지 않는
     * 아키텍처 정합성을 우선합니다.
     */
    @Override
    public ActiveChallengerInvitationSearchResult search(SearchActiveChallengerInvitationQuery query) {
        return currentActiveGisu(query.gisuId())
            .map(activeGisu -> searchWithinActiveGisu(query, activeGisu))
            .orElseGet(this::emptySearchResult);
    }

    @Override
    public Map<Long, ActiveChallengerInvitationInfo> batchGetEligibleActiveChallengers(
        Long gisuId,
        Set<Long> memberIds
    ) {
        validateBatchRequest(gisuId, memberIds);
        if (memberIds.isEmpty()) {
            return Map.of();
        }

        return currentActiveGisu(gisuId)
            .map(activeGisu -> batchWithinActiveGisu(gisuId, memberIds, activeGisu))
            .orElseGet(Map::of);
    }

    private ActiveChallengerInvitationSearchResult searchWithinActiveGisu(
        SearchActiveChallengerInvitationQuery query,
        GisuInfo activeGisu
    ) {
        List<ChallengerBasicInfo> activeChallengers = getChallengerUseCase
            .listBasicByGisuId(activeGisu.gisuId())
            .stream()
            .filter(challenger -> challenger.challengerStatus() == ChallengerStatus.ACTIVE)
            .filter(challenger -> !query.excludedMemberIds().contains(challenger.memberId()))
            .toList();
        if (activeChallengers.isEmpty()) {
            return emptySearchResult();
        }

        Set<Long> memberIds = activeChallengers.stream()
            .map(ChallengerBasicInfo::memberId)
            .collect(Collectors.toSet());
        Map<Long, MemberInfo> memberInfos = getMemberUseCase.findAllByIds(memberIds);
        SearchContext context = new SearchContext(
            activeGisu.generation(),
            normalizeKeyword(query.keyword())
        );

        List<ActiveChallengerInvitationInfo> candidates = activeChallengers.stream()
            .map(challenger -> toSearchItem(challenger, memberInfos.get(challenger.memberId()), context))
            .flatMap(Optional::stream)
            .sorted(invitationOrder())
            .toList();

        return page(candidates, query);
    }

    private Map<Long, ActiveChallengerInvitationInfo> batchWithinActiveGisu(
        Long gisuId,
        Set<Long> memberIds,
        GisuInfo activeGisu
    ) {
        List<ChallengerBasicInfo> challengers = getChallengerUseCase
            .listBasicByMemberIdsAndGisuId(memberIds, gisuId)
            .stream()
            .filter(challenger -> challenger.challengerStatus() == ChallengerStatus.ACTIVE)
            .toList();
        if (challengers.isEmpty()) {
            return Map.of();
        }

        Map<Long, MemberInfo> memberInfos = getMemberUseCase.findAllByIds(memberIds);
        Map<Long, ActiveChallengerInvitationInfo> result = challengers.stream()
            .map(challenger -> toBatchItem(challenger, memberInfos.get(challenger.memberId()), activeGisu.generation()))
            .flatMap(Optional::stream)
            .collect(Collectors.toMap(
                ActiveChallengerInvitationInfo::memberId,
                item -> item,
                (first, ignored) -> first
            ));
        return Map.copyOf(result);
    }

    private Optional<ActiveChallengerInvitationInfo> toSearchItem(
        ChallengerBasicInfo challenger,
        MemberInfo member,
        SearchContext context
    ) {
        if (member == null || !matchesKeyword(member.name(), context.normalizedKeyword())) {
            return Optional.empty();
        }
        return Optional.of(new ActiveChallengerInvitationInfo(
            challenger.memberId(),
            challenger.challengerId(),
            member.name(),
            challenger.part(),
            context.generation()
        ));
    }

    private Optional<ActiveChallengerInvitationInfo> toBatchItem(
        ChallengerBasicInfo challenger,
        MemberInfo member,
        Long generation
    ) {
        if (member == null) {
            return Optional.empty();
        }
        return Optional.of(new ActiveChallengerInvitationInfo(
            challenger.memberId(),
            challenger.challengerId(),
            member.name(),
            challenger.part(),
            generation
        ));
    }

    private Optional<GisuInfo> currentActiveGisu(Long gisuId) {
        return getGisuUseCase.findActiveGisu()
            .filter(gisu -> gisu.isActive() && gisu.gisuId().equals(gisuId));
    }

    private boolean matchesKeyword(String name, String normalizedKeyword) {
        return normalizedKeyword == null
            || name.toLowerCase(Locale.ROOT).contains(normalizedKeyword);
    }

    private Comparator<ActiveChallengerInvitationInfo> invitationOrder() {
        return Comparator.comparing(ActiveChallengerInvitationInfo::name)
            .thenComparing(ActiveChallengerInvitationInfo::memberId)
            .thenComparing(ActiveChallengerInvitationInfo::challengerId);
    }

    private ActiveChallengerInvitationSearchResult page(
        List<ActiveChallengerInvitationInfo> candidates,
        SearchActiveChallengerInvitationQuery query
    ) {
        long total = candidates.size();
        long from = Math.min((long) query.offset(), total);
        long to = Math.min(from + query.limit(), total);
        List<ActiveChallengerInvitationInfo> items = candidates.subList((int) from, (int) to);
        Integer nextOffset = to < total ? Math.toIntExact(to) : null;
        return new ActiveChallengerInvitationSearchResult(items, nextOffset, total);
    }

    private ActiveChallengerInvitationSearchResult emptySearchResult() {
        return new ActiveChallengerInvitationSearchResult(List.of(), null, 0L);
    }

    private void validateBatchRequest(Long gisuId, Set<Long> memberIds) {
        if (gisuId == null || gisuId <= 0 || memberIds == null
            || memberIds.stream().anyMatch(memberId -> memberId == null || memberId <= 0)) {
            throw new IllegalArgumentException("gisuId and memberIds must contain positive IDs");
        }
    }

    private record SearchContext(Long generation, String normalizedKeyword) {
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null ? null : keyword.toLowerCase(Locale.ROOT);
    }
}
