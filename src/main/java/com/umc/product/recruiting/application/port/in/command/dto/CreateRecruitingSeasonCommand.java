package com.umc.product.recruiting.application.port.in.command.dto;

import java.util.List;

import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.Builder;

@Builder
public record CreateRecruitingSeasonCommand(
    Long requesterMemberId,
    Long gisuId,
    Long schoolId,
    List<RecruitingSeasonTrackQuotaCommand> quotas
) {

    public CreateRecruitingSeasonCommand {
        if (requesterMemberId == null || gisuId == null || schoolId == null) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_SEASON_REQUIRED_FIELD);
        }
        quotas = quotas == null ? List.of() : List.copyOf(quotas);
    }
}
