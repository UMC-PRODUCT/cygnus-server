package com.umc.product.organization.adapter.in.web;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.authorization.adapter.in.aspect.CheckAccess;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.organization.adapter.in.web.dto.request.AssignSchoolRequest;
import com.umc.product.organization.adapter.in.web.dto.request.CreateSchoolRequest;
import com.umc.product.organization.adapter.in.web.dto.request.DeleteSchoolsRequest;
import com.umc.product.organization.adapter.in.web.dto.request.UnassignSchoolRequest;
import com.umc.product.organization.adapter.in.web.dto.request.UpdateSchoolRequest;
import com.umc.product.organization.adapter.in.web.swagger.AdminSchoolControllerApi;
import com.umc.product.organization.application.port.in.command.ManageSchoolUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/schools")
@RequiredArgsConstructor
public class SchoolCommandController implements AdminSchoolControllerApi {

    private final ManageSchoolUseCase manageSchoolUseCase;

    @CheckAccess(
        resourceType = ResourceType.SCHOOL,
        permission = PermissionType.WRITE,
        message = "학교를 만들 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요."
    )
    @Override
    @PostMapping
    public void createSchool(
        @RequestBody @Valid CreateSchoolRequest request,
        @CurrentMember MemberPrincipal memberPrincipal
    ) {
        manageSchoolUseCase.create(request.toCommand(currentMemberId(memberPrincipal)));
    }

    @CheckAccess(
        resourceType = ResourceType.SCHOOL,
        permission = PermissionType.EDIT,
        message = "학교를 수정할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요."
    )
    @Override
    @PatchMapping("/{schoolId}")
    public void updateSchool(
        @PathVariable Long schoolId,
        @RequestBody @Valid UpdateSchoolRequest request,
        @CurrentMember MemberPrincipal memberPrincipal
    ) {
        manageSchoolUseCase.updateSchool(schoolId, request.toCommand(currentMemberId(memberPrincipal)));
    }

    @CheckAccess(
        resourceType = ResourceType.SCHOOL,
        permission = PermissionType.DELETE,
        message = "학교를 삭제할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요."
    )
    @Override
    @DeleteMapping
    public void deleteSchools(@RequestBody @Valid DeleteSchoolsRequest request) {
        manageSchoolUseCase.deleteSchools(request.schoolIds());
    }

    @CheckAccess(
        resourceType = ResourceType.SCHOOL,
        permission = PermissionType.EDIT,
        message = "학교를 지부에 배정할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요."
    )
    @Override
    @PatchMapping("/{schoolId}/assign")
    public void assignToChapter(@PathVariable Long schoolId, @RequestBody @Valid AssignSchoolRequest request) {
        manageSchoolUseCase.assignToChapter(request.toCommand(schoolId));
    }

    @CheckAccess(
        resourceType = ResourceType.SCHOOL,
        permission = PermissionType.EDIT,
        message = "학교의 지부 배정을 해제할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요."
    )
    @Override
    @PatchMapping("/{schoolId}/unassign")
    public void unassignFromChapter(@PathVariable Long schoolId, @RequestBody @Valid UnassignSchoolRequest request) {
        manageSchoolUseCase.unassignFromChapter(request.toCommand(schoolId));
    }

    private Long currentMemberId(MemberPrincipal memberPrincipal) {
        if (memberPrincipal == null) {
            throw new AccessDeniedException("인증이 필요합니다.");
        }
        return memberPrincipal.getMemberId();
    }
}
