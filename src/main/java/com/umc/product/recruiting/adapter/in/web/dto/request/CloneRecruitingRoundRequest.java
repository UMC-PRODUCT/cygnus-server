package com.umc.product.recruiting.adapter.in.web.dto.request;

import com.umc.product.recruiting.application.port.in.command.dto.CloneRecruitingRoundCommand;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "모집 Round 복제 요청")
public record CloneRecruitingRoundRequest(
    @NotNull @Positive Long targetSeasonId,
    @NotBlank @Size(max = 100) String title,
    @NotNull RecruitingRoundType type,
    @Positive Integer roundNo
) {

    public CloneRecruitingRoundCommand toCommand(
        Long sourceSeasonId,
        Long sourceRoundId,
        Long requesterMemberId
    ) {
        return CloneRecruitingRoundCommand.builder()
            .sourceSeasonId(sourceSeasonId)
            .sourceRoundId(sourceRoundId)
            .targetSeasonId(targetSeasonId)
            .title(title)
            .type(type)
            .roundNo(roundNo)
            .requesterMemberId(requesterMemberId)
            .build();
    }
}
