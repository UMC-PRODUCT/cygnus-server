package com.umc.product.recruiting.adapter.in.web.dto.request;

import java.util.List;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.recruiting.application.port.in.command.dto.UpsertRecruitingApplicationFormCommand;
import com.umc.product.recruiting.domain.enums.RecruitingFormSectionType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Recruiting 지원 Form 전체 구조 Upsert 요청")
public record UpsertRecruitingApplicationFormRequest(
    String description,
    @NotEmpty List<@Valid SectionRequest> sections
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
            .sections(sections.stream().map(SectionRequest::toCommand).toList())
            .build();
    }

    public record SectionRequest(
        @Positive Long sectionId,
        @NotBlank @Size(max = 100) String clientKey,
        @NotBlank String title,
        String description,
        @NotNull RecruitingFormSectionType type,
        ChallengerTrack track,
        List<@Valid QuestionRequest> questions
    ) {

        private UpsertRecruitingApplicationFormCommand.SectionEntry toCommand() {
            return UpsertRecruitingApplicationFormCommand.SectionEntry.builder()
                .sectionId(sectionId)
                .clientKey(clientKey)
                .title(title)
                .description(description)
                .type(type)
                .track(track)
                .questions(questions == null ? List.of() : questions.stream().map(QuestionRequest::toCommand).toList())
                .build();
        }
    }

    public record QuestionRequest(
        @Positive Long questionId,
        @NotNull QuestionType type,
        @NotBlank String title,
        String description,
        boolean required,
        List<@Valid OptionRequest> options
    ) {

        private UpsertRecruitingApplicationFormCommand.QuestionEntry toCommand() {
            return UpsertRecruitingApplicationFormCommand.QuestionEntry.builder()
                .questionId(questionId)
                .type(type)
                .title(title)
                .description(description)
                .required(required)
                .options(options == null ? List.of() : options.stream().map(OptionRequest::toCommand).toList())
                .build();
        }
    }

    public record OptionRequest(
        @Positive Long optionId,
        @NotBlank String content,
        boolean other,
        @Size(max = 100) String nextSectionKey
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
