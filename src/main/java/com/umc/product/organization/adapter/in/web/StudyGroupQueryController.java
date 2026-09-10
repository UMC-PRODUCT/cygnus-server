package com.umc.product.organization.adapter.in.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.authorization.adapter.in.aspect.CheckAccess;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.global.response.CursorResponse;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.organization.adapter.in.web.dto.response.studygroup.StudyGroupNameResponse;
import com.umc.product.organization.adapter.in.web.dto.response.studygroup.StudyGroupResponse;
import com.umc.product.organization.adapter.in.web.swagger.StudyGroupQueryControllerApi;
import com.umc.product.organization.application.port.in.query.GetStudyGroupUseCase;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupWithMemberAndMentorInfo;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/study-groups")
@RequiredArgsConstructor
public class StudyGroupQueryController implements StudyGroupQueryControllerApi {

    private final GetStudyGroupUseCase getStudyGroupUseCase;

    /**
     * 사용자의 schoolId/part 기반으로, 내가 관리할 수 있는 스터디 그룹의 목록을 반환
     */
    @Override
    @GetMapping("/managed")
    public CursorResponse<StudyGroupResponse> getStudyGroups(
        @CurrentMember MemberPrincipal memberPrincipal,
        @RequestParam(required = false) Long cursor,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) Long gisuId
    ) {

        List<StudyGroupWithMemberAndMentorInfo> content = gisuId == null
            ? getStudyGroupUseCase.getMyStudyGroups(memberPrincipal.getMemberId(), cursor, size)
            : getStudyGroupUseCase.getMyStudyGroups(memberPrincipal.getMemberId(), cursor, size, gisuId);

        return CursorResponse.of(
            content,
            size,
            StudyGroupWithMemberAndMentorInfo::groupId,
            StudyGroupResponse::from
        );
    }

    /**
     * 내가 관리할 수 있는 스터디 그룹의 이름 목록 조회 (드롭다운 등 목록 전체가 필요한 화면용)
     */
    @Override
    @GetMapping("/names")
    public StudyGroupNameResponse getStudyGroupNames(
        @CurrentMember MemberPrincipal memberPrincipal,
        @RequestParam(required = false) Long gisuId
    ) {
        return StudyGroupNameResponse.from(
            gisuId == null ? getStudyGroupUseCase.getStudyGroupNames(memberPrincipal.getMemberId())
                : getStudyGroupUseCase.getStudyGroupNames(memberPrincipal.getMemberId(), gisuId)
        );
    }

    public CursorResponse<StudyGroupResponse> getStudyGroups(MemberPrincipal principal, Long cursor, int size) {
        return getStudyGroups(principal, cursor, size, null);
    }

    public StudyGroupNameResponse getStudyGroupNames(MemberPrincipal principal) {
        return getStudyGroupNames(principal, null);
    }

    /**
     * 스터디 그룹 스터디원 목록 조회
     */
    @CheckAccess(
        resourceType = ResourceType.STUDY_GROUP,
        permission = PermissionType.READ
    )
    @Override
    @GetMapping("/{studyGroupId}")
    public StudyGroupResponse getStudyGroupInfo(@PathVariable Long studyGroupId) {
        return StudyGroupResponse.from(
            getStudyGroupUseCase.getWithMemberAndMentorInfoById(studyGroupId)
        );
    }
}
