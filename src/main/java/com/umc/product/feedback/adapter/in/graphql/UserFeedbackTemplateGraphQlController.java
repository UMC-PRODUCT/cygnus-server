package com.umc.product.feedback.adapter.in.graphql;

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.feedback.adapter.in.graphql.dto.UserFeedbackTemplateGraphQlResponse;
import com.umc.product.feedback.adapter.in.graphql.dto.UserFeedbackTemplateSearchGraphQlRequest;
import com.umc.product.feedback.adapter.in.graphql.dto.UserFeedbackTemplateSummaryGraphQlResponse;
import com.umc.product.feedback.application.port.in.query.GetUserFeedbackTemplateAdminUseCase;
import com.umc.product.global.security.MemberPrincipal;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class UserFeedbackTemplateGraphQlController {

    private final GetUserFeedbackTemplateAdminUseCase getUserFeedbackTemplateAdminUseCase;
    private final CheckPermissionUseCase checkPermissionUseCase;

    @QueryMapping
    public List<UserFeedbackTemplateSummaryGraphQlResponse> userFeedbackTemplates(
        @Argument UserFeedbackTemplateSearchGraphQlRequest input
    ) {
        Long memberId = currentMemberId();
        checkPermissionUseCase.checkOrThrow(memberId, feedbackReadPermission());

        if (input == null) {
            return getUserFeedbackTemplateAdminUseCase.listTemplates(null, null, null).stream()
                .map(UserFeedbackTemplateSummaryGraphQlResponse::from)
                .toList();
        }

        return getUserFeedbackTemplateAdminUseCase.listTemplates(input.context(), input.targetType(), input.active())
            .stream()
            .map(UserFeedbackTemplateSummaryGraphQlResponse::from)
            .toList();
    }

    @QueryMapping
    public UserFeedbackTemplateGraphQlResponse userFeedbackTemplate(@Argument Long id) {
        Long memberId = currentMemberId();
        checkPermissionUseCase.checkOrThrow(memberId, feedbackReadPermission());
        return UserFeedbackTemplateGraphQlResponse.from(getUserFeedbackTemplateAdminUseCase.getTemplate(id));
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

    private ResourcePermission feedbackReadPermission() {
        return ResourcePermission.ofType(ResourceType.FEEDBACK, PermissionType.READ);
    }
}
