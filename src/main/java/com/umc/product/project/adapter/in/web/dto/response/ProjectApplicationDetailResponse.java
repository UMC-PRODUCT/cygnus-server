package com.umc.product.project.adapter.in.web.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.project.adapter.in.web.dto.common.MatchingRoundPhaseView;
import com.umc.product.project.adapter.in.web.dto.common.MemberBrief;
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

import lombok.Builder;

/**
 * 지원서 단건 상세 조회 Web Response DTO.
 * <p>
 * Service 가 모은 cross-domain 컨테이너({@link ProjectApplicationDetailInfo}) 를 화면 친화적 트리(섹션 -> 질문 -> 답변) 로 합성한다. 지원자 파트 기준으로
 * 제한된 폼 구조는 {@link ApplicationFormInfo} 가 이미 처리한 상태(COMMON + 지원자 파트의 PART 만)로 들어온다.
 * <p>
 * 답변이 없는 질문은 {@code answer} 필드가 {@code null}. 첨부 파일은 storage 메타에서 누락된 fileId 는 응답에서 제외된다.
 *
 * @param status      표시용 지원 상태. 제출한 매칭 차수의 {@code decisionDeadline} 전이면 제출 이후 상태는 {@code null}. DRAFT 는 지원자 본인 호출 시에만
 *                    포함되며, 그 외 호출자에게는 본 응답 자체가 반환되지 않는다.
 * @param submittedAt DRAFT 상태이면 {@code null}.
 */
