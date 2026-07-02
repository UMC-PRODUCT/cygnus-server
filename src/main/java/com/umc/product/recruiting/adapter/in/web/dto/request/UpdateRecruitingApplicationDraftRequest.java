package com.umc.product.recruiting.adapter.in.web.dto.request;

import java.util.List;

import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingApplicationDraftCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Schema(description = "지원서 초안 수정 요청")
public record UpdateRecruitingApplicationDraftRequest(
    @Schema(description = "지원서 답변 목록")
    @NotNull List<@Valid AnswerRequest> answers,
    @Schema(description = "요청자 회원 ID. 로그인 사용자는 세션의 회원 ID가 우선합니다.", example = "1001")
    Long requesterMemberId
) {

    public UpdateRecruitingApplicationDraftCommand toCommand(Long applicationId, Long resolvedRequesterMemberId) {
        return UpdateRecruitingApplicationDraftCommand.builder()
            .applicationId(applicationId)
            .requesterMemberId(resolvedRequesterMemberId)
            .answers(answers.stream()
                .map(AnswerRequest::toCommand)
                .toList())
            .build();
    }

    @Schema(description = "지원서 문항 답변")
    public record AnswerRequest(
        @Schema(description = "form 문항 ID", example = "7001")
        @NotNull Long questionId,
        @Schema(description = "단답형 또는 서술형 답변", example = "UMC 활동을 통해 서비스 개발 역량을 키우고 싶습니다.")
        String textValue,
        @Schema(description = "선택형 문항에서 선택한 option ID 목록", example = "[8001,8002]")
        List<Long> selectedOptionIds,
        @Schema(description = "파일 업로드 문항의 파일 ID 목록", example = "[\"file-1\",\"file-2\"]")
        List<String> fileIds
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
