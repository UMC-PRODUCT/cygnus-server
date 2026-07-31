package com.umc.product.recruiting.adapter.in.web.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingPublicApplicationInfo;
import com.umc.product.recruiting.domain.enums.RecruitingPublicResultStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "익명 지원자가 자격 증명으로 조회한 지원서")
public record RecruitingPublicApplicationResponse(
    @Schema(description = "지원서 ID", example = "100") Long applicationId,
    @Schema(description = "지원자 이름", example = "홍길동") String applicantName,
    @Schema(description = "정규화된 지원자 이메일", example = "applicant@example.org") String applicantEmail,
    @Schema(description = "1지망 모집 트랙", example = "PLAN") ChallengerTrack firstChoice,
    @Schema(description = "2지망 모집 트랙", example = "DESIGN") ChallengerTrack secondChoice,
    @Schema(description = "제출 완료 여부") boolean submitted,
    @Schema(description = "지원 취소 여부") boolean cancelled,
    @Schema(description = "현재 수정 가능 여부") boolean editable,
    @Schema(description = "서류 결과. 발표 전에는 PENDING") RecruitingPublicResultStatus documentResult,
    @Schema(description = "최종 결과. 발표 전에는 PENDING") RecruitingPublicResultStatus finalResult,
    @Schema(description = "최종 합격 트랙. 최종 발표 후 합격한 경우에만 제공") ChallengerTrack acceptedTrack,
    @Schema(description = "Form 답변 목록") List<AnswerResponse> answers
) {

    public static RecruitingPublicApplicationResponse from(RecruitingPublicApplicationInfo info) {
        return new RecruitingPublicApplicationResponse(
            info.applicationId(),
            info.applicantName(),
            info.applicantEmail(),
            info.firstChoice(),
            info.secondChoice(),
            info.submitted(),
            info.cancelled(),
            info.editable(),
            info.documentResult(),
            info.finalResult(),
            info.acceptedTrack(),
            info.answers().stream().map(AnswerResponse::from).toList()
        );
    }

    @Schema(description = "Form 질문 답변")
    public record AnswerResponse(
        @Schema(description = "Form 질문 ID") Long questionId,
        @Schema(description = "텍스트 답변") String textValue,
        @Schema(description = "선택한 Form 옵션 ID 목록") List<Long> selectedOptionIds,
        @Schema(description = "첨부 파일 ID 목록") Set<String> fileIds,
        @Schema(description = "선택한 일정 목록") Set<Instant> times
    ) {

        private static AnswerResponse from(RecruitingPublicApplicationInfo.Answer answer) {
            return new AnswerResponse(
                answer.questionId(),
                answer.textValue(),
                answer.selectedOptionIds(),
                answer.fileIds(),
                answer.times()
            );
        }
    }
}
