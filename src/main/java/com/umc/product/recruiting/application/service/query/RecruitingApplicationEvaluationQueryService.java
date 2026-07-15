package com.umc.product.recruiting.application.service.query;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationEvaluationUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingRoundEvaluatorUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationEvaluationInfo;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationEvaluationPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecruitingApplicationEvaluationQueryService implements GetRecruitingApplicationEvaluationUseCase {

    private final LoadRecruitingApplicationPort loadApplicationPort;
    private final LoadRecruitingApplicationEvaluationPort loadEvaluationPort;
    private final GetRecruitingRoundEvaluatorUseCase getRoundEvaluatorUseCase;
    private final CheckPermissionUseCase checkPermissionUseCase;

    @Override
    public List<RecruitingApplicationEvaluationInfo> listVisibleEvaluations(
        Long applicationId,
        Long requesterMemberId,
        RecruitingEvaluatorStage stage
    ) {
        RecruitingApplication application = loadApplicationPort.getById(applicationId);
        if (canReadAsRecruitingOperator(application, requesterMemberId)) {
            return listAll(applicationId, stage);
        }
        requireEvaluator(application, requesterMemberId, stage);
        return loadEvaluationPort
            .findByApplicationIdAndEvaluatorMemberIdAndStage(applicationId, requesterMemberId, stage)
            .map(own -> own.isSubmitted()
                ? listAll(applicationId, stage)
                : List.of(RecruitingApplicationEvaluationInfo.from(own)))
            .orElse(List.of());
    }

    private boolean canReadAsRecruitingOperator(RecruitingApplication application, Long requesterMemberId) {
        return checkPermissionUseCase.check(
            requesterMemberId,
            ResourcePermission.of(
                ResourceType.RECRUITMENT,
                application.getRound().getSeason().getId(),
                PermissionType.READ
            )
        );
    }

    private List<RecruitingApplicationEvaluationInfo> listAll(
        Long applicationId,
        RecruitingEvaluatorStage stage
    ) {
        return loadEvaluationPort.listByApplicationIdAndStage(applicationId, stage).stream()
            .map(RecruitingApplicationEvaluationInfo::from)
            .toList();
    }

    private void requireEvaluator(
        RecruitingApplication application,
        Long requesterMemberId,
        RecruitingEvaluatorStage stage
    ) {
        if (!getRoundEvaluatorUseCase.canEvaluate(application.getRound().getId(), requesterMemberId)) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_EVALUATION_ACCESS_DENIED);
        }
    }
}
