package com.umc.product.recruiting.adapter.in.web.dto.request;

import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingApplicationDraftCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "지원서 초안 생성 요청")
public record CreateRecruitingApplicationDraftRequest(
    @Schema(description = "리크루팅 도메인에 연결된 지원 폼 ID", example = "30")
    @NotNull Long applicationFormId,
    @Schema(description = "지원자 회원 ID. 로그인 사용자는 세션의 회원 ID가 우선하고 익명 지원자는 null입니다.", example = "1001")
    Long applicantMemberId,
    @Schema(description = "익명 지원자 또는 form 제출을 식별하는 키", example = "identity-key")
    @NotBlank String applicantIdentityKey,
    @Schema(description = "지원자에게 표시할 마스킹 이메일", example = "h***@example.com")
    String maskedEmail
) {

    public CreateRecruitingApplicationDraftCommand toCommand(Long resolvedApplicantMemberId) {
        return CreateRecruitingApplicationDraftCommand.builder()
            .applicationFormId(applicationFormId)
            .applicantMemberId(resolvedApplicantMemberId)
            .applicantIdentityKey(applicantIdentityKey)
            .maskedEmail(maskedEmail)
            .build();
    }
}
