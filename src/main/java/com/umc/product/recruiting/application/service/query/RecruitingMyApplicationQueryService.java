package com.umc.product.recruiting.application.service.query;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.form.application.port.in.query.GetFormResponseUseCase;
import com.umc.product.form.application.port.in.query.dto.FormResponseWithAnswersInfo;
import com.umc.product.recruiting.application.port.in.query.ListMyRecruitingApplicationsUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingPublicApplicationInfo;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecruitingMyApplicationQueryService implements ListMyRecruitingApplicationsUseCase {

    private final LoadRecruitingApplicationPort loadApplicationPort;
    private final GetFormResponseUseCase getFormResponseUseCase;
    private final Clock clock;

    @Override
    public List<RecruitingPublicApplicationInfo> listMyApplications(Long requesterMemberId) {
        List<RecruitingApplication> applications = loadApplicationPort.listByApplicantMemberId(requesterMemberId);
        if (applications.isEmpty()) {
            return List.of();
        }

        Set<Long> formResponseIds = applications.stream()
            .map(RecruitingApplication::getFormResponseId)
            .collect(Collectors.toSet());
        Map<Long, FormResponseWithAnswersInfo> formResponses =
            getFormResponseUseCase.findResponsesWithAnswers(formResponseIds);
        Instant now = clock.instant();

        return applications.stream()
            .map(application -> RecruitingApplicationViewFactory.create(
                application,
                getFormResponse(application, formResponses),
                now
            ))
            .toList();
    }

    private FormResponseWithAnswersInfo getFormResponse(
        RecruitingApplication application,
        Map<Long, FormResponseWithAnswersInfo> formResponses
    ) {
        FormResponseWithAnswersInfo formResponse = formResponses.get(application.getFormResponseId());
        if (formResponse == null) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_NOT_FOUND);
        }
        return formResponse;
    }
}
