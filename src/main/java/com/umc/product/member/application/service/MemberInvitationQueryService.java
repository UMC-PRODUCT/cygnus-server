package com.umc.product.member.application.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerBasicInfo;
import com.umc.product.member.application.port.in.query.SearchMemberInvitationUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInvitationInfo;
import com.umc.product.member.application.port.in.query.dto.MemberInvitationSearchResult;
import com.umc.product.member.application.port.in.query.dto.SearchMemberInvitationQuery;
import com.umc.product.member.application.port.out.SearchMemberInvitationPort;
import com.umc.product.member.application.port.out.dto.MemberInvitationCandidate;
import com.umc.product.member.application.port.out.dto.MemberInvitationCandidatePage;
import com.umc.product.member.application.port.out.dto.SearchMemberInvitationCondition;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberInvitationQueryService implements SearchMemberInvitationUseCase {

    private final SearchMemberInvitationPort searchMemberInvitationPort;
    private final GetGisuUseCase getGisuUseCase;
    private final GetChallengerUseCase getChallengerUseCase;

    @Override
    public MemberInvitationSearchResult search(SearchMemberInvitationQuery query) {
        MemberInvitationCandidatePage candidatePage = searchMemberInvitationPort.search(
            new SearchMemberInvitationCondition(
                query.keyword(),
                query.excludedMemberIds(),
                query.offset(),
                query.limit()
            )
        );
        if (candidatePage.items().isEmpty()) {
            return new MemberInvitationSearchResult(List.of(), null, candidatePage.total());
        }

        Set<Long> memberIds = candidatePage.items().stream()
            .map(MemberInvitationCandidate::memberId)
            .collect(Collectors.toSet());
        Map<Long, LatestChallenger> latestChallengerByMemberId = loadLatestChallengers(memberIds);
        List<MemberInvitationInfo> items = candidatePage.items().stream()
            .map(candidate -> toInvitationInfo(
                candidate,
                latestChallengerByMemberId.get(candidate.memberId())
            ))
            .toList();

        int consumed = Math.addExact(query.offset(), items.size());
        Integer nextOffset = consumed < candidatePage.total() ? consumed : null;
        return new MemberInvitationSearchResult(items, nextOffset, candidatePage.total());
    }

    @Override
    public Set<Long> batchGetInvitableMemberIds(Set<Long> memberIds) {
        validateBatchRequest(memberIds);
        if (memberIds.isEmpty()) {
            return Set.of();
        }

        return searchMemberInvitationPort.findActiveMemberIds(memberIds);
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

    private MemberInvitationInfo toInvitationInfo(
        MemberInvitationCandidate candidate,
        LatestChallenger latest
    ) {
        if (latest == null) {
            return new MemberInvitationInfo(candidate.memberId(), null, candidate.name(), null, null);
        }
        return new MemberInvitationInfo(
            candidate.memberId(),
            latest.challenger().challengerId(),
            candidate.name(),
            latest.challenger().part(),
            latest.generation()
        );
    }

    private void validateBatchRequest(Set<Long> memberIds) {
        if (memberIds == null
            || memberIds.stream().anyMatch(memberId -> memberId == null || memberId <= 0)) {
            throw new IllegalArgumentException("memberIds must contain positive IDs");
        }
    }

    private record LatestChallenger(
        ChallengerBasicInfo challenger,
        Long generation
    ) {
    }
}
