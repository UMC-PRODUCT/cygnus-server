package com.umc.product.recruiting.application.port.in.command.dto;

import com.umc.product.recruiting.domain.enums.RecruitingRoundType;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.Builder;

@Builder
public record CreateRecruitingRoundCommand(
    Long seasonId,
    RecruitingRoundType type,
    Integer roundNo,
    String title,
    RecruitingRoundConfigurationCommand configuration
) {

    public CreateRecruitingRoundCommand {
        if (configuration == null) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_ROUND_INVALID_SCHEDULE);
        }
    }
}
