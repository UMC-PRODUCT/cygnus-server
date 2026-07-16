package com.umc.product.recruiting.adapter.in.web.dto.request;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.command.dto.CreateAnonymousRecruitingApplicationDraftCommand;
import com.umc.product.recruiting.domain.RecruitingApplicantEmail;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "익명 지원서 초안 생성 요청")
public record CreateAnonymousRecruitingApplicationDraftRequest(
    @Schema(description = "리크루팅 지원 폼 ID", example = "10") @NotNull @Positive Long applicationFormId,
    @Schema(description = "지원자 이름", example = "홍길동", maxLength = 100)
    @NotBlank @Pattern(regexp = "\\S+") @Size(max = 100) String applicantName,
    @Schema(description = "지원자 이메일", example = "applicant@example.org", maxLength = 254)
    @NotBlank @Email @Size(max = 254) String applicantEmail,
    @Schema(description = "1지망 모집 트랙", example = "PLAN") @NotNull ChallengerTrack firstChoice,
    @Schema(description = "2지망 모집 트랙", example = "DESIGN") ChallengerTrack secondChoice,
    @Schema(description = "동의한 개인정보 처리방침 약관 ID", example = "3") @NotNull @Positive Long privacyTermId,
    @Schema(description = "개인정보 처리방침 동의 여부", example = "true") @AssertTrue boolean privacyAgreed
) {

    public CreateAnonymousRecruitingApplicationDraftRequest {
        if (applicantEmail != null && !applicantEmail.isBlank()) {
            applicantEmail = RecruitingApplicantEmail.from(applicantEmail).value();
        }
    }

    public CreateAnonymousRecruitingApplicationDraftCommand toCommand() {
        return CreateAnonymousRecruitingApplicationDraftCommand.builder()
            .applicationFormId(applicationFormId)
            .applicantName(applicantName)
            .applicantEmail(applicantEmail)
            .firstChoice(firstChoice)
            .secondChoice(secondChoice)
            .privacyTermId(privacyTermId)
            .privacyAgreed(privacyAgreed)
            .build();
    }
}
