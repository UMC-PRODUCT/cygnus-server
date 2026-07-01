package com.umc.product.recruiting.application.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.recruiting.application.port.in.command.CreateRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.command.CreateRecruitingSeasonUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingRoundStatusUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingSeasonStatusUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingRoundCommand;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingSeasonCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingRoundStatusCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingSeasonStatusCommand;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingSeasonPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingRoundPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingSeasonPort;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.enums.RecruitingRoundStatus;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;
import com.umc.product.recruiting.domain.enums.RecruitingSeasonStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class RecruitingSeasonCommandService implements
    CreateRecruitingSeasonUseCase,
    UpdateRecruitingSeasonStatusUseCase,
    CreateRecruitingRoundUseCase,
    UpdateRecruitingRoundStatusUseCase {

    private final LoadRecruitingSeasonPort loadSeasonPort;
    private final SaveRecruitingSeasonPort saveSeasonPort;
    private final LoadRecruitingRoundPort loadRoundPort;
    private final SaveRecruitingRoundPort saveRoundPort;

    @Override
    public Long createSeason(CreateRecruitingSeasonCommand command) {
        if (loadSeasonPort.existsByGisuIdAndSchoolId(command.gisuId(), command.schoolId())) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_SEASON_ALREADY_EXISTS);
        }
        RecruitingSeason saved = saveSeasonPort.save(RecruitingSeason.create(command.gisuId(), command.schoolId()));
        return saved.getId();
    }

    @Override
    public void updateSeasonStatus(UpdateRecruitingSeasonStatusCommand command) {
        RecruitingSeason season = loadSeasonPort.getById(command.seasonId());
        if (command.status() == RecruitingSeasonStatus.ACTIVE) {
            season.activate();
        } else if (command.status() == RecruitingSeasonStatus.CLOSED) {
            season.close();
        } else {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_SEASON_INVALID_TRANSITION);
        }
        saveSeasonPort.save(season);
    }

    @Override
    public Long createRound(CreateRecruitingRoundCommand command) {
        Integer roundNo = resolveRoundNo(command);
        RecruitingRoundType type = command.type();
        if (loadRoundPort.existsBySeasonIdAndTypeAndRoundNo(command.seasonId(), type, roundNo)) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_ROUND_ALREADY_EXISTS);
        }

        RecruitingSeason season = loadSeasonPort.getById(command.seasonId());
        RecruitingRound round = type == RecruitingRoundType.REGULAR
            ? RecruitingRound.createRegular(season)
            : RecruitingRound.createAdditional(season, roundNo);
        return saveRoundPort.save(round).getId();
    }

    @Override
    public void updateRoundStatus(UpdateRecruitingRoundStatusCommand command) {
        RecruitingRound round = loadRoundPort.getById(command.roundId());
        if (command.status() == RecruitingRoundStatus.OPEN) {
            round.open();
        } else if (command.status() == RecruitingRoundStatus.CLOSED) {
            round.close();
        } else {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_ROUND_INVALID_TRANSITION);
        }
        saveRoundPort.save(round);
    }

    private Integer resolveRoundNo(CreateRecruitingRoundCommand command) {
        if (command.type() == RecruitingRoundType.REGULAR) {
            return 1;
        }
        return command.roundNo();
    }
}
