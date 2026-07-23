package com.umc.product.recruiting.adapter.in.web.dto.request;

import com.umc.product.recruiting.application.port.in.command.dto.SubmitAnonymousRecruitingApplicationCommand;
import com.umc.product.recruiting.domain.RecruitingApplicantEmail;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "익명 지원서 제출 요청")
public record SubmitAnonymousRecruitingApplicationRequest(
    @Schema(description = "현재 지원서에 저장된 이메일", example = "applicant@example.org", maxLength = 254)
    @NotBlank @Email @Size(max = 254) String email,
    @Schema(description = "지원서 생성 시 발급된 6자리 키", example = "A1B2C3")
    @NotBlank @Pattern(regexp = "[A-Z0-9]{6}") String applicationKey
) {

    public SubmitAnonymousRecruitingApplicationRequest {
        if (email != null && !email.isBlank()) {
            email = RecruitingApplicantEmail.from(email).value();
        }
    }

    public SubmitAnonymousRecruitingApplicationCommand toCommand(String submittedIp) {
        return SubmitAnonymousRecruitingApplicationCommand.builder()
            .credentialEmail(email)
            .applicationKey(applicationKey)
            .submittedIp(submittedIp)
            .build();
    }
}
