package com.umc.product.organization.adapter.in.graphql;

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
import org.springframework.stereotype.Controller;

import com.umc.product.authorization.adapter.in.aspect.CheckAccess;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.global.graphql.dto.PageGraphQlRequest;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.member.application.port.in.query.dto.MemberPublicInfo;
import com.umc.product.organization.adapter.in.graphql.dto.GisuGraphQlResponse;
import com.umc.product.organization.adapter.in.graphql.dto.UmcProductMemberFilterGraphQlRequest;
import com.umc.product.organization.adapter.in.graphql.dto.UmcProductMemberPageGraphQlResponse;
import com.umc.product.organization.adapter.in.graphql.dto.UmcProductSquadFilterGraphQlRequest;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.GetStudyGroupUseCase;
import com.umc.product.organization.application.port.in.query.GetUmcProductMemberUseCase;
import com.umc.product.organization.application.port.in.query.GetUmcProductOrganizationChartUseCase;
import com.umc.product.organization.application.port.in.query.GetUmcProductSquadUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupMemberInfo;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupWithMemberAndMentorInfo;
import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductMemberInfo;
import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductOrganizationChartInfo;
import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductSquadInfo;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class OrganizationResourceGraphQlController {

    private final GetStudyGroupUseCase getStudyGroupUseCase;
    private final GetUmcProductOrganizationChartUseCase getOrganizationChartUseCase;
    private final GetUmcProductMemberUseCase getUmcProductMemberUseCase;
    private final GetUmcProductSquadUseCase getUmcProductSquadUseCase;
    private final GetMemberUseCase getMemberUseCase;
    private final GetGisuUseCase getGisuUseCase;

    @QueryMapping
    public List<StudyGroupWithMemberAndMentorInfo> managedStudyGroups(
        @CurrentMember MemberPrincipal principal,
        @Argument Long cursor,
        @Argument Integer size
    ) {
        return getStudyGroupUseCase.getMyStudyGroups(
            principal.getMemberId(),
            cursor,
            size == null ? 20 : size
        );
    }

    @QueryMapping
    @CheckAccess(
        resourceType = ResourceType.STUDY_GROUP,
        permission = PermissionType.READ,
        resourceId = "#id"
    )
    public StudyGroupWithMemberAndMentorInfo studyGroup(@Argument Long id) {
        return getStudyGroupUseCase.getWithMemberAndMentorInfoById(id);
    }

    @QueryMapping
    public UmcProductOrganizationChartInfo umcProductOrganizationChart() {
        return getOrganizationChartUseCase.getCurrent();
    }

    @QueryMapping
    public UmcProductMemberPageGraphQlResponse umcProductMembers(
        @Argument UmcProductMemberFilterGraphQlRequest filter,
        @Argument PageGraphQlRequest page
    ) {
        return UmcProductMemberPageGraphQlResponse.from(
            getUmcProductMemberUseCase.search(
                UmcProductMemberFilterGraphQlRequest.toCondition(filter),
                PageGraphQlRequest.defaultIfNull(page).toPageableWithMaxOffset(10_000L)
            )
        );
    }

    @QueryMapping
    public UmcProductMemberInfo umcProductMember(@Argument Long id) {
        return getUmcProductMemberUseCase.getById(id);
    }

    @QueryMapping
    public List<UmcProductSquadInfo> umcProductSquads(
        @Argument UmcProductSquadFilterGraphQlRequest filter
    ) {
        return filter == null
            ? getUmcProductSquadUseCase.list(null, null)
            : getUmcProductSquadUseCase.list(filter.active(), filter.activeOn());
    }

    @BatchMapping(typeName = "StudyGroup", field = "gisu")
    public Map<StudyGroupWithMemberAndMentorInfo, GisuGraphQlResponse> gisuByStudyGroup(
        List<StudyGroupWithMemberAndMentorInfo> groups
    ) {
        Set<Long> gisuIds = groups.stream()
            .map(StudyGroupWithMemberAndMentorInfo::gisuId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, GisuInfo> gisusById = getGisuUseCase.getByIds(gisuIds).stream()
            .collect(Collectors.toMap(GisuInfo::gisuId, Function.identity()));

        return groups.stream().collect(Collectors.toMap(
            Function.identity(),
            group -> GisuGraphQlResponse.from(gisusById.get(group.gisuId())),
            (left, right) -> left,
            LinkedHashMap::new
        ));
    }

    @BatchMapping(typeName = "StudyGroup", field = "mentors")
    public Map<StudyGroupWithMemberAndMentorInfo, List<MemberPublicInfo>> mentorsByStudyGroup(
        List<StudyGroupWithMemberAndMentorInfo> groups
    ) {
        return membersByStudyGroup(groups, true);
    }

    @BatchMapping(typeName = "StudyGroup", field = "members")
    public Map<StudyGroupWithMemberAndMentorInfo, List<MemberPublicInfo>> membersByStudyGroup(
        List<StudyGroupWithMemberAndMentorInfo> groups
    ) {
        return membersByStudyGroup(groups, false);
    }

    @BatchMapping(typeName = "UmcProductMember", field = "member")
    public Map<UmcProductMemberInfo, MemberPublicInfo> memberByUmcProductMember(
        List<UmcProductMemberInfo> members
    ) {
        Map<Long, MemberInfo> membersById = getMemberUseCase.findAllByIds(
            members.stream()
                .map(UmcProductMemberInfo::memberId)
                .collect(Collectors.toCollection(LinkedHashSet::new))
        );
        return members.stream().collect(Collectors.toMap(
            Function.identity(),
            member -> MemberPublicInfo.from(membersById.get(member.memberId())),
            (left, right) -> left,
            LinkedHashMap::new
        ));
    }

    private Map<StudyGroupWithMemberAndMentorInfo, List<MemberPublicInfo>> membersByStudyGroup(
        List<StudyGroupWithMemberAndMentorInfo> groups,
        boolean mentors
    ) {
        Set<Long> memberIds = groups.stream()
            .flatMap(group -> studyGroupMembers(group, mentors).stream())
            .map(StudyGroupMemberInfo::memberId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, MemberInfo> membersById = getMemberUseCase.findAllByIds(memberIds);

        return groups.stream().collect(Collectors.toMap(
            Function.identity(),
            group -> studyGroupMembers(group, mentors).stream()
                .map(StudyGroupMemberInfo::memberId)
                .map(membersById::get)
                .filter(Objects::nonNull)
                .map(MemberPublicInfo::from)
                .toList(),
            (left, right) -> left,
            LinkedHashMap::new
        ));
    }

    private List<StudyGroupMemberInfo> studyGroupMembers(
        StudyGroupWithMemberAndMentorInfo group,
        boolean mentors
    ) {
        return mentors ? group.mentors() : group.members();
    }
}
