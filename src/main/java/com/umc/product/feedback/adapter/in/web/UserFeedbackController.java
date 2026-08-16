package com.umc.product.feedback.adapter.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.feedback.adapter.in.web.dto.request.SubmitUserFeedbackResponseRequest;
import com.umc.product.feedback.adapter.in.web.dto.response.GetUserFeedbackTemplateResponse;
import com.umc.product.feedback.adapter.in.web.dto.response.UserFeedbackSubmitResponse;
import com.umc.product.feedback.application.port.in.command.SubmitUserFeedbackResponseUseCase;
import com.umc.product.feedback.application.port.in.query.GetUserFeedbackTemplateUseCase;
import com.umc.product.feedback.domain.enums.UserFeedbackContext;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/user-feedbacks")
@RequiredArgsConstructor
@Tag(name = "UserFeedback | 사용자 조사", description = "사용자 피드백 템플릿 조회 및 응답 제출")
public class UserFeedbackController {

    private final GetUserFeedbackTemplateUseCase getUserFeedbackTemplateUseCase;
    private final SubmitUserFeedbackResponseUseCase submitUserFeedbackResponseUseCase;

    @GetMapping("/templates")
    @Operation(
        summary = "사용자 피드백 템플릿 조회",
        description = """
            요청자의 챌린저 이력 및 운영진 역할을 기반으로 TargetType을 자동 판별하여,
            해당 context에 맞는 활성 피드백 템플릿(Form 폼 전체 구조 포함)을 반환합니다.
            활성 기수가 없거나 해당 context + targetType 조합의 템플릿이 없으면 result = null.

            [조건부 렌더링 - ADMIN / APPLICATION_MONITORING 템플릿]
            현재 Form 엔진은 조건부 렌더링을 지원하지 않으므로, 프론트엔드에서 직접 처리해야 합니다.
            questions 및 options 배열은 orderNo 기준 오름차순으로 정렬되어 내려오며, 조건에 따라 숨길 질문도 포함되어 있습니다.
            (예: '필요한 정보를 찾는 데 어려움이 있었나요?' 질문에서 '조금 있었어요' 또는 '많이 있었어요' 선택 시 자유 서술 텍스트 박스 표시)
            """
    )
    public GetUserFeedbackTemplateResponse getTemplate(
        @CurrentMember MemberPrincipal memberPrincipal,
        @RequestParam UserFeedbackContext context
    ) {
        return getUserFeedbackTemplateUseCase
            .findTemplate(memberPrincipal.getMemberId(), context)
            .map(GetUserFeedbackTemplateResponse::from)
            .orElse(null);
    }

    @PostMapping("/responses")
    @Operation(
        summary = "사용자 피드백 응답 제출",
        description = """
            지정한 피드백 템플릿에 대한 응답을 즉시 제출합니다.
            동일 폼에 이미 응답한 경우 Form 도메인에서 중복 응답 예외가 발생합니다.
            """
    )
    public UserFeedbackSubmitResponse submit(
        @CurrentMember MemberPrincipal memberPrincipal,
        @Valid @RequestBody SubmitUserFeedbackResponseRequest request
    ) {
        Long formResponseId = submitUserFeedbackResponseUseCase.submit(
            request.toCommand(memberPrincipal.getMemberId())
        );
        return UserFeedbackSubmitResponse.from(formResponseId);
    }
}
