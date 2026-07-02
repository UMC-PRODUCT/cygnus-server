package com.umc.product.recruiting.application.port.in.command.dto;

import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.Builder;

@Builder
public record CreateRecruitingSeasonCommand(
    Long gisuId,
    Long schoolId
) {

    public CreateRecruitingSeasonCommand {
        if (gisuId == null || schoolId == null) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_SEASON_REQUIRED_FIELD);
        }
    }
}
