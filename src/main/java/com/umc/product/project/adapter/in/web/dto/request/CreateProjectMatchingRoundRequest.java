package com.umc.product.project.adapter.in.web.dto.request;

import java.time.Instant;

import com.umc.product.project.application.port.in.command.dto.CreateProjectMatchingRoundCommand;
import com.umc.product.project.domain.enums.MatchingPhase;
import com.umc.product.project.domain.enums.MatchingType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateProjectMatchingRoundRequest(
    @NotBlank(message = "매칭 차수 이름은 필수입니다") @Size(max = 255, message = "매칭 차수 이름은 255자 이하여야 합니다") String name,

    @Size(max = 255, message = "매칭 차수 설명은 255자 이하여야 합니다") String description,

    @NotNull(message = "매칭 유형은 필수입니다") MatchingType type,

    @NotNull(message = "매칭 차수 단계는 필수입니다") MatchingPhase phase,

    @NotNull(message = "기수 ID는 필수입니다") Long gisuId,

    @NotNull(message = "지부 ID는 필수입니다") Long chapterId,

    @NotNull(message = "매칭 시작 시각은 필수입니다") Instant startsAt,

    @NotNull(message = "매칭 종료 시각은 필수입니다") Instant endsAt,

    @NotNull(message = "선발 마감 시각은 필수입니다") Instant decisionDeadline
) {
    public CreateProjectMatchingRoundCommand toCreateCommand(Long requesterMemberId) {
        return CreateProjectMatchingRoundCommand.builder()
            .requesterMemberId(requesterMemberId)
            .name(name)
            .description(description)
            .type(type)
            .phase(phase)
            .gisuId(gisuId)
            .chapterId(chapterId)
            .startsAt(startsAt)
            .endsAt(endsAt)
            .decisionDeadline(decisionDeadline)
            .build();
    }
}
