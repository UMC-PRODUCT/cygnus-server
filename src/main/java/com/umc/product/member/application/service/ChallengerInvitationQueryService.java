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
import com.umc.product.member.application.port.in.query.SearchChallengerInvitationUseCase;
import com.umc.product.member.application.port.in.query.dto.ChallengerInvitationInfo;
import com.umc.product.member.application.port.in.query.dto.ChallengerInvitationSearchResult;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.member.application.port.in.query.dto.SearchChallengerInvitationQuery;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChallengerInvitationQueryService implements SearchChallengerInvitationUseCase {

    private final GetGisuUseCase getGisuUseCase;
    private final GetChallengerUseCase getChallengerUseCase;
    private final GetMemberUseCase getMemberUseCase;

    /**
     * 회원별 최신 챌린저 후보를 public Query UseCase로 한 번 materialize한 뒤
     * Member 서비스 경계에서 필터·정렬·페이지합니다.
     * 챌린저 이력이 있는 회원 수는 운영 상한 안에 있다는 전제에서,
     * 외부 도메인 저장소를 Member adapter에 끌어오지 않는
     * 아키텍처 정합성을 우선합니다.
     */
    @Override
    public ChallengerInvitationSearchResult search(SearchChallengerInvitationQuery query) {
        List<ChallengerBasicInfo> eligibleChallengers = getChallengerUseCase
            .listLatestBasicPerMember()
            .stream()
            .filter(this::isEligible)
            .filter(challenger -> !query.excludedMemberIds().contains(challenger.memberId()))
            .toList();
        if (eligibleChallengers.isEmpty()) {
            return emptySearchResult();
        }

        Map<Long, MemberInfo> memberInfos = loadMemberInfos(eligibleChallengers);
        Map<Long, Long> generationByGisuId = loadGenerationByGisuId(eligibleChallengers);
        String normalizedKeyword = normalizeKeyword(query.keyword());
        List<ChallengerInvitationInfo> candidates = eligibleChallengers.stream()
            .map(challenger -> toInvitationInfo(
                challenger,
                memberInfos.get(challenger.memberId()),
                generationByGisuId.get(challenger.gisuId()),
                normalizedKeyword
            ))
            .flatMap(Optional::stream)
            .sorted(invitationOrder())
            .toList();

        return page(candidates, query);
    }

    @Override
    public Map<Long, ChallengerInvitationInfo> batchGetEligibleChallengers(Set<Long> memberIds) {
        validateBatchRequest(memberIds);
        if (memberIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<ChallengerBasicInfo>> historiesByMemberId = getChallengerUseCase
            .getAllBasicByMemberIds(memberIds);
        List<ChallengerBasicInfo> allChallengers = historiesByMemberId
            .values()
            .stream()
            .flatMap(List::stream)
            .toList();
        if (allChallengers.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> generationByGisuId = loadGenerationByGisuId(allChallengers);
        if (generationByGisuId.size() != countDistinctGisus(allChallengers)) {
            return Map.of();
        }

        List<ChallengerBasicInfo> eligibleChallengers = historiesByMemberId
            .values()
            .stream()
            .map(challengers -> latestChallenger(challengers, generationByGisuId))
            .flatMap(Optional::stream)
            .filter(this::isEligible)
            .toList();
        if (eligibleChallengers.isEmpty()) {
            return Map.of();
        }

        Map<Long, MemberInfo> memberInfos = loadMemberInfos(eligibleChallengers);
        Map<Long, ChallengerInvitationInfo> result = eligibleChallengers.stream()
            .map(challenger -> toInvitationInfo(
                challenger,
                memberInfos.get(challenger.memberId()),
                generationByGisuId.get(challenger.gisuId()),
                null
            ))
            .flatMap(Optional::stream)
            .collect(Collectors.toMap(
                ChallengerInvitationInfo::memberId,
                item -> item,
                (first, ignored) -> first
            ));
        return Map.copyOf(result);
    }

    private Optional<ChallengerInvitationInfo> toInvitationInfo(
        ChallengerBasicInfo challenger,
        MemberInfo member,
        Long generation,
        String normalizedKeyword
    ) {
        if (member == null || generation == null || !matchesKeyword(member.name(), normalizedKeyword)) {
            return Optional.empty();
        }
        return Optional.of(new ChallengerInvitationInfo(
            challenger.memberId(),
            challenger.challengerId(),
            member.name(),
            challenger.part(),
            generation
        ));
    }

    private Optional<ChallengerBasicInfo> latestChallenger(
        List<ChallengerBasicInfo> challengers,
        Map<Long, Long> generationByGisuId
    ) {
        return challengers.stream()
            .max(Comparator.comparing(
                    (ChallengerBasicInfo challenger) -> generationByGisuId.get(challenger.gisuId())
                )
                .thenComparing(ChallengerBasicInfo::gisuId)
                .thenComparing(ChallengerBasicInfo::challengerId));
    }

    private boolean isEligible(ChallengerBasicInfo challenger) {
        return challenger.challengerStatus() == ChallengerStatus.ACTIVE
            || challenger.challengerStatus() == ChallengerStatus.GRADUATED;
    }

    private Map<Long, MemberInfo> loadMemberInfos(List<ChallengerBasicInfo> challengers) {
        Set<Long> memberIds = challengers.stream()
            .map(ChallengerBasicInfo::memberId)
            .collect(Collectors.toSet());
        return getMemberUseCase.findAllByIds(memberIds);
    }

    private Map<Long, Long> loadGenerationByGisuId(List<ChallengerBasicInfo> challengers) {
        Set<Long> gisuIds = challengers.stream()
            .map(ChallengerBasicInfo::gisuId)
            .collect(Collectors.toSet());
        return getGisuUseCase.getByIds(gisuIds).stream()
            .collect(Collectors.toUnmodifiableMap(GisuInfo::gisuId, GisuInfo::generation));
    }

    private long countDistinctGisus(List<ChallengerBasicInfo> challengers) {
        return challengers.stream()
            .map(ChallengerBasicInfo::gisuId)
            .distinct()
            .count();
    }

    private boolean matchesKeyword(String name, String normalizedKeyword) {
        return normalizedKeyword == null
            || name.toLowerCase(Locale.ROOT).contains(normalizedKeyword);
    }

    private Comparator<ChallengerInvitationInfo> invitationOrder() {
        return Comparator.comparing(ChallengerInvitationInfo::name)
            .thenComparing(ChallengerInvitationInfo::memberId)
            .thenComparing(ChallengerInvitationInfo::challengerId);
    }

    private ChallengerInvitationSearchResult page(
        List<ChallengerInvitationInfo> candidates,
        SearchChallengerInvitationQuery query
    ) {
        long total = candidates.size();
        long from = Math.min((long) query.offset(), total);
        long to = Math.min(from + query.limit(), total);
        List<ChallengerInvitationInfo> items = candidates.subList((int) from, (int) to);
        Integer nextOffset = to < total ? Math.toIntExact(to) : null;
        return new ChallengerInvitationSearchResult(items, nextOffset, total);
    }

    private ChallengerInvitationSearchResult emptySearchResult() {
        return new ChallengerInvitationSearchResult(List.of(), null, 0L);
    }

    private void validateBatchRequest(Set<Long> memberIds) {
        if (memberIds == null
            || memberIds.stream().anyMatch(memberId -> memberId == null || memberId <= 0)) {
            throw new IllegalArgumentException("memberIds must contain positive IDs");
        }
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null ? null : keyword.toLowerCase(Locale.ROOT);
    }
}
