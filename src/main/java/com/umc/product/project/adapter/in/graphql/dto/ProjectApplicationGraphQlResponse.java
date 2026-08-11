package com.umc.product.project.adapter.in.graphql.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.form.application.port.in.query.dto.AnswerInfo;
import com.umc.product.form.application.port.in.query.dto.AnswerInfo.SelectedOption;
import com.umc.product.form.application.port.in.query.dto.FormResponseInfo;
import com.umc.product.form.domain.enums.FormResponseStatus;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.project.application.port.in.query.dto.ApplicationFormInfo;
import com.umc.product.project.application.port.in.query.dto.ProjectApplicationDetailInfo;
import com.umc.product.project.application.port.in.query.dto.ProjectApplicationViewStatus;
import com.umc.product.project.domain.enums.FormSectionType;
import com.umc.product.project.domain.enums.MatchingType;
import com.umc.product.storage.application.port.in.query.dto.FileInfo;

public record ProjectApplicationGraphQlResponse(
    String applicationId,
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
            GlobalId.encode(GlobalIdTypes.PROJECT_APPLICATION, info.applicationId()),
            new ProjectApplicantGraphQlResponse(
                GlobalId.encode(GlobalIdTypes.MEMBER, info.applicantMemberId()),
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
            GlobalId.encode(GlobalIdTypes.MATCHING_ROUND, info.matchingRoundId()),
            info.matchingRoundType(),
            MatchingRoundPhaseView.from(info.matchingRoundPhase())
        );
    }

    public record ProjectApplicantGraphQlResponse(
        String memberId,
        String nickname,
        String name,
        String schoolName,
        ChallengerPart part
    ) {
    }

    public record ProjectMatchingRoundBriefGraphQlResponse(
        String matchingRoundId,
        MatchingType type,
        MatchingRoundPhaseView phase
    ) {
    }

    public record ProjectApplicationFormResponseGraphQlResponse(
        String formResponseId,
        String formId,
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
                GlobalId.encode(GlobalIdTypes.FORM_RESPONSE, formResponse.id()),
                GlobalId.encode(GlobalIdTypes.FORM, formResponse.formId()),
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
        String sectionId,
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
                GlobalId.encode(GlobalIdTypes.FORM_SECTION, info.sectionId()),
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
        String questionId,
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
                GlobalId.encode(GlobalIdTypes.FORM_QUESTION, info.questionId()),
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
        String answerId,
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
                GlobalId.encode(GlobalIdTypes.FORM_ANSWER, info.id()),
                info.answeredAsType(),
                info.textValue(),
                selectedOptions,
                files,
                info.times() == null ? List.of() : info.times().stream().map(Instant::toString).toList()
            );
        }
    }

    public record ProjectApplicationSelectedOptionGraphQlResponse(
        String questionOptionId,
        String answeredAsContent
    ) {
        public static ProjectApplicationSelectedOptionGraphQlResponse from(SelectedOption info) {
            return new ProjectApplicationSelectedOptionGraphQlResponse(
                info.questionOptionId() == null
                    ? null
                    : GlobalId.encode(GlobalIdTypes.FORM_OPTION, info.questionOptionId()),
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
                GlobalId.encode(GlobalIdTypes.FILE, info.fileId()),
                info.originalFileName(),
                info.fileLink()
            );
        }
    }
}
