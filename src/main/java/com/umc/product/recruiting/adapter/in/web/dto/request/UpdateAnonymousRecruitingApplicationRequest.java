package com.umc.product.recruiting.adapter.in.web.dto.request;

import java.util.List;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateAnonymousRecruitingApplicationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingApplicationDraftCommand;
import com.umc.product.recruiting.domain.RecruitingApplicantEmail;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "익명 지원서 수정 요청")
public record UpdateAnonymousRecruitingApplicationRequest(
    @Schema(description = "현재 지원서에 저장된 이메일", example = "applicant@example.org", maxLength = 254)
    @NotBlank @Email @Size(max = 254) String credentialEmail,
    @Schema(description = "지원서 생성 시 발급된 6자리 키", example = "A1B2C3")
    @NotBlank @Pattern(regexp = "[A-Z0-9]{6}") String applicationKey,
    @Schema(description = "수정할 지원자 이름", example = "홍길동", maxLength = 100)
    @NotBlank @Pattern(regexp = "\\S+") @Size(max = 100) String applicantName,
    @Schema(description = "수정할 지원자 이메일", example = "new-applicant@example.org", maxLength = 254)
    @NotBlank @Email @Size(max = 254) String applicantEmail,
    @Schema(description = "1지망 모집 트랙", example = "PLAN") @NotNull ChallengerTrack firstChoice,
    @Schema(description = "2지망 모집 트랙", example = "DESIGN") ChallengerTrack secondChoice,
    @Schema(description = "Form 질문별 답변 목록") @NotNull List<@Valid AnswerRequest> answers
) {

    public UpdateAnonymousRecruitingApplicationRequest {
        if (credentialEmail != null && !credentialEmail.isBlank()) {
            credentialEmail = RecruitingApplicantEmail.from(credentialEmail).value();
        }
        if (applicantEmail != null && !applicantEmail.isBlank()) {
            applicantEmail = RecruitingApplicantEmail.from(applicantEmail).value();
        }
    }

    public UpdateAnonymousRecruitingApplicationCommand toCommand() {
        return UpdateAnonymousRecruitingApplicationCommand.builder()
            .credentialEmail(credentialEmail)
            .applicationKey(applicationKey)
            .applicantName(applicantName)
            .applicantEmail(applicantEmail)
            .firstChoice(firstChoice)
            .secondChoice(secondChoice)
            .answers(answers.stream().map(AnswerRequest::toCommand).toList())
            .build();
    }

    @Schema(description = "Form 질문 답변")
    public record AnswerRequest(
        @Schema(description = "Form 질문 ID", example = "7") @NotNull @Positive Long questionId,
        @Schema(description = "텍스트 답변") String textValue,
        @Schema(description = "선택한 Form 옵션 ID 목록") List<@Positive Long> selectedOptionIds,
        @Schema(description = "첨부 파일 ID 목록") List<String> fileIds
    ) {

        private UpdateRecruitingApplicationDraftCommand.AnswerEntry toCommand() {
            return UpdateRecruitingApplicationDraftCommand.AnswerEntry.builder()
                .questionId(questionId)
                .textValue(textValue)
                .selectedOptionIds(selectedOptionIds)
                .fileIds(fileIds)
                .build();
        }
    }
}
