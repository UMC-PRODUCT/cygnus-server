package com.umc.product.feedback.adapter.in.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.authorization.adapter.in.aspect.CheckAccess;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.feedback.adapter.in.web.dto.request.CreateUserFeedbackTemplateRequest;
import com.umc.product.feedback.adapter.in.web.dto.request.UpdateUserFeedbackTemplateRequest;
import com.umc.product.feedback.adapter.in.web.dto.response.AdminUserFeedbackTemplateResponse;
import com.umc.product.feedback.adapter.in.web.dto.response.AdminUserFeedbackTemplateSummaryResponse;
import com.umc.product.feedback.application.port.in.command.ManageUserFeedbackTemplateUseCase;
import com.umc.product.feedback.application.port.in.query.GetUserFeedbackTemplateAdminUseCase;
import com.umc.product.feedback.domain.enums.UserFeedbackContext;
import com.umc.product.feedback.domain.enums.UserFeedbackTargetType;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/user-feedbacks/templates")
@RequiredArgsConstructor
@Tag(name = "UserFeedback | 피드백 양식 관리", description = "SUPER_ADMIN이 사용자 피드백 양식을 관리합니다.")
public class AdminUserFeedbackTemplateController {

    private final GetUserFeedbackTemplateAdminUseCase getTemplateAdminUseCase;
    private final ManageUserFeedbackTemplateUseCase manageTemplateUseCase;

    @GetMapping
    @Operation(summary = "피드백 양식 목록 조회")
    @CheckAccess(resourceType = ResourceType.FEEDBACK, permission = PermissionType.READ)
    public List<AdminUserFeedbackTemplateSummaryResponse> listTemplates(
        @RequestParam(required = false) UserFeedbackContext context,
        @RequestParam(required = false) UserFeedbackTargetType targetType,
        @RequestParam(required = false) Boolean active
    ) {
        return getTemplateAdminUseCase.listTemplates(context, targetType, active).stream()
            .map(AdminUserFeedbackTemplateSummaryResponse::from)
            .toList();
    }

    @GetMapping("/{templateId}")
    @Operation(summary = "피드백 양식 상세 조회")
    @CheckAccess(resourceType = ResourceType.FEEDBACK, permission = PermissionType.READ)
    public AdminUserFeedbackTemplateResponse getTemplate(@PathVariable Long templateId) {
        return AdminUserFeedbackTemplateResponse.from(getTemplateAdminUseCase.getTemplate(templateId));
    }

    @PostMapping
    @Operation(summary = "피드백 양식 생성")
    @CheckAccess(resourceType = ResourceType.FEEDBACK, permission = PermissionType.MANAGE)
    public AdminUserFeedbackTemplateResponse createTemplate(
        @CurrentMember MemberPrincipal memberPrincipal,
        @Valid @RequestBody CreateUserFeedbackTemplateRequest request
    ) {
        return AdminUserFeedbackTemplateResponse.from(
            manageTemplateUseCase.create(request.toCommand(memberPrincipal.getMemberId()))
        );
    }

    @PutMapping("/{templateId}")
    @Operation(summary = "피드백 양식 수정")
    @CheckAccess(resourceType = ResourceType.FEEDBACK, permission = PermissionType.MANAGE)
    public AdminUserFeedbackTemplateResponse updateTemplate(
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable Long templateId,
        @Valid @RequestBody UpdateUserFeedbackTemplateRequest request
    ) {
        return AdminUserFeedbackTemplateResponse.from(
            manageTemplateUseCase.update(request.toCommand(templateId, memberPrincipal.getMemberId()))
        );
    }

    @DeleteMapping("/{templateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "피드백 양식 비활성화")
    @CheckAccess(resourceType = ResourceType.FEEDBACK, permission = PermissionType.MANAGE)
    public void deleteTemplate(@PathVariable Long templateId) {
        manageTemplateUseCase.delete(templateId);
    }
}
