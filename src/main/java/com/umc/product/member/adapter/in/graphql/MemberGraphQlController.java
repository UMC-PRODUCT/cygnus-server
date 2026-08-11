package com.umc.product.member.adapter.in.graphql;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import com.umc.product.global.graphql.relay.ConnectionArguments;
import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.global.graphql.relay.OffsetPageRequest;
import com.umc.product.global.graphql.relay.RelayConnection;
import com.umc.product.global.security.CurrentMemberProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.member.adapter.in.graphql.dto.MemberChallengerGraphQlResponse;
import com.umc.product.member.adapter.in.graphql.dto.MemberFilterGraphQlRequest;
import com.umc.product.member.adapter.in.graphql.dto.MemberGraphQlResponse;
import com.umc.product.member.adapter.in.graphql.dto.MemberSearchChallengerGraphQlResponse;
import com.umc.product.member.adapter.in.graphql.dto.MemberSearchResultGraphQlResponse;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.SearchMemberUseCase;
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
    public MemberGraphQlResponse me(@Nullable @CurrentMember MemberPrincipal memberPrincipal) {
        Long requesterMemberId = currentMemberId(memberPrincipal);
        return MemberGraphQlResponse.privateFrom(getMemberUseCase.getById(requesterMemberId));
    }

    @QueryMapping
    public MemberGraphQlResponse member(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument String id
    ) {
        Long requesterMemberId = currentMemberId(memberPrincipal);
        Long memberId = GlobalId.decodeLong(id, GlobalIdTypes.MEMBER);
        checkPermissionUseCase.checkOrThrow(requesterMemberId, memberReadPermission(memberId));
        if (requesterMemberId.equals(memberId)) {
            return MemberGraphQlResponse.privateFrom(getMemberUseCase.getById(memberId));
        }
        return MemberGraphQlResponse.publicFrom(getMemberUseCase.getById(memberId));
    }

    @QueryMapping
    public RelayConnection<MemberSearchResultGraphQlResponse> members(
        @Argument MemberFilterGraphQlRequest filter,
        @Argument Integer first,
        @Argument String after,
        @Argument Integer last,
        @Argument String before
    ) {
        Long requesterMemberId = currentMemberId();
        ConnectionArguments arguments = ConnectionArguments.of(first, after, last, before);
        MemberFilterGraphQlRequest effectiveFilter = filter == null ? MemberFilterGraphQlRequest.empty() : filter;
        var pageable = arguments.toPageable(() -> searchMemberUseCase
            .searchByV2ForGraphQl(
                effectiveFilter.toQuery(),
                requesterMemberId,
                new OffsetPageRequest(0, 1)
            )
            .page()
            .getTotalElements());
        return RelayConnection.fromPage(
            searchMemberUseCase
                .searchByV2ForGraphQl(effectiveFilter.toQuery(), requesterMemberId, pageable)
                .page(),
            arguments,
            MemberSearchResultGraphQlResponse::from
        );
    }

    @BatchMapping(typeName = "Member", field = "school")
    public Map<MemberGraphQlResponse, SchoolGraphQlResponse> schoolByMember(
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

        Map<MemberGraphQlResponse, SchoolGraphQlResponse> result = new LinkedHashMap<>();
        for (MemberGraphQlResponse member : members) {
            SchoolDetailInfo school = schoolsById.get(member.schoolId());
            result.put(member, school == null ? null : SchoolGraphQlResponse.from(school));
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
    public Map<MemberChallengerGraphQlResponse, GisuGraphQlResponse> gisuByMemberChallenger(
        List<MemberChallengerGraphQlResponse> challengers
    ) {
        Set<Long> gisuIds = challengers.stream()
            .map(MemberChallengerGraphQlResponse::gisuId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, GisuInfo> gisusById = gisusById(gisuIds);

        Map<MemberChallengerGraphQlResponse, GisuGraphQlResponse> result = new LinkedHashMap<>();
        for (MemberChallengerGraphQlResponse challenger : challengers) {
            GisuInfo gisu = challenger.gisuId() == null ? null : gisusById.get(challenger.gisuId());
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
        Map<Long, GisuInfo> gisusById = gisusById(gisuIds);

        Map<MemberSearchChallengerGraphQlResponse, GisuGraphQlResponse> result = new LinkedHashMap<>();
        for (MemberSearchChallengerGraphQlResponse challenger : challengers) {
            GisuInfo gisu = challenger.gisuId() == null ? null : gisusById.get(challenger.gisuId());
            result.put(challenger, gisu == null ? null : GisuGraphQlResponse.from(gisu));
        }
        return result;
    }

    private Map<Long, GisuInfo> gisusById(Set<Long> gisuIds) {
        return gisuIds.isEmpty()
            ? Map.of()
            : getGisuUseCase.getByIds(gisuIds).stream()
                .collect(Collectors.toMap(
                    GisuInfo::gisuId,
                    Function.identity(),
                    (left, right) -> left
                ));
    }

    private Long currentMemberId(MemberPrincipal memberPrincipal) {
        return memberPrincipal == null
            ? currentMemberProvider.getRequiredCurrentMemberId()
            : memberPrincipal.getMemberId();
    }

    private Long currentMemberId() {
        return currentMemberProvider.getRequiredCurrentMemberId();
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
