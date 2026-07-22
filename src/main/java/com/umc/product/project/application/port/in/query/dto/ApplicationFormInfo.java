package com.umc.product.project.application.port.in.query.dto;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.project.domain.ProjectApplicationForm;
import com.umc.product.project.domain.ProjectApplicationFormPolicy;
import com.umc.product.project.domain.enums.FormSectionType;

import lombok.Builder;

/**
 * 지원 폼 구조 (폼 메타 + 섹션 → 질문 → 옵션) Info DTO.
 * <p>
 * Form 도메인의 {@link FormWithStructureInfo} 와 Project 도메인의 정책({@link ProjectApplicationFormPolicy}) 을 합성하여 단일 응답 구조를
 * 만든다.
 * <ul>
 *   <li>{@link #of} — PM/운영진 시점의 마스킹 없는 전체 섹션 노출</li>
 *   <li>{@link #forApplicant} — 일반 챌린저 시점의 파트 기반 마스킹 (COMMON + 본인 파트 PART 만)</li>
 * </ul>
 */
@Builder
public record ApplicationFormInfo(
    Long projectId,
    Long applicationFormId,
    String title,
    String description,
    List<SectionInfo> sections
) {

    /**
     * 어댑터/서비스에서 모은 데이터들을 합성하여 Info DTO 를 조립한다. 마스킹 없이 전체 섹션을 노출한다.
     */
    public static ApplicationFormInfo of(
        ProjectApplicationForm applicationForm,
        FormWithStructureInfo formStructure,
        List<ProjectApplicationFormPolicy> policies
    ) {
        Map<Long, ProjectApplicationFormPolicy> policyByFormSectionId = policies.stream()
            .collect(Collectors.toMap(ProjectApplicationFormPolicy::getFormSectionId, Function.identity()));

        List<SectionInfo> sections = formStructure.sections().stream()
            .map(section -> SectionInfo.of(section, policyByFormSectionId.get(section.sectionId())))
            .toList();

        return ApplicationFormInfo.builder()
            .projectId(applicationForm.getProject().getId())
            .applicationFormId(applicationForm.getId())
            .title(formStructure.title())
            .description(formStructure.description())
            .sections(sections)
            .build();
    }

    /**
     * 일반 챌린저(지원자) 시점의 마스킹된 Info 를 조립한다.
     * <p>
     * {@link FormSectionType#COMMON COMMON} 섹션은 모두 노출, {@link FormSectionType#PART PART} 섹션은 {@code applicantPart} 가
     * 정책의 {@code allowedParts} 에 포함될 때만 노출한다. 정책이 누락된 섹션은 안전을 위해 차단한다.
     * <p>
     * 외부 사용자(프로젝트 기수에 챌린저 레코드가 없는 호출자)는 Service 단에서 사전에 차단되어야 하며, 본 메서드는 호출자가 해당 기수의 챌린저임이 확인된 상태로 진입한다.
     */
    public static ApplicationFormInfo forApplicant(
        ProjectApplicationForm applicationForm,
        FormWithStructureInfo formStructure,
        List<ProjectApplicationFormPolicy> policies,
        ChallengerPart applicantPart
    ) {
        Map<Long, ProjectApplicationFormPolicy> policyByFormSectionId = policies.stream()
            .collect(Collectors.toMap(ProjectApplicationFormPolicy::getFormSectionId, Function.identity()));

        List<SectionInfo> sections = formStructure.sections().stream()
            .filter(section -> isVisibleToApplicant(
                policyByFormSectionId.get(section.sectionId()), applicantPart))
            .map(section -> SectionInfo.of(section, policyByFormSectionId.get(section.sectionId())))
            .toList();

        return ApplicationFormInfo.builder()
            .projectId(applicationForm.getProject().getId())
            .applicationFormId(applicationForm.getId())
            .title(formStructure.title())
            .description(formStructure.description())
            .sections(sections)
            .build();
    }

    private static boolean isVisibleToApplicant(
        ProjectApplicationFormPolicy policy, ChallengerPart applicantPart
    ) {
        if (policy == null) {
            return false;
        }
        if (policy.getType() == FormSectionType.COMMON) {
            return true;
        }
        return policy.getAllowedParts().contains(applicantPart);
    }

    /**
     * 섹션 정보. 정책이 없는 섹션(보통은 발생하지 않지만 데이터 정합 깨질 경우)은 PART + 빈 parts 로 폴백한다.
     */
    @Builder
    public record SectionInfo(
        Long sectionId,
        FormSectionType type,
        Set<ChallengerPart> allowedParts,
        String title,
        String description,
        long orderNo,
        List<QuestionInfo> questions
    ) {
        public static SectionInfo of(
            FormWithStructureInfo.SectionWithQuestions section,
            ProjectApplicationFormPolicy policy
        ) {
            FormSectionType type = (policy != null) ? policy.getType() : FormSectionType.PART;
            Set<ChallengerPart> allowedParts = (policy != null)
                ? new HashSet<>(policy.getAllowedParts())
                : Set.of();

            List<QuestionInfo> questions = section.questions().stream()
                .map(QuestionInfo::from)
                .toList();

            return SectionInfo.builder()
                .sectionId(section.sectionId())
                .type(type)
                .allowedParts(allowedParts)
                .title(section.title())
                .description(section.description())
                .orderNo(section.orderNo())
                .questions(questions)
                .build();
        }
    }

    @Builder
    public record QuestionInfo(
        Long questionId,
        QuestionType type,
        String title,
        String description,
        boolean isRequired,
        long orderNo,
        List<OptionInfo> options
    ) {
        public static QuestionInfo from(FormWithStructureInfo.QuestionWithOptions question) {
            List<OptionInfo> options = question.options().stream()
                .map(OptionInfo::from)
                .toList();

            return QuestionInfo.builder()
                .questionId(question.questionId())
                .type(question.type())
                .title(question.title())
                .description(question.description())
                .isRequired(question.isRequired())
                .orderNo(question.orderNo())
                .options(options)
                .build();
        }
    }

    @Builder
    public record OptionInfo(
        Long optionId,
        String content,
        long orderNo,
        boolean isOther,
        Long nextSectionId
    ) {
        public static OptionInfo from(FormWithStructureInfo.Option option) {
            return OptionInfo.builder()
                .optionId(option.optionId())
                .content(option.content())
                .orderNo(option.orderNo())
                .isOther(option.isOther())
                .nextSectionId(option.nextSectionId())
                .build();
        }
    }
}
