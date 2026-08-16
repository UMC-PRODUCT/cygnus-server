package com.umc.product.project.application.port.in.query.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.form.application.port.in.query.dto.AnswerInfo;
import com.umc.product.form.application.port.in.query.dto.FormResponseInfo;
import com.umc.product.form.application.port.in.query.dto.FormResponseWithAnswersInfo;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.project.domain.ProjectApplication;
import com.umc.product.project.domain.ProjectApplicationForm;
import com.umc.product.project.domain.ProjectApplicationFormPolicy;
import com.umc.product.project.domain.ProjectMatchingRound;
import com.umc.product.project.domain.enums.MatchingPhase;
import com.umc.product.project.domain.enums.MatchingType;
import com.umc.product.storage.application.port.in.query.dto.FileInfo;

import lombok.Builder;

/**
 * 지원서 단건 상세 Info DTO.
 * <p>
 * Service 가 cross-domain (project/challenger/form/storage) raw 데이터를 모아 넘기면, {@link #of} 가 지원자 파트 기준 폼 구조 /
 * FormResponse 메타 / questionId -> 답변 매핑까지 합성한다. 트리(섹션 -> 질문 -> 답변) 구조 합성은 Web Response 레이어가 책임진다 -- Info 는
 * 컨테이너 형태를 유지한다.
 *
 * @param applicationId       지원서 ID
 * @param applicantMemberId   지원자 Member ID
 * @param applicantPart       지원자(챌린저) 의 파트 — 폼 섹션 제한과 응답 표시에 모두 사용
 * @param matchingRoundId     매칭 차수 ID
 * @param matchingRoundType   매칭 종류
 * @param matchingRoundPhase  매칭 차수
 * @param status              표시용 상태 (DRAFT 포함). 지원자 본인 조회에서 제출한 매칭 차수의 decisionDeadline 전이면 제출 이후 상태는 {@code null}
 * @param submittedAt         지원시각 (DRAFT 이면 null)
 * @param statusChangedAt     처리시각 (합/불 결정 전이면 null)
 * @param formStructure       지원자 파트 기준 폼 구조 (COMMON + applicantPart 의 PART 섹션만 포함)
 * @param formResponse        FormResponse 메타 (제출 시각 / 마지막 저장 시각 등)
 * @param answersByQuestionId 답변이 있는 questionId 만 매핑됨 — 답변 없는 질문은 키 자체가 없음
 * @param filesByFileId       answer.fileIds 들의 메타+URL — 누락된 fileId 는 매핑에서 빠짐
 */
@Builder
public record ProjectApplicationDetailInfo(
    Long applicationId,
    Long applicantMemberId,
    ChallengerPart applicantPart,
    Long matchingRoundId,
    MatchingType matchingRoundType,
    MatchingPhase matchingRoundPhase,
    ProjectApplicationViewStatus status,
    Instant submittedAt,
    Instant statusChangedAt,
    ApplicationFormInfo formStructure,
    FormResponseInfo formResponse,
    Map<Long, AnswerInfo> answersByQuestionId,
    Map<String, FileInfo> filesByFileId
) {
    /**
     * Service 가 모은 raw 데이터를 합성해 컨테이너를 만든다.
     *
     * @param application             fetch 된 지원서 (applicationForm/project, appliedMatchingRound 로드 상태)
     * @param applicantPart           지원자 파트 (challenger 도메인 enrichment 결과)
     * @param formStructure           form 도메인의 폼 구조
     * @param formPolicies            project 도메인의 섹션 표시 정책
     * @param formResponseWithAnswers form 도메인의 응답 메타 + 답변
     * @param filesByFileId           storage 도메인의 fileId -> FileInfo 매핑 (batch 조회 결과)
     */
    public static ProjectApplicationDetailInfo of(
        ProjectApplication application,
        ChallengerPart applicantPart,
        FormWithStructureInfo formStructure,
        List<ProjectApplicationFormPolicy> formPolicies,
        FormResponseWithAnswersInfo formResponseWithAnswers,
        Map<String, FileInfo> filesByFileId
    ) {
        return of(
            application,
            applicantPart,
            formStructure,
            formPolicies,
            formResponseWithAnswers,
            filesByFileId,
            true
        );
    }

    public static ProjectApplicationDetailInfo of(
        ProjectApplication application,
        ChallengerPart applicantPart,
        FormWithStructureInfo formStructure,
        List<ProjectApplicationFormPolicy> formPolicies,
        FormResponseWithAnswersInfo formResponseWithAnswers,
        Map<String, FileInfo> filesByFileId,
        boolean statusVisible
    ) {
        ProjectApplicationForm applicationForm = application.getApplicationForm();
        ProjectMatchingRound round = application.getAppliedMatchingRound();

        ApplicationFormInfo maskedFormStructure = ApplicationFormInfo.forApplicant(
            applicationForm, formStructure, formPolicies, applicantPart);

        FormResponseInfo formResponse = FormResponseInfo.builder()
            .id(formResponseWithAnswers.id())
            .formId(formResponseWithAnswers.formId())
            .respondentMemberId(formResponseWithAnswers.respondentMemberId())
            .status(formResponseWithAnswers.status())
            .submittedAt(formResponseWithAnswers.submittedAt())
            .submittedIp(formResponseWithAnswers.submittedIp())
            .lastSavedAt(formResponseWithAnswers.lastSavedAt())
            .createdAt(formResponseWithAnswers.createdAt())
            .updatedAt(formResponseWithAnswers.updatedAt())
            .build();

        // 동일 question 에 답변이 중복 저장되어 있다면(스키마상 가능), 먼저 들어온 항목을 우선한다.
        Map<Long, AnswerInfo> answersByQuestionId = formResponseWithAnswers.answers().stream()
            .collect(Collectors.toMap(AnswerInfo::questionId, Function.identity(), (a, b) -> a));

        return ProjectApplicationDetailInfo.builder()
            .applicationId(application.getId())
            .applicantMemberId(application.getApplicantMemberId())
            .applicantPart(applicantPart)
            .matchingRoundId(round.getId())
            .matchingRoundType(round.getType())
            .matchingRoundPhase(round.getPhase())
            .status(statusVisible ? ProjectApplicationViewStatus.from(application.getStatus()) : null)
            .submittedAt(application.getSubmittedAt())
            .statusChangedAt(application.getStatusChangedAt())
            .formStructure(maskedFormStructure)
            .formResponse(formResponse)
            .answersByQuestionId(answersByQuestionId)
            .filesByFileId(filesByFileId)
            .build();
    }
}
