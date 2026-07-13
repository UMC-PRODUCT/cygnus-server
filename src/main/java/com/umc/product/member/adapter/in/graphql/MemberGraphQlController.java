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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerBasicInfo;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.member.adapter.in.graphql.dto.MemberChallengerGraphQlResponse;
import com.umc.product.member.adapter.in.graphql.dto.MemberGisuGraphQlResponse;
import com.umc.product.member.adapter.in.graphql.dto.MemberGraphQlResponse;
import com.umc.product.member.adapter.in.graphql.dto.MemberPageGraphQlRequest;
import com.umc.product.member.adapter.in.graphql.dto.MemberPageGraphQlResponse;
import com.umc.product.member.adapter.in.graphql.dto.MemberSchoolGraphQlResponse;
import com.umc.product.member.adapter.in.graphql.dto.MemberSearchChallengerGraphQlResponse;
import com.umc.product.member.adapter.in.graphql.dto.MemberSearchGraphQlRequest;
import com.umc.product.member.adapter.in.graphql.dto.MemberSearchResultGraphQlResponse;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.SearchMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.organization.adapter.in.graphql.dto.GisuGraphQlResponse;
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

    @QueryMapping
    public MemberGraphQlResponse me() {
        Long requesterMemberId = currentMemberId();
        return MemberGraphQlResponse.privateFrom(getMemberUseCase.getById(requesterMemberId));
    }

    @QueryMapping
    public MemberGraphQlResponse member(@Argument Long id) {
        Long requesterMemberId = currentMemberId();
        checkPermissionUseCase.checkOrThrow(requesterMemberId, memberReadPermission(id));
        return MemberGraphQlResponse.publicFrom(getMemberUseCase.getById(id));
    }

    @QueryMapping
    public List<MemberGraphQlResponse> members(@Argument List<Long> ids) {
        Long requesterMemberId = currentMemberId();
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
            .map(MemberGraphQlResponse::publicFrom)
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
    public Map<MemberGraphQlResponse, MemberSchoolGraphQlResponse> schoolByMember(
        List<MemberGraphQlResponse> members
    ) {
        assertMembersVisible(members);

        Set<Long> schoolIds = members.stream()
            .map(MemberGraphQlResponse::schoolId)
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

        Map<MemberGraphQlResponse, MemberSchoolGraphQlResponse> result = new LinkedHashMap<>();
        for (MemberGraphQlResponse member : members) {
            SchoolDetailInfo school = schoolsById.get(member.schoolId());
            result.put(member, school == null ? null : MemberSchoolGraphQlResponse.from(school));
        }
        return result;
    }

    @BatchMapping(typeName = "Member", field = "challengers")
    public Map<MemberGraphQlResponse, List<MemberChallengerGraphQlResponse>> challengersByMember(
        List<MemberGraphQlResponse> members
    ) {
        assertMembersVisible(members);

        Set<Long> memberIds = members.stream()
            .map(MemberGraphQlResponse::memberId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, List<ChallengerBasicInfo>> challengersByMemberId = memberIds.isEmpty()
            ? Map.of()
            : getChallengerUseCase.getAllBasicByMemberIds(memberIds);

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
    public Map<MemberChallengerGraphQlResponse, MemberGisuGraphQlResponse> gisuByMemberChallenger(
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

        Map<MemberChallengerGraphQlResponse, MemberGisuGraphQlResponse> result = new LinkedHashMap<>();
        for (MemberChallengerGraphQlResponse challenger : challengers) {
            GisuInfo gisu = gisusById.get(challenger.gisuId());
            result.put(challenger, gisu == null ? null : MemberGisuGraphQlResponse.from(gisu));
        }
        return result;
    }

    @BatchMapping(typeName = "MemberSearchResult", field = "school")
    public Map<MemberSearchResultGraphQlResponse, MemberSchoolGraphQlResponse> schoolByMemberSearchResult(
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

        Map<MemberSearchResultGraphQlResponse, MemberSchoolGraphQlResponse> result = new LinkedHashMap<>();
        for (MemberSearchResultGraphQlResponse member : members) {
            SchoolDetailInfo school = member.schoolId() == null ? null : schoolsById.get(member.schoolId());
            result.put(member, school == null ? null : MemberSchoolGraphQlResponse.from(school));
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

    private Long currentMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("로그인이 필요해요. 로그인 후 다시 시도해주세요.");
        }
        if (authentication.getPrincipal() instanceof MemberPrincipal principal) {
            return principal.getMemberId();
        }
        throw new AccessDeniedException("인증 정보가 올바르지 않아요. 다시 로그인해주세요.");
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

    private void assertMembersVisible(List<MemberGraphQlResponse> members) {
        Long requesterMemberId = currentMemberId();
        List<Long> memberIds = members.stream()
            .map(MemberGraphQlResponse::memberId)
            .filter(Objects::nonNull)
            .filter(memberId -> !memberId.equals(requesterMemberId))
            .collect(Collectors.collectingAndThen(
                Collectors.toCollection(LinkedHashSet::new),
                List::copyOf
            ));
        if (memberIds.isEmpty()) {
            return;
        }

        SubjectAttributes subject = checkPermissionUseCase.loadSubject(requesterMemberId);
        memberIds.forEach(memberId -> assertMemberRead(subject, memberId));
    }

    private ResourcePermission memberReadPermission(Long memberId) {
        return ResourcePermission.of(ResourceType.MEMBER, memberId, PermissionType.READ);
    }
}
