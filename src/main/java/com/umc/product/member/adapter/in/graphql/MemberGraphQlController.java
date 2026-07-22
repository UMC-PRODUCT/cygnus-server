package com.umc.product.member.adapter.in.graphql;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerBasicInfo;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.global.graphql.dto.PageGraphQlRequest;
import com.umc.product.global.security.CurrentMemberProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.member.adapter.in.graphql.dto.MemberChallengerGraphQlResponse;
import com.umc.product.member.adapter.in.graphql.dto.MemberSearchEdgeGraphQlResponse;
import com.umc.product.member.adapter.in.graphql.dto.MemberSearchGraphQlRequest;
import com.umc.product.member.adapter.in.graphql.dto.MemberSearchPageGraphQlResponse;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.SearchMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.member.application.port.in.query.dto.MemberPublicInfo;
import com.umc.product.organization.adapter.in.graphql.dto.GisuGraphQlResponse;
import com.umc.product.organization.adapter.in.graphql.dto.SchoolGraphQlResponse;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolDetailInfo;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class MemberGraphQlController {

    private final GetMemberUseCase getMemberUseCase;
    private final CheckPermissionUseCase checkPermissionUseCase;
    private final GetSchoolUseCase getSchoolUseCase;
    private final GetChallengerUseCase getChallengerUseCase;
    private final GetGisuUseCase getGisuUseCase;
    private final SearchMemberUseCase searchMemberUseCase;
    private final CurrentMemberProvider currentMemberProvider;

    @QueryMapping
    public MemberPublicInfo me(@Nullable @CurrentMember MemberPrincipal memberPrincipal) {
        Long requesterMemberId = currentMemberId(memberPrincipal);
        return MemberPublicInfo.from(getMemberUseCase.getById(requesterMemberId));
    }

    @QueryMapping
    public MemberPublicInfo member(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long id
    ) {
        Long requesterMemberId = currentMemberId(memberPrincipal);
        checkPermissionUseCase.checkOrThrow(requesterMemberId, memberReadPermission(id));
        return MemberPublicInfo.from(getMemberUseCase.getById(id));
    }

    @QueryMapping
    public List<MemberPublicInfo> members(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument List<Long> ids
    ) {
        Long requesterMemberId = currentMemberId(memberPrincipal);
        List<Long> uniqueMemberIds = uniqueMemberIds(ids);
        if (uniqueMemberIds.isEmpty()) {
            return List.of();
        }

        SubjectAttributes subject = checkPermissionUseCase.loadSubject(requesterMemberId);
        uniqueMemberIds.forEach(memberId -> assertMemberRead(subject, memberId));

        Map<Long, MemberInfo> membersById = getMemberUseCase.findAllByIds(new LinkedHashSet<>(uniqueMemberIds));
        return uniqueMemberIds.stream()
            .map(membersById::get)
            .filter(Objects::nonNull)
            .map(MemberPublicInfo::from)
            .toList();
    }

    @QueryMapping
    public MemberSearchPageGraphQlResponse memberSearch(
        @Argument MemberSearchGraphQlRequest input,
        @Argument PageGraphQlRequest page
    ) {
        Long requesterMemberId = currentMemberId();
        Pageable pageable = PageGraphQlRequest.defaultIfNull(page).toPageableWithMaxOffset(10_000L);
        return MemberSearchPageGraphQlResponse.from(
            searchMemberUseCase.searchByV2ForGraphQl(input.toQuery(), requesterMemberId, pageable).page()
        );
    }

    @BatchMapping(typeName = "MemberPublic", field = "school")
    public Map<MemberPublicInfo, SchoolGraphQlResponse> schoolByMember(
        List<MemberPublicInfo> members
    ) {
        Set<Long> readableMemberIds = readableMemberIds(members);

        Set<Long> schoolIds = members.stream()
            .filter(member -> readableMemberIds.contains(member.memberId()))
            .map(MemberPublicInfo::schoolId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<Long, SchoolDetailInfo> schoolsById = schoolIds.isEmpty()
            ? Map.of()
            : getSchoolUseCase.listDetailsByIds(schoolIds).stream()
                .collect(Collectors.toMap(
                    SchoolDetailInfo::schoolId,
                    Function.identity(),
                    (left, right) -> left
                ));

        Map<MemberPublicInfo, SchoolGraphQlResponse> result = new LinkedHashMap<>();
        for (MemberPublicInfo member : members) {
            SchoolDetailInfo school = readableMemberIds.contains(member.memberId()) && member.schoolId() != null
                ? schoolsById.get(member.schoolId())
                : null;
            result.put(member, school == null ? null : SchoolGraphQlResponse.from(school));
        }
        return result;
    }

    @BatchMapping(typeName = "MemberPublic", field = "challengers")
    public Map<MemberPublicInfo, List<MemberChallengerGraphQlResponse>> challengersByMember(
        List<MemberPublicInfo> members
    ) {
        Set<Long> readableMemberIds = readableMemberIds(members);
        Map<Long, List<ChallengerBasicInfo>> challengersByMemberId = readableMemberIds.isEmpty()
            ? Map.of()
            : getChallengerUseCase.getAllBasicByMemberIds(readableMemberIds);

        return members.stream()
            .collect(Collectors.toMap(
                Function.identity(),
                member -> challengersByMemberId.getOrDefault(member.memberId(), List.of()).stream()
                    .map(MemberChallengerGraphQlResponse::from)
                    .toList(),
                (left, right) -> left,
                LinkedHashMap::new
            ));
    }

    @BatchMapping(typeName = "MemberChallenger", field = "gisu")
    public Map<MemberChallengerGraphQlResponse, GisuGraphQlResponse> gisuByMemberChallenger(
        List<MemberChallengerGraphQlResponse> challengers
    ) {
        Set<Long> gisuIds = challengers.stream()
            .map(MemberChallengerGraphQlResponse::gisuId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, GisuInfo> gisusById = gisuIds.isEmpty()
            ? Map.of()
            : getGisuUseCase.getByIds(gisuIds).stream()
                .collect(Collectors.toMap(
                    GisuInfo::gisuId,
                    Function.identity(),
                    (left, right) -> left
                ));

        Map<MemberChallengerGraphQlResponse, GisuGraphQlResponse> result = new LinkedHashMap<>();
        for (MemberChallengerGraphQlResponse challenger : challengers) {
            GisuInfo gisu = challenger.gisuId() == null ? null : gisusById.get(challenger.gisuId());
            result.put(challenger, gisu == null ? null : GisuGraphQlResponse.from(gisu));
        }
        return result;
    }

    @BatchMapping(typeName = "MemberSearchEdge", field = "currentChallenger")
    public Map<MemberSearchEdgeGraphQlResponse, MemberChallengerGraphQlResponse> currentChallengerBySearchEdge(
        List<MemberSearchEdgeGraphQlResponse> edges
    ) {
        Set<Long> challengerIds = edges.stream()
            .map(MemberSearchEdgeGraphQlResponse::currentChallengerId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, ChallengerInfo> challengersById = challengerIds.isEmpty()
            ? Map.of()
            : getChallengerUseCase.getAllByIds(challengerIds).stream()
                .collect(Collectors.toMap(
                    ChallengerInfo::challengerId,
                    Function.identity(),
                    (left, right) -> left
                ));

        Map<MemberSearchEdgeGraphQlResponse, MemberChallengerGraphQlResponse> result = new LinkedHashMap<>();
        for (MemberSearchEdgeGraphQlResponse edge : edges) {
            ChallengerInfo challenger = edge.currentChallengerId() == null
                ? null
                : challengersById.get(edge.currentChallengerId());
            result.put(edge, challenger == null ? null : MemberChallengerGraphQlResponse.from(challenger));
        }
        return result;
    }

    private Long currentMemberId(MemberPrincipal memberPrincipal) {
        return memberPrincipal == null
            ? currentMemberProvider.getRequiredCurrentMemberId()
            : memberPrincipal.getMemberId();
    }

    private Long currentMemberId() {
        return currentMemberProvider.getRequiredCurrentMemberId();
    }

    private List<Long> uniqueMemberIds(List<Long> memberIds) {
        return memberIds.stream()
            .collect(Collectors.collectingAndThen(
                Collectors.toCollection(LinkedHashSet::new),
                List::copyOf
            ));
    }

    private void assertMemberRead(SubjectAttributes subject, Long memberId) {
        if (!checkPermissionUseCase.check(subject, memberReadPermission(memberId))) {
            throw new AccessDeniedException("회원 정보를 볼 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요.");
        }
    }

    private Set<Long> readableMemberIds(List<MemberPublicInfo> members) {
        Long requesterMemberId = currentMemberId();
        Set<Long> memberIds = members.stream()
            .map(MemberPublicInfo::memberId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> readableMemberIds = new LinkedHashSet<>();
        if (memberIds.remove(requesterMemberId)) {
            readableMemberIds.add(requesterMemberId);
        }
        if (!memberIds.isEmpty()) {
            SubjectAttributes subject = checkPermissionUseCase.loadSubject(requesterMemberId);
            memberIds.stream()
                .filter(memberId -> checkPermissionUseCase.check(subject, memberReadPermission(memberId)))
                .forEach(readableMemberIds::add);
        }
        return readableMemberIds;
    }

    private ResourcePermission memberReadPermission(Long memberId) {
        return ResourcePermission.of(ResourceType.MEMBER, memberId, PermissionType.READ);
    }
}
