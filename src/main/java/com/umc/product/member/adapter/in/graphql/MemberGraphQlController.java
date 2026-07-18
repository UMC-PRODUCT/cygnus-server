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
import com.umc.product.global.security.CurrentMemberProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.member.adapter.in.graphql.dto.MemberChallengerGraphQlResponse;
import com.umc.product.member.adapter.in.graphql.dto.MemberPageGraphQlRequest;
import com.umc.product.member.adapter.in.graphql.dto.MemberPageGraphQlResponse;
import com.umc.product.member.adapter.in.graphql.dto.MemberSearchChallengerGraphQlResponse;
import com.umc.product.member.adapter.in.graphql.dto.MemberSearchGraphQlRequest;
import com.umc.product.member.adapter.in.graphql.dto.MemberSearchResultGraphQlResponse;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.SearchMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
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
    public MemberInfo me(@Nullable @CurrentMember MemberPrincipal memberPrincipal) {
        Long requesterMemberId = currentMemberId(memberPrincipal);
        return getMemberUseCase.getById(requesterMemberId);
    }

    @QueryMapping
    public MemberInfo member(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long id
    ) {
        Long requesterMemberId = currentMemberId(memberPrincipal);
        checkPermissionUseCase.checkOrThrow(requesterMemberId, memberReadPermission(id));
        return getMemberUseCase.getById(id);
    }

    @QueryMapping
    public List<MemberInfo> members(
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
            .toList();
    }

    @QueryMapping
    public MemberPageGraphQlResponse memberSearch(
        @Argument MemberSearchGraphQlRequest input,
        @Argument MemberPageGraphQlRequest page
    ) {
        Long requesterMemberId = currentMemberId();
        Pageable pageable = (page == null ? new MemberPageGraphQlRequest(null, null) : page).toPageable();
        return MemberPageGraphQlResponse.from(
            searchMemberUseCase.searchByV2ForGraphQl(input.toQuery(), requesterMemberId, pageable).page()
        );
    }

    @BatchMapping(typeName = "Member", field = "school")
    public Map<MemberInfo, SchoolGraphQlResponse> schoolByMember(
        List<MemberInfo> members
    ) {
        Set<Long> readableMemberIds = readableMemberIds(members);

        Set<Long> schoolIds = members.stream()
            .filter(member -> readableMemberIds.contains(member.id()))
            .map(MemberInfo::schoolId)
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

        Map<MemberInfo, SchoolGraphQlResponse> result = new LinkedHashMap<>();
        for (MemberInfo member : members) {
            SchoolDetailInfo school = readableMemberIds.contains(member.id())
                ? schoolsById.get(member.schoolId())
                : null;
            result.put(member, school == null ? null : SchoolGraphQlResponse.from(school));
        }
        return result;
    }

    @BatchMapping(typeName = "Member", field = "challengers")
    public Map<MemberInfo, List<MemberChallengerGraphQlResponse>> challengersByMember(
        List<MemberInfo> members
    ) {
        Set<Long> readableMemberIds = readableMemberIds(members);
        Map<Long, List<ChallengerBasicInfo>> challengersByMemberId = readableMemberIds.isEmpty()
            ? Map.of()
            : getChallengerUseCase.getAllBasicByMemberIds(readableMemberIds);

        return members.stream()
            .collect(Collectors.toMap(
                Function.identity(),
                member -> challengersByMemberId.getOrDefault(member.id(), List.of()).stream()
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
            GisuInfo gisu = gisusById.get(challenger.gisuId());
            result.put(challenger, gisu == null ? null : GisuGraphQlResponse.from(gisu));
        }
        return result;
    }

    @BatchMapping(typeName = "MemberSearchResult", field = "school")
    public Map<MemberSearchResultGraphQlResponse, SchoolGraphQlResponse> schoolByMemberSearchResult(
        List<MemberSearchResultGraphQlResponse> members
    ) {
        Set<Long> schoolIds = members.stream()
            .map(MemberSearchResultGraphQlResponse::schoolId)
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

        Map<MemberSearchResultGraphQlResponse, SchoolGraphQlResponse> result = new LinkedHashMap<>();
        for (MemberSearchResultGraphQlResponse member : members) {
            SchoolDetailInfo school = member.schoolId() == null ? null : schoolsById.get(member.schoolId());
            result.put(member, school == null ? null : SchoolGraphQlResponse.from(school));
        }
        return result;
    }

    @BatchMapping(typeName = "MemberSearchChallenger", field = "gisu")
    public Map<MemberSearchChallengerGraphQlResponse, GisuGraphQlResponse> gisuByMemberSearchChallenger(
        List<MemberSearchChallengerGraphQlResponse> challengers
    ) {
        Set<Long> gisuIds = challengers.stream()
            .map(MemberSearchChallengerGraphQlResponse::gisuId)
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

        Map<MemberSearchChallengerGraphQlResponse, GisuGraphQlResponse> result = new LinkedHashMap<>();
        for (MemberSearchChallengerGraphQlResponse challenger : challengers) {
            GisuInfo gisu = challenger.gisuId() == null ? null : gisusById.get(challenger.gisuId());
            result.put(challenger, gisu == null ? null : GisuGraphQlResponse.from(gisu));
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

    private Set<Long> readableMemberIds(List<MemberInfo> members) {
        Long requesterMemberId = currentMemberId();
        Set<Long> memberIds = members.stream()
            .map(MemberInfo::id)
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
