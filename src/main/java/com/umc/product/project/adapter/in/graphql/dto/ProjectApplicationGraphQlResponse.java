package com.umc.product.project.adapter.in.graphql.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.project.application.port.in.query.dto.ApplicationFormInfo;
import com.umc.product.project.application.port.in.query.dto.ProjectApplicationDetailInfo;
import com.umc.product.project.application.port.in.query.dto.ProjectApplicationViewStatus;
import com.umc.product.project.domain.enums.FormSectionType;
import com.umc.product.project.domain.enums.MatchingType;
import com.umc.product.storage.application.port.in.query.dto.FileInfo;
import com.umc.product.form.application.port.in.query.dto.AnswerInfo;
import com.umc.product.form.application.port.in.query.dto.AnswerInfo.SelectedOption;
import com.umc.product.form.application.port.in.query.dto.FormResponseInfo;
import com.umc.product.form.domain.enums.FormResponseStatus;
import com.umc.product.form.domain.enums.QuestionType;

public record ProjectApplicationGraphQlResponse(
    Long applicationId,
    ProjectApplicantGraphQlResponse applicant,
    ChallengerPart applicantPart,
    ProjectMatchingRoundBriefGraphQlResponse matchingRound,
    ProjectApplicationViewStatus status,
    String submittedAt,
    String statusChangedAt,
    ProjectApplicationFormResponseGraphQlResponse formResponse
) {
    public static ProjectApplicationGraphQlResponse from(ProjectApplicationDetailInfo info) {
        return new ProjectApplicationGraphQlResponse(
            info.applicationId(),
            new ProjectApplicantGraphQlResponse(
                info.applicantMemberId(),
                null,
                null,
                null,
                info.applicantPart()
            ),
            info.applicantPart(),
            matchingRound(info),
            info.status(),
            instantToString(info.submittedAt()),
            instantToString(info.statusChangedAt()),
            ProjectApplicationFormResponseGraphQlResponse.from(
                info.formResponse(),
                info.formStructure(),
                info.answersByQuestionId(),
                info.filesByFileId()
            )
        );
    }

    private static String instantToString(Instant instant) {
        return instant == null ? null : instant.toString();
    }

    private static ProjectMatchingRoundBriefGraphQlResponse matchingRound(ProjectApplicationDetailInfo info) {
        if (info.matchingRoundId() == null || info.matchingRoundType() == null || info.matchingRoundPhase() == null) {
            return null;
        }
        return new ProjectMatchingRoundBriefGraphQlResponse(
            info.matchingRoundId(),
            info.matchingRoundType(),
            MatchingRoundPhaseView.from(info.matchingRoundPhase())
        );
    }

    public record ProjectApplicantGraphQlResponse(
        Long memberId,
        String nickname,
        String name,
        String schoolName,
        ChallengerPart part
    ) {
    }

    public record ProjectMatchingRoundBriefGraphQlResponse(
        Long id,
        MatchingType type,
        MatchingRoundPhaseView phase
    ) {
    }

    public record ProjectApplicationFormResponseGraphQlResponse(
        Long formResponseId,
        Long formId,
        FormResponseStatus status,
        String submittedAt,
        String lastSavedAt,
        List<ProjectApplicationResponseSectionGraphQlResponse> sections
    ) {
        public static ProjectApplicationFormResponseGraphQlResponse from(
            FormResponseInfo formResponse,
            ApplicationFormInfo formStructure,
            Map<Long, AnswerInfo> answersByQuestionId,
            Map<String, FileInfo> filesByFileId
        ) {
            if (formResponse == null || formStructure == null) {
                return null;
            }
            Map<Long, AnswerInfo> answers = answersByQuestionId == null ? Map.of() : answersByQuestionId;
            Map<String, FileInfo> files = filesByFileId == null ? Map.of() : filesByFileId;

            return new ProjectApplicationFormResponseGraphQlResponse(
                formResponse.id(),
                formResponse.formId(),
                formResponse.status(),
                instantToString(formResponse.submittedAt()),
                instantToString(formResponse.lastSavedAt()),
                formStructure.sections().stream()
                    .map(section -> ProjectApplicationResponseSectionGraphQlResponse.from(section, answers, files))
                    .toList()
            );
        }
    }

    public record ProjectApplicationResponseSectionGraphQlResponse(
        Long sectionId,
        FormSectionType type,
        Set<ChallengerPart> allowedParts,
        String title,
        String description,
        long orderNo,
        List<ProjectApplicationResponseQuestionGraphQlResponse> questions
    ) {
        public static ProjectApplicationResponseSectionGraphQlResponse from(
            ApplicationFormInfo.SectionInfo info,
            Map<Long, AnswerInfo> answersByQuestionId,
            Map<String, FileInfo> filesByFileId
        ) {
            return new ProjectApplicationResponseSectionGraphQlResponse(
                info.sectionId(),
                info.type(),
                info.allowedParts(),
                info.title(),
                info.description(),
                info.orderNo(),
                info.questions().stream()
                    .map(question -> ProjectApplicationResponseQuestionGraphQlResponse.from(
                        question,
                        answersByQuestionId.get(question.questionId()),
                        filesByFileId
                    ))
                    .toList()
            );
        }
    }

    public record ProjectApplicationResponseQuestionGraphQlResponse(
        Long questionId,
        QuestionType type,
        String title,
        String description,
        boolean required,
        long orderNo,
        List<ProjectApplicationFormGraphQlResponse.ApplicationFormOptionGraphQlResponse> options,
        ProjectApplicationAnswerGraphQlResponse answer
    ) {
        public static ProjectApplicationResponseQuestionGraphQlResponse from(
            ApplicationFormInfo.QuestionInfo info,
            AnswerInfo answer,
            Map<String, FileInfo> filesByFileId
        ) {
            return new ProjectApplicationResponseQuestionGraphQlResponse(
                info.questionId(),
                info.type(),
                info.title(),
                info.description(),
                info.isRequired(),
                info.orderNo(),
                info.options().stream()
                    .map(ProjectApplicationFormGraphQlResponse.ApplicationFormOptionGraphQlResponse::from)
                    .toList(),
                answer == null ? null : ProjectApplicationAnswerGraphQlResponse.from(answer, filesByFileId)
            );
        }
    }

    public record ProjectApplicationAnswerGraphQlResponse(
        Long answerId,
        QuestionType answeredAsType,
        String textValue,
        List<ProjectApplicationSelectedOptionGraphQlResponse> selectedOptions,
        List<ProjectApplicationFileGraphQlResponse> files,
        List<String> times
    ) {
        public static ProjectApplicationAnswerGraphQlResponse from(
            AnswerInfo info,
            Map<String, FileInfo> filesByFileId
        ) {
            List<ProjectApplicationSelectedOptionGraphQlResponse> selectedOptions = info.selectedOptions().stream()
                .map(ProjectApplicationSelectedOptionGraphQlResponse::from)
                .toList();
            List<ProjectApplicationFileGraphQlResponse> files = info.fileIds() == null
                ? List.of()
                : info.fileIds().stream()
                    .map(filesByFileId::get)
                    .filter(file -> file != null)
                    .map(ProjectApplicationFileGraphQlResponse::from)
                    .toList();

            return new ProjectApplicationAnswerGraphQlResponse(
                info.id(),
                info.answeredAsType(),
                info.textValue(),
                selectedOptions,
                files,
                info.times() == null ? List.of() : info.times().stream().map(Instant::toString).toList()
            );
        }
    }

    public record ProjectApplicationSelectedOptionGraphQlResponse(
        Long questionOptionId,
        String answeredAsContent
    ) {
        public static ProjectApplicationSelectedOptionGraphQlResponse from(SelectedOption info) {
            return new ProjectApplicationSelectedOptionGraphQlResponse(
                info.questionOptionId(),
                info.answeredAsContent()
            );
        }
    }

    public record ProjectApplicationFileGraphQlResponse(
        String fileId,
        String originalFileName,
        String url
    ) {
        public static ProjectApplicationFileGraphQlResponse from(FileInfo info) {
            return new ProjectApplicationFileGraphQlResponse(
                info.fileId(),
                info.originalFileName(),
                info.fileLink()
            );
        }
    }
}
