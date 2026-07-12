package com.umc.product.recruiting.adapter.in.web;

import org.springframework.validation.annotation.Validated;
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
import com.umc.product.recruiting.adapter.in.web.dto.request.RecruitingFormSectionPolicyRequest;
import com.umc.product.recruiting.adapter.in.web.dto.response.RecruitingIdResponse;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingFormSectionPolicyUseCase;
import com.umc.product.recruiting.application.port.in.query.ValidateRecruitingFormScopeUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/recruiting/admin/seasons/{seasonId}/forms/{applicationFormId}/section-policies")
@Validated
@Tag(name = "Recruiting | 지원 폼 정책", description = "운영진이 지원 폼의 공통 및 트랙별 섹션 정책을 관리합니다.")
@RequiredArgsConstructor
public class RecruitingAdminFormPolicyController {

    private final ManageRecruitingFormSectionPolicyUseCase manageFormSectionPolicyUseCase;
    private final ValidateRecruitingFormScopeUseCase validateFormScopeUseCase;

    @PostMapping
    @CheckAccess(resourceType = ResourceType.RECRUITMENT, resourceId = "#seasonId", permission = PermissionType.EDIT)
    @Operation(
        operationId = "RECRUITING-ADMIN-FORM-POLICY-001",
        summary = "지원 폼 섹션 정책 추가",
        description = "Survey 폼 섹션을 공통 또는 단일 모집 트랙 전용 섹션으로 연결합니다."
    )
    public RecruitingIdResponse addPolicy(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal actor,
        @PathVariable @Positive Long seasonId,
        @PathVariable @Positive Long applicationFormId,
        @Valid @RequestBody RecruitingFormSectionPolicyRequest request
    ) {
        validateFormScopeUseCase.validateSeasonScope(applicationFormId, seasonId);
        return RecruitingIdResponse.from(manageFormSectionPolicyUseCase.addPolicy(
            request.toCommand(applicationFormId)
        ));
    }
}
