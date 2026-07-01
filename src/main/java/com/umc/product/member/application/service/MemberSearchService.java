package com.umc.product.member.application.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleInfo;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerBasicInfo;
import com.umc.product.challenger.domain.Challenger;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.SearchMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.ChallengerSearchItemV2Info;
import com.umc.product.member.application.port.in.query.dto.ChallengerSearchV2Result;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.member.application.port.in.query.dto.SearchMemberAccessScope;
import com.umc.product.member.application.port.in.query.dto.SearchMemberItemInfo;
import com.umc.product.member.application.port.in.query.dto.SearchMemberItemV2Info;
import com.umc.product.member.application.port.in.query.dto.SearchMemberItemV2Info.Participation;
import com.umc.product.member.application.port.in.query.dto.SearchMemberItemV2Info.PrimaryChallenger;
import com.umc.product.member.application.port.in.query.dto.SearchMemberQuery;
import com.umc.product.member.application.port.in.query.dto.SearchMemberResult;
import com.umc.product.member.application.port.in.query.dto.SearchMemberV2Result;
import com.umc.product.member.application.port.out.SearchMemberPort;
import com.umc.product.member.domain.exception.MemberDomainException;
import com.umc.product.member.domain.exception.MemberErrorCode;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberSearchService implements SearchMemberUseCase {

    private final SearchMemberPort searchMemberPort;

    private final GetMemberUseCase getMemberUseCase;
    private final GetChallengerUseCase getChallengerUseCase;
    private final GetChallengerRoleUseCase getChallengerRoleUseCase;
    private final GetGisuUseCase getGisuUseCase;

    @Override
    public SearchMemberResult searchBy(SearchMemberQuery query, Pageable pageable) {
        SearchMemberQuery scopedQuery = applyAccessScope(query);
        Page<Challenger> challengers = searchMemberPort.search(scopedQuery, pageable);

        // 배치 데이터 로딩
        Map<Long, MemberInfo> memberProfiles = loadMemberProfiles(challengers.getContent());
        Map<Long, List<ChallengerRoleType>> roleTypes = loadRoleTypes(challengers.getContent());
        Map<Long, Long> gisuGenerationMap = loadGisuGenerationMap(challengers.getContent());

        Page<SearchMemberItemInfo> items = challengers.map(challenger ->
            toItemInfo(challenger, memberProfiles, roleTypes, gisuGenerationMap)
        );

        return new SearchMemberResult(items);
    }

    @Override
    public ChallengerSearchV2Result searchChallengersByV2(SearchMemberQuery query, Pageable pageable) {
        SearchMemberQuery scopedQuery = applyAccessScope(query);
        Page<Challenger> challengers = searchMemberPort.search(scopedQuery, pageable);
        List<Challenger> content = challengers.getContent();

        Map<Long, MemberInfo> memberProfiles = loadMemberProfiles(content);
        Map<Long, List<ChallengerRoleType>> roleTypes = loadRoleTypes(content);
        Map<Long, Long> gisuGenerationMap = loadGisuGenerationMap(content);

        // 활성 기수 + 해당 회원이 활성 기수에 운영진 ChallengerRole을 보유하는지 회원 단위로 산출
        Long activeGisuId = getGisuUseCase.findActiveGisu()
            .map(GisuInfo::gisuId)
            .orElse(null);
        Map<Long, ChallengerBasicInfo> activeGisuChallengerByMemberId =
            loadActiveGisuChallengerByMemberId(content, activeGisuId);
        Set<Long> adminChallengerIdsInActiveGisu =
            collectAdminChallengerIdsInActiveGisu(content, roleTypes, activeGisuChallengerByMemberId);

        Page<ChallengerSearchItemV2Info> items = challengers.map(challenger ->
            toChallengerSearchItemV2(challenger, memberProfiles, roleTypes, gisuGenerationMap,
                adminChallengerIdsInActiveGisu)
        );

        return new ChallengerSearchV2Result(items);
    }

    @Override
    public SearchMemberV2Result searchByV2(SearchMemberQuery query, Pageable pageable) {
        SearchMemberQuery scopedQuery = applyAccessScope(query);
        Page<Long> memberIdPage = searchMemberPort.searchMemberIds(scopedQuery, pageable);
        List<Long> memberIds = memberIdPage.getContent();

        if (memberIds.isEmpty()) {
            return new SearchMemberV2Result(memberIdPage.map(id -> null));
        }

        Set<Long> memberIdSet = Set.copyOf(memberIds);

        // 회원 정보 batch 로딩
        Map<Long, MemberInfo> memberInfoMap = getMemberUseCase.findAllByIds(memberIdSet);

        // 회원별 모든 챌린저 batch 로딩 (IN 쿼리 1회)
        Map<Long, List<ChallengerBasicInfo>> challengersByMemberId =
            getChallengerUseCase.getAllBasicByMemberIds(memberIdSet);

        // 활성 기수 한 번 조회 (휴지기에는 Optional.empty)
        Optional<GisuInfo> activeGisuOpt = getGisuUseCase.findActiveGisu();
        Long activeGisuId = activeGisuOpt.map(GisuInfo::gisuId).orElse(null);

        // 모든 챌린저의 gisuId를 모아 generation 매핑 일괄 조회
        Map<Long, Long> generationByGisuId = loadGenerationMap(challengersByMemberId);

        // 활성 기수에 속한 챌린저들의 운영진 RoleType 일괄 조회 → 회원 단위 isAdminInActiveGisu 산출
        Set<Long> memberIdsWithAdminInActiveGisu =
            computeMembersWithAdminInActiveGisu(challengersByMemberId, activeGisuId);

        Page<SearchMemberItemV2Info> items = memberIdPage.map(memberId -> toItemV2Info(
            memberId,
            memberInfoMap,
            challengersByMemberId.getOrDefault(memberId, List.of()),
            generationByGisuId,
            activeGisuId,
            memberIdsWithAdminInActiveGisu.contains(memberId)
        ));

        return new SearchMemberV2Result(items);
    }

    private SearchMemberQuery applyAccessScope(SearchMemberQuery query) {
        if (query.requesterMemberId() == null) {
            return query;
        }

        SearchMemberAccessScope accessScope = resolveAccessScope(query.requesterMemberId());
        if (accessScope.noAccess()) {
            throw new MemberDomainException(MemberErrorCode.MEMBER_SEARCH_ACCESS_DENIED);
        }

        return query.withAccessScope(accessScope);
    }

    private SearchMemberAccessScope resolveAccessScope(Long requesterMemberId) {
        List<ChallengerRoleInfo> roles = getChallengerRoleUseCase.findAllByMemberId(requesterMemberId);

        if (roles.stream().anyMatch(role -> role.roleType().isAtLeastCentralCore())) {
            return SearchMemberAccessScope.all();
        }

        Set<Long> schoolIds = roles.stream()
            .filter(role -> role.roleType() == ChallengerRoleType.SCHOOL_PRESIDENT
                || role.roleType() == ChallengerRoleType.SCHOOL_VICE_PRESIDENT)
            .map(ChallengerRoleInfo::organizationId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        if (!schoolIds.isEmpty()) {
            return SearchMemberAccessScope.ofSchoolIds(schoolIds);
        }

        Map<Long, List<ChallengerBasicInfo>> challengersByMemberId =
            getChallengerUseCase.getAllBasicByMemberIds(Set.of(requesterMemberId));
        Set<Long> gisuIds = challengersByMemberId.getOrDefault(requesterMemberId, List.of()).stream()
            .map(ChallengerBasicInfo::gisuId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        if (!gisuIds.isEmpty()) {
            return SearchMemberAccessScope.ofGisuIds(gisuIds);
        }

        return SearchMemberAccessScope.none();
    }

    // ======= PRIVATE — v1 helpers =========

    private SearchMemberItemInfo toItemInfo(
        Challenger challenger,
        Map<Long, MemberInfo> memberProfiles,
        Map<Long, List<ChallengerRoleType>> roleTypes,
        Map<Long, Long> gisuGenerationMap
    ) {
        MemberInfo profile = memberProfiles.get(challenger.getMemberId());

        return new SearchMemberItemInfo(
            challenger.getMemberId(),
            profile != null ? profile.name() : null,
            profile != null ? profile.nickname() : null,
            profile != null ? profile.email() : null,
            profile != null ? profile.schoolId() : null,
            profile != null ? profile.schoolName() : null,
            profile != null ? profile.profileImageLink() : null,
            challenger.getId(),
            challenger.getGisuId(),
            gisuGenerationMap.get(challenger.getGisuId()),
            challenger.getPart(),
            roleTypes.getOrDefault(challenger.getId(), List.of())
        );
    }

    private Map<Long, MemberInfo> loadMemberProfiles(List<Challenger> challengers) {
        Set<Long> memberIds = challengers.stream()
            .map(Challenger::getMemberId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        if (memberIds.isEmpty()) {
            return Map.of();
        }

        return getMemberUseCase.findAllByIds(memberIds);
    }

    private Map<Long, List<ChallengerRoleType>> loadRoleTypes(List<Challenger> challengers) {
        Set<Long> challengerIds = challengers.stream()
            .map(Challenger::getId)
            .collect(Collectors.toSet());

        if (challengerIds.isEmpty()) {
            return Map.of();
        }

        return getChallengerRoleUseCase.getAllRoleTypesByChallengerIds(challengerIds);
    }

    private Map<Long, Long> loadGisuGenerationMap(List<Challenger> challengers) {
        Set<Long> gisuIds = challengers.stream()
            .map(Challenger::getGisuId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        if (gisuIds.isEmpty()) {
            return Map.of();
        }

        return getGisuUseCase.getByIds(gisuIds).stream()
            .collect(Collectors.toMap(GisuInfo::gisuId, GisuInfo::generation));
    }

    // ======= PRIVATE — challenger search v2 helpers =========

    /**
     * 검색 결과 챌린저 행들 가운데, 같은 회원이 활성 기수에 운영진 ChallengerRole을 보유하면
     * 그 회원이 가진 모든 챌린저 행에 대해 isAdminInActiveGisu=true가 되도록 챌린저 ID 집합을 만듭니다.
     */
    private Map<Long, ChallengerBasicInfo> loadActiveGisuChallengerByMemberId(
        List<Challenger> content,
        Long activeGisuId
    ) {
        if (activeGisuId == null || content.isEmpty()) {
            return Map.of();
        }

        Set<Long> memberIds = content.stream()
            .map(Challenger::getMemberId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        if (memberIds.isEmpty()) {
            return Map.of();
        }

        return getChallengerUseCase.listBasicByMemberIdsAndGisuId(memberIds, activeGisuId).stream()
            .collect(Collectors.toMap(ChallengerBasicInfo::memberId, c -> c, (a, b) -> a));
    }

    private Set<Long> collectAdminChallengerIdsInActiveGisu(
        List<Challenger> content,
        Map<Long, List<ChallengerRoleType>> roleTypes,
        Map<Long, ChallengerBasicInfo> activeGisuChallengerByMemberId
    ) {
        if (content.isEmpty() || activeGisuChallengerByMemberId.isEmpty()) {
            return Set.of();
        }

        Set<Long> activeGisuChallengerIds = activeGisuChallengerByMemberId.values().stream()
            .map(ChallengerBasicInfo::challengerId)
            .collect(Collectors.toSet());

        Set<Long> missingRoleChallengerIds = activeGisuChallengerIds.stream()
            .filter(challengerId -> !roleTypes.containsKey(challengerId))
            .collect(Collectors.toSet());

        Map<Long, List<ChallengerRoleType>> missingRoleTypes = missingRoleChallengerIds.isEmpty()
            ? Map.of()
            : getChallengerRoleUseCase.getAllRoleTypesByChallengerIds(missingRoleChallengerIds);

        Set<Long> adminMemberIds = activeGisuChallengerByMemberId.values().stream()
            .filter(c -> !roleTypes.getOrDefault(
                c.challengerId(),
                missingRoleTypes.getOrDefault(c.challengerId(), List.of())
            ).isEmpty())
            .map(ChallengerBasicInfo::memberId)
            .collect(Collectors.toSet());

        return content.stream()
            .filter(c -> adminMemberIds.contains(c.getMemberId()))
            .map(Challenger::getId)
            .collect(Collectors.toSet());
    }

    private ChallengerSearchItemV2Info toChallengerSearchItemV2(
        Challenger challenger,
        Map<Long, MemberInfo> memberProfiles,
        Map<Long, List<ChallengerRoleType>> roleTypes,
        Map<Long, Long> gisuGenerationMap,
        Set<Long> adminChallengerIdsInActiveGisu
    ) {
        MemberInfo profile = memberProfiles.get(challenger.getMemberId());

        return new ChallengerSearchItemV2Info(
            challenger.getMemberId(),
            profile != null ? profile.name() : null,
            profile != null ? profile.nickname() : null,
            profile != null ? profile.email() : null,
            profile != null ? profile.schoolId() : null,
            profile != null ? profile.schoolName() : null,
            profile != null ? profile.profileImageLink() : null,
            challenger.getId(),
            challenger.getGisuId(),
            gisuGenerationMap.get(challenger.getGisuId()),
            challenger.getPart(),
            challenger.getStatus(),
            roleTypes.getOrDefault(challenger.getId(), List.of()),
            adminChallengerIdsInActiveGisu.contains(challenger.getId())
        );
    }

    // ======= PRIVATE — member search v2 helpers =========

    private Map<Long, Long> loadGenerationMap(Map<Long, List<ChallengerBasicInfo>> challengersByMemberId) {
        Set<Long> gisuIds = challengersByMemberId.values().stream()
            .flatMap(List::stream)
            .map(ChallengerBasicInfo::gisuId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        if (gisuIds.isEmpty()) {
            return Map.of();
        }

        return getGisuUseCase.getByIds(gisuIds).stream()
            .collect(Collectors.toMap(GisuInfo::gisuId, GisuInfo::generation));
    }

    private Set<Long> computeMembersWithAdminInActiveGisu(
        Map<Long, List<ChallengerBasicInfo>> challengersByMemberId,
        Long activeGisuId
    ) {
        if (activeGisuId == null) {
            return Set.of();
        }

        Set<Long> activeGisuChallengerIds = challengersByMemberId.values().stream()
            .flatMap(List::stream)
            .filter(c -> Objects.equals(c.gisuId(), activeGisuId))
            .map(ChallengerBasicInfo::challengerId)
            .collect(Collectors.toSet());

        if (activeGisuChallengerIds.isEmpty()) {
            return Set.of();
        }

        Map<Long, List<ChallengerRoleType>> rolesByChallengerId =
            getChallengerRoleUseCase.getAllRoleTypesByChallengerIds(activeGisuChallengerIds);

        Set<Long> adminChallengerIds = rolesByChallengerId.entrySet().stream()
            .filter(e -> !e.getValue().isEmpty())
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());

        return challengersByMemberId.entrySet().stream()
            .filter(e -> e.getValue().stream()
                .anyMatch(c -> adminChallengerIds.contains(c.challengerId())))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }

    private SearchMemberItemV2Info toItemV2Info(
        Long memberId,
        Map<Long, MemberInfo> memberInfoMap,
        List<ChallengerBasicInfo> myChallengers,
        Map<Long, Long> generationByGisuId,
        Long activeGisuId,
        boolean isAdminInActiveGisu
    ) {
        MemberInfo m = memberInfoMap.get(memberId);

        PrimaryChallenger primary = selectPrimaryChallenger(myChallengers, generationByGisuId, activeGisuId);
        List<Participation> participations = myChallengers.stream()
            .sorted(Comparator
                .comparing((ChallengerBasicInfo c) -> generationByGisuId.getOrDefault(c.gisuId(), 0L)).reversed()
                .thenComparing(ChallengerBasicInfo::challengerId))
            .map(c -> new Participation(
                c.challengerId(),
                c.gisuId(),
                generationByGisuId.get(c.gisuId()),
                c.part(),
                c.challengerStatus()
            ))
            .toList();

        return new SearchMemberItemV2Info(
            memberId,
            m != null ? m.name() : null,
            m != null ? m.nickname() : null,
            m != null ? m.email() : null,
            m != null ? m.schoolId() : null,
            m != null ? m.schoolName() : null,
            m != null ? m.profileImageLink() : null,
            primary,
            isAdminInActiveGisu,
            participations
        );
    }

    /**
     * 활성 기수 챌린저가 있으면 그것을, 없으면 가장 최신 기수의 챌린저를 대표로 선택합니다.
     */
    private PrimaryChallenger selectPrimaryChallenger(
        List<ChallengerBasicInfo> challengers,
        Map<Long, Long> generationByGisuId,
        Long activeGisuId
    ) {
        if (challengers.isEmpty()) {
            return null;
        }

        Optional<ChallengerBasicInfo> active = activeGisuId == null ? Optional.empty()
            : challengers.stream()
                .filter(c -> Objects.equals(c.gisuId(), activeGisuId))
                .findFirst();

        ChallengerBasicInfo picked = active.orElseGet(() -> challengers.stream()
            .max(Comparator.comparing(c -> generationByGisuId.getOrDefault(c.gisuId(), 0L)))
            .orElseThrow());

        return new PrimaryChallenger(
            picked.challengerId(),
            picked.gisuId(),
            generationByGisuId.get(picked.gisuId()),
            picked.part(),
            picked.challengerStatus()
        );
    }
}
