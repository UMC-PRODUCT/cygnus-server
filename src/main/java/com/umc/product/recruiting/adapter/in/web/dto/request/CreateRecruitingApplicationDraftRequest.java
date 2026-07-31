package com.umc.product.recruiting.adapter.in.web.dto.request;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingApplicationDraftCommand;
import com.umc.product.recruiting.domain.RecruitingApplicantEmail;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "지원서 초안 생성 요청")
public record CreateRecruitingApplicationDraftRequest(
    @Schema(description = "리크루팅 지원 폼 연결 ID", example = "10")
    @NotNull @Positive Long applicationFormId,
    @Schema(description = "지원자 이름", example = "홍길동", maxLength = 100)
    @NotBlank @Pattern(regexp = "\\S+") @Size(max = 100) String applicantName,
    @Schema(description = "지원자 이메일", example = "applicant@example.org", maxLength = 254)
    @NotBlank @Email @Size(max = 254) String applicantEmail,
    @Schema(description = "1지망 모집 트랙", example = "PLAN") @NotNull ChallengerTrack firstChoice,
    @Schema(description = "2지망 모집 트랙", example = "DESIGN") ChallengerTrack secondChoice
) {

    public CreateRecruitingApplicationDraftRequest {
        if (applicantEmail != null && !applicantEmail.isBlank()) {
            applicantEmail = RecruitingApplicantEmail.from(applicantEmail).value();
        }
    }

    public CreateRecruitingApplicationDraftCommand toCommand(Long applicantMemberId) {
        return CreateRecruitingApplicationDraftCommand.builder()
            .applicationFormId(applicationFormId)
            .applicantMemberId(applicantMemberId)
            .applicantName(applicantName)
            .applicantEmail(applicantEmail)
            .firstChoice(firstChoice)
            .secondChoice(secondChoice)
            .build();
    }
}
