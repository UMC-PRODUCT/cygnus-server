package com.umc.product.recruiting.application.service.query;

import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.form.application.port.in.query.GetFormResponseUseCase;
import com.umc.product.form.application.port.in.query.dto.FormResponseWithAnswersInfo;
import com.umc.product.recruiting.application.port.in.query.GetAnonymousRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingPublicApplicationInfo;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.domain.RecruitingApplicantEmail;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecruitingPublicApplicationQueryService implements GetAnonymousRecruitingApplicationUseCase {

    private final LoadRecruitingApplicationPort loadApplicationPort;
    private final GetFormResponseUseCase getFormResponseUseCase;
    private final Clock clock;

    @Override
    public RecruitingPublicApplicationInfo getByCredential(String applicantEmail, String applicationKey) {
        String normalizedEmail = RecruitingApplicantEmail.from(applicantEmail).value();
        validateApplicationKey(applicationKey);
        RecruitingApplication application = loadApplicationPort
            .findByApplicantEmailAndApplicationKey(normalizedEmail, applicationKey)
            .filter(RecruitingApplication::isAnonymous)
            .orElseThrow(() -> new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_NOT_FOUND));
        application.validateAnonymousApplicant(normalizedEmail);
        FormResponseWithAnswersInfo formResponse = getFormResponseUseCase.getResponseWithAnswersByAccessKey(
            application.getFormResponseAccessKey()
        );

        return RecruitingApplicationViewFactory.create(application, formResponse, clock.instant());
    }

    private void validateApplicationKey(String applicationKey) {
        if (applicationKey == null || !applicationKey.matches("[A-Z0-9]{6}")) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_INVALID_KEY);
        }
    }
}
