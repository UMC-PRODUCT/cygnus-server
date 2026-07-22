package com.umc.product.recruiting.application.service.query;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.form.application.port.in.query.GetFormResponseUseCase;
import com.umc.product.form.application.port.in.query.dto.FormResponseWithAnswersInfo;
import com.umc.product.recruiting.application.port.in.command.AuthorizeRecruitingManagementUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingRoundEvaluatorUseCase;
import com.umc.product.recruiting.application.port.in.query.SearchRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationDetailInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationSearchQuery;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationSummaryInfo;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationEvaluationPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundPort;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingApplicationEvaluation;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecruitingApplicationReviewQueryService implements SearchRecruitingApplicationUseCase {

    private final LoadRecruitingApplicationPort loadApplicationPort;
    private final LoadRecruitingApplicationEvaluationPort loadEvaluationPort;
    private final LoadRecruitingRoundPort loadRoundPort;
    private final GetRecruitingRoundEvaluatorUseCase getRoundEvaluatorUseCase;
    private final AuthorizeRecruitingManagementUseCase authorizeManagementUseCase;
    private final GetFormResponseUseCase getFormResponseUseCase;

    @Override
    public Page<RecruitingApplicationSummaryInfo> search(RecruitingApplicationSearchQuery query) {
        RecruitingRound round = loadRoundPort.getById(query.roundId());
        authorizeReview(round, query.requesterMemberId());
        Page<RecruitingApplication> applications = loadApplicationPort.searchByRoundId(
            query.roundId(),
            query.statuses(),
            query.tracks(),
            query.pageable()
        );
        Map<Long, Set<RecruitingEvaluatorStage>> stagesByApplication = stagesEvaluatedByRequester(
            applications.getContent(),
            query.requesterMemberId()
        );
        return applications.map(application -> {
            Set<RecruitingEvaluatorStage> stages = stagesByApplication.getOrDefault(
                application.getId(),
                Set.of()
            );
            return RecruitingApplicationSummaryInfo.from(
                application,
                stages.contains(RecruitingEvaluatorStage.DOCUMENT),
                stages.contains(RecruitingEvaluatorStage.INTERVIEW)
            );
        });
    }

    @Override
    public RecruitingApplicationDetailInfo getDetail(
        Long roundId,
        Long applicationId,
        Long requesterMemberId
    ) {
        if (!roundId.equals(loadApplicationPort.getRoundIdByApplicationId(applicationId))) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_NOT_FOUND);
        }
        RecruitingApplicationDetailInfo detail = getDetails(Set.of(applicationId), requesterMemberId).get(applicationId);
        if (detail == null) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_NOT_FOUND);
        }
        return detail;
    }

    @Override
    public Map<Long, RecruitingApplicationDetailInfo> getDetails(
        Set<Long> applicationIds,
        Long requesterMemberId
    ) {
        if (applicationIds == null || applicationIds.isEmpty()) {
            return Map.of();
        }

        List<RecruitingApplication> applications = loadApplicationPort.listByIdsWithDetails(applicationIds).stream()
            .filter(application -> application.getStatus()
                != com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus.DRAFT)
            .toList();
        applications.stream()
            .map(RecruitingApplication::getRound)
            .collect(java.util.stream.Collectors.toMap(
                RecruitingRound::getId,
                java.util.function.Function.identity(),
                (left, right) -> left
            ))
            .values()
            .forEach(round -> authorizeReview(round, requesterMemberId));

        Map<Long, Set<RecruitingEvaluatorStage>> stagesByApplication = stagesEvaluatedByRequester(
            applications,
            requesterMemberId
        );
        Set<Long> memberResponseIds = applications.stream()
            .filter(application -> !application.isAnonymous())
            .map(RecruitingApplication::getFormResponseId)
            .collect(java.util.stream.Collectors.toSet());
        Map<Long, String> anonymousAccessKeys = applications.stream()
            .filter(RecruitingApplication::isAnonymous)
            .collect(java.util.stream.Collectors.toMap(
                RecruitingApplication::getFormResponseId,
                RecruitingApplication::getFormResponseAccessKey
            ));
        Map<Long, FormResponseWithAnswersInfo> responsesById = new HashMap<>(
            getFormResponseUseCase.findResponsesWithAnswers(memberResponseIds)
        );
        responsesById.putAll(
            getFormResponseUseCase.findAnonymousResponsesWithAnswers(anonymousAccessKeys)
        );

        Map<Long, RecruitingApplicationDetailInfo> result = new LinkedHashMap<>();
        for (RecruitingApplication application : applications) {
            FormResponseWithAnswersInfo response = responsesById.get(application.getFormResponseId());
            if (response == null) {
                continue;
            }
            Set<RecruitingEvaluatorStage> stages = stagesByApplication.getOrDefault(application.getId(), Set.of());
            result.put(application.getId(), RecruitingApplicationDetailInfo.builder()
                .application(RecruitingApplicationSummaryInfo.from(
                    application,
                    stages.contains(RecruitingEvaluatorStage.DOCUMENT),
                    stages.contains(RecruitingEvaluatorStage.INTERVIEW)
                ))
                .formResponseId(response.id())
                .answers(response.answers())
                .build());
        }
        return result;
    }

    private void authorizeReview(RecruitingRound round, Long requesterMemberId) {
        if (getRoundEvaluatorUseCase.canEvaluate(round.getId(), requesterMemberId)
            || authorizeManagementUseCase.canManageSeason(requesterMemberId, round.getSeason().getId())) {
            return;
        }
        throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_EVALUATION_ACCESS_DENIED);
    }

    private Map<Long, Set<RecruitingEvaluatorStage>> stagesEvaluatedByRequester(
        List<RecruitingApplication> applications,
        Long requesterMemberId
    ) {
        List<Long> applicationIds = applications.stream().map(RecruitingApplication::getId).toList();
        Map<Long, Set<RecruitingEvaluatorStage>> result = new HashMap<>();
        for (RecruitingApplicationEvaluation evaluation
            : loadEvaluationPort.listByApplicationIdsAndEvaluatorMemberId(applicationIds, requesterMemberId)) {
            result.computeIfAbsent(
                evaluation.getApplication().getId(),
                ignored -> EnumSet.noneOf(RecruitingEvaluatorStage.class)
            ).add(evaluation.getStage());
        }
        return result;
    }
}
