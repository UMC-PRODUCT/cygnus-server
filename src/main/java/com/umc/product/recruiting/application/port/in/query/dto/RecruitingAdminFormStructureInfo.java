package com.umc.product.recruiting.application.port.in.query.dto;

import java.util.List;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationFormStatus;
import com.umc.product.recruiting.domain.enums.RecruitingFormSectionType;

import lombok.Builder;

/**
 * 운영진 편집기에서 사용하는 지원 Form 전체 구조.
 *
 * <p>공개 조회와 달리 트랙 필터링 없이 모든 section을 담고, Form 모듈이 알지 못하는
 * COMMON/TRACK 정책({@code type}, {@code track})을 section마다 병합해 노출한다.
 * Upsert 요청과 대칭 구조라 편집 화면에서 그대로 되돌려 보낼 수 있다.
 *
 * <p>Form을 아직 만들지 않은 차수는 {@link #empty()}로 표현한다. 차수 자체가 없거나
 * 다른 시즌 소속이면 이 값이 아니라 예외로 처리한다.
 */
@Builder
public record RecruitingAdminFormStructureInfo(
    boolean exists,
    Long applicationFormId,
    Long formId,
    String title,
    String description,
    RecruitingApplicationFormStatus status,
    List<SectionInfo> sections
) {

    public static RecruitingAdminFormStructureInfo empty() {
        return RecruitingAdminFormStructureInfo.builder()
            .exists(false)
            .sections(List.of())
            .build();
    }

    @Builder
    public record SectionInfo(
        Long sectionId,
        String clientKey,
        String title,
        String description,
        Long orderNo,
        RecruitingFormSectionType type,
        ChallengerTrack track,
        List<QuestionInfo> questions
    ) {
    }

    @Builder
    public record QuestionInfo(
        Long questionId,
        String title,
        String description,
        QuestionType type,
        boolean required,
        Long orderNo,
        List<OptionInfo> options
    ) {
    }

    @Builder
    public record OptionInfo(
        Long optionId,
        String content,
        Long orderNo,
        boolean other,
        Long nextSectionId,
        String nextSectionKey
    ) {
    }
}
