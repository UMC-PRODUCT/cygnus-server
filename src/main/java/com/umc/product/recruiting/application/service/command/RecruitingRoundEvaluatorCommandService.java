package com.umc.product.recruiting.application.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.recruiting.application.port.in.command.AuthorizeRecruitingManagementUseCase;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingRoundEvaluatorUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.RecruitingRoundEvaluatorCommand;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundEvaluatorPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingRoundEvaluatorPort;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingRoundEvaluator;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class RecruitingRoundEvaluatorCommandService implements ManageRecruitingRoundEvaluatorUseCase {

    private final LoadRecruitingRoundPort loadRoundPort;
    private final LoadRecruitingRoundEvaluatorPort loadEvaluatorPort;
    private final SaveRecruitingRoundEvaluatorPort saveEvaluatorPort;
    private final AuthorizeRecruitingManagementUseCase authorizeManagementUseCase;

    @Override
    public Long addEvaluator(RecruitingRoundEvaluatorCommand command) {
        RecruitingRound round = loadRoundPort.getById(command.roundId());
        authorizeManagementUseCase.authorizeSeasonManagement(
            command.requesterMemberId(),
            round.getSeason().getId()
        );
        if (loadEvaluatorPort.existsByRoundIdAndMemberId(command.roundId(), command.memberId())) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_ROUND_EVALUATOR_ALREADY_EXISTS);
        }

        RecruitingRoundEvaluator evaluator = RecruitingRoundEvaluator.create(round, command.memberId());
        return saveEvaluatorPort.save(evaluator).getId();
    }

    @Override
    public void removeEvaluator(RecruitingRoundEvaluatorCommand command) {
        RecruitingRound round = loadRoundPort.getById(command.roundId());
        authorizeManagementUseCase.authorizeSeasonManagement(
            command.requesterMemberId(),
            round.getSeason().getId()
        );
        RecruitingRoundEvaluator evaluator = loadEvaluatorPort.getByRoundIdAndMemberId(
            command.roundId(),
            command.memberId()
        );
        saveEvaluatorPort.delete(evaluator);
    }
}