@Builder
public record ProjectApplicationDetailResponse(
    Long applicationId,
    Applicant applicant,
    MatchingRoundBrief matchingRound,
    ProjectApplicationViewStatus status,
    Instant submittedAt,
    Instant statusChangedAt,
    FormResponseView formResponse
) {
    public static ProjectApplicationDetailResponse from(
        ProjectApplicationDetailInfo info, MemberBrief applicantMember
    ) {
        Applicant applicant = Applicant.builder()
            .memberId(info.applicantMemberId())
            .nickname(applicantMember == null ? null : applicantMember.nickname())
            .name(applicantMember == null ? null : applicantMember.name())
            .schoolName(applicantMember == null ? null : applicantMember.schoolName())
            .part(info.applicantPart())
            .build();

        MatchingRoundBrief round = MatchingRoundBrief.builder()
            .id(info.matchingRoundId())
            .type(info.matchingRoundType())
            .phase(MatchingRoundPhaseView.from(info.matchingRoundPhase()))
            .build();

        FormResponseView formResponse = FormResponseView.of(
            info.formResponse(),
            info.formStructure(),
            info.answersByQuestionId(),
            info.filesByFileId()
        );

        return ProjectApplicationDetailResponse.builder()
            .applicationId(info.applicationId())
            .applicant(applicant)
            .matchingRound(round)
            .status(info.status())
            .submittedAt(info.submittedAt())
            .statusChangedAt(info.statusChangedAt())
            .formResponse(formResponse)
            .build();
    }

    /**
     * 지원자(챌린저) 정보. 닉네임/이름/학교는 member 도메인, 파트는 challenger 도메인에서 합성된다.
     */
    @Builder
    public record Applicant(
        Long memberId,
        String nickname,
        String name,
        String schoolName,
        ChallengerPart part
    ) {
    }

    /**
     * 매칭 라운드 식별 정보. 라벨 합성("기획-개발자 1차 매칭" 등)은 클라이언트가 type/phase 조합으로 처리한다.
     * <p>
     * phase 는 도메인 enum 대신 표시용 enum {@link MatchingRoundPhaseView} 로 변환한다. 본 응답은 실제 라운드 엔티티가 있는 지원서만 다루므로
     * RANDOM_MATCHING 경우는 실제로 채워지지 않는다.
     */
    @Builder
    public record MatchingRoundBrief(
        Long id,
        MatchingType type,
        MatchingRoundPhaseView phase
    ) {
    }

    /**
     * FormResponse 메타 + 지원자 파트 기준 섹션 트리. 답변이 없는 질문은 {@link SectionView.QuestionView#answer} 가 {@code null}.
     */
    @Builder
    public record FormResponseView(
        Long formResponseId,
        Long formId,
        FormResponseStatus status,
        Instant submittedAt,
        Instant lastSavedAt,
        List<SectionView> sections
    ) {
        public static FormResponseView of(
            FormResponseInfo formResponse,
            ApplicationFormInfo formStructure,
            Map<Long, AnswerInfo> answersByQuestionId,
            Map<String, FileInfo> filesByFileId
        ) {
            List<SectionView> sections = formStructure.sections().stream()
                .map(section -> SectionView.of(section, answersByQuestionId, filesByFileId))
                .toList();

            return FormResponseView.builder()
                .formResponseId(formResponse.id())
                .formId(formResponse.formId())
                .status(formResponse.status())
                .submittedAt(formResponse.submittedAt())
                .lastSavedAt(formResponse.lastSavedAt())
                .sections(sections)
                .build();
        }
    }

    @Builder
    public record SectionView(
        Long sectionId,
        FormSectionType type,
        Set<ChallengerPart> allowedParts,
        String title,
        String description,
        long orderNo,
        List<QuestionView> questions
    ) {
        public static SectionView of(
            ApplicationFormInfo.SectionInfo section,
            Map<Long, AnswerInfo> answersByQuestionId,
            Map<String, FileInfo> filesByFileId
        ) {
            List<QuestionView> questions = section.questions().stream()
                .map(question -> QuestionView.of(
                    question, answersByQuestionId.get(question.questionId()), filesByFileId))
                .toList();

            return SectionView.builder()
                .sectionId(section.sectionId())
                .type(section.type())
                .allowedParts(section.allowedParts())
                .title(section.title())
                .description(section.description())
                .orderNo(section.orderNo())
                .questions(questions)
                .build();
        }

        @Builder
        public record QuestionView(
            Long questionId,
            QuestionType type,
            String title,
            String description,
            boolean isRequired,
            long orderNo,
            List<OptionView> options,
            AnswerView answer
        ) {
            public static QuestionView of(
                ApplicationFormInfo.QuestionInfo question,
                AnswerInfo answer,
                Map<String, FileInfo> filesByFileId
            ) {
                List<OptionView> options = question.options().stream()
                    .map(OptionView::from)
                    .toList();

                return QuestionView.builder()
                    .questionId(question.questionId())
                    .type(question.type())
                    .title(question.title())
                    .description(question.description())
                    .isRequired(question.isRequired())
                    .orderNo(question.orderNo())
                    .options(options)
                    .answer(answer == null ? null : AnswerView.of(answer, filesByFileId))
                    .build();
            }
        }

        @Builder
        public record OptionView(
            Long optionId,
            String content,
            long orderNo,
            boolean isOther
        ) {
            public static OptionView from(ApplicationFormInfo.OptionInfo option) {
                return OptionView.builder()
                    .optionId(option.optionId())
                    .content(option.content())
                    .orderNo(option.orderNo())
                    .isOther(option.isOther())
                    .build();
            }
        }

        /**
         * 답변 본문. 질문 타입에 따라 사용되는 필드가 다르다.
         * <ul>
         *   <li>SHORT_TEXT / LONG_TEXT -- {@code textValue}</li>
         *   <li>RADIO / DROPDOWN / CHECKBOX -- {@code selectedOptions}</li>
         *   <li>SCHEDULE -- {@code times}</li>
         *   <li>FILE -- {@code files}</li>
         *   <li>PORTFOLIO -- {@code textValue} 또는 {@code files}</li>
         * </ul>
         */
        @Builder
        public record AnswerView(
            Long answerId,
            QuestionType answeredAsType,
            String textValue,
            List<SelectedOptionView> selectedOptions,
            List<FileView> files,
            Set<Instant> times
        ) {
            public static AnswerView of(AnswerInfo answer, Map<String, FileInfo> filesByFileId) {
                List<SelectedOptionView> selectedOptions = answer.selectedOptions().stream()
                    .map(SelectedOptionView::from)
                    .toList();

                List<FileView> files = answer.fileIds() == null
                    ? List.of()
                    : answer.fileIds().stream()
                        .map(filesByFileId::get)
                        .filter(file -> file != null) // storage 에서 누락된 fileId 는 응답에서 제외
                        .map(FileView::from)
                        .toList();

                return AnswerView.builder()
                    .answerId(answer.id())
                    .answeredAsType(answer.answeredAsType())
                    .textValue(answer.textValue())
                    .selectedOptions(selectedOptions)
                    .files(files)
                    .times(answer.times())
                    .build();
            }
        }

        /**
         * 객관식 선택지 답변. {@code questionOptionId} 가 null 이면 응답 시점 이후 옵션이 삭제된 경우이며, {@code answeredAsContent} 스냅샷으로 표시한다.
         */
        @Builder
        public record SelectedOptionView(
            Long questionOptionId,
            String answeredAsContent
        ) {
            public static SelectedOptionView from(SelectedOption option) {
                return SelectedOptionView.builder()
                    .questionOptionId(option.questionOptionId())
                    .answeredAsContent(option.answeredAsContent())
                    .build();
            }
        }

        @Builder
        public record FileView(
            String fileId,
            String originalFileName,
            String url
        ) {
            public static FileView from(FileInfo file) {
                return FileView.builder()
                    .fileId(file.fileId())
                    .originalFileName(file.originalFileName())
                    .url(file.fileLink())
                    .build();
            }
        }
    }
}
