package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.util.List;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.recruiting.application.port.in.command.dto.UpsertRecruitingApplicationFormCommand;
import com.umc.product.recruiting.domain.enums.RecruitingFormSectionType;

public record UpsertRecruitingApplicationFormGraphQlRequest(
    String description,
    List<Section> sections
) {

    public UpsertRecruitingApplicationFormCommand toCommand(
        Long seasonId,
        Long roundId,
        Long requesterMemberId
    ) {
        return UpsertRecruitingApplicationFormCommand.builder()
            .seasonId(seasonId)
            .roundId(roundId)
            .requesterMemberId(requesterMemberId)
            .description(description)
            .sections(sections == null ? List.of() : sections.stream().map(Section::toCommand).toList())
            .build();
    }

    public record Section(
        Long sectionId,
        String clientKey,
        String title,
        String description,
        RecruitingFormSectionType type,
        ChallengerTrack track,
        List<Question> questions
    ) {

        private UpsertRecruitingApplicationFormCommand.SectionEntry toCommand() {
            return UpsertRecruitingApplicationFormCommand.SectionEntry.builder()
                .sectionId(sectionId)
                .clientKey(clientKey)
                .title(title)
                .description(description)
                .type(type)
                .track(track)
                .questions(questions == null ? List.of() : questions.stream().map(Question::toCommand).toList())
                .build();
        }
    }

    public record Question(
        Long questionId,
        QuestionType type,
        String title,
        String description,
        boolean required,
        List<Option> options
    ) {

        private UpsertRecruitingApplicationFormCommand.QuestionEntry toCommand() {
            return UpsertRecruitingApplicationFormCommand.QuestionEntry.builder()
                .questionId(questionId)
                .type(type)
                .title(title)
                .description(description)
                .required(required)
                .options(options == null ? List.of() : options.stream().map(Option::toCommand).toList())
                .build();
        }
    }

    public record Option(
        Long optionId,
        String content,
        boolean other,
        String nextSectionKey
    ) {

        private UpsertRecruitingApplicationFormCommand.OptionEntry toCommand() {
            return UpsertRecruitingApplicationFormCommand.OptionEntry.builder()
                .optionId(optionId)
                .content(content)
                .other(other)
                .nextSectionKey(nextSectionKey)
                .build();
        }
    }
}
