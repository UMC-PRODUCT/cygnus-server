package com.umc.product.recruiting.application.port.in.command.dto;

import java.util.List;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.recruiting.domain.enums.RecruitingFormSectionType;

import lombok.Builder;

@Builder
public record UpsertRecruitingApplicationFormCommand(
    Long seasonId,
    Long roundId,
    Long requesterMemberId,
    String description,
    List<SectionEntry> sections
) {

    public UpsertRecruitingApplicationFormCommand {
        sections = sections == null ? List.of() : List.copyOf(sections);
    }

    @Builder
    public record SectionEntry(
        Long sectionId,
        String clientKey,
        String title,
        String description,
        RecruitingFormSectionType type,
        ChallengerTrack track,
        List<QuestionEntry> questions
    ) {

        public SectionEntry {
            questions = questions == null ? List.of() : List.copyOf(questions);
        }
    }

    @Builder
    public record QuestionEntry(
        Long questionId,
        QuestionType type,
        String title,
        String description,
        boolean required,
        List<OptionEntry> options
    ) {

        public QuestionEntry {
            options = options == null ? List.of() : List.copyOf(options);
        }
    }

    @Builder
    public record OptionEntry(
        Long optionId,
        String content,
        boolean other,
        String nextSectionKey
    ) {
    }
}
