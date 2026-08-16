package com.umc.product.recruiting.application.service.command;

import org.springframework.stereotype.Component;

import com.umc.product.recruiting.application.port.out.GenerateRecruitingApplicationKeyPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RecruitingApplicationKeyIssuer {

    private static final int MAX_ATTEMPTS = 10;

    private final GenerateRecruitingApplicationKeyPort generateApplicationKeyPort;
    private final LoadRecruitingApplicationPort loadApplicationPort;

    public String issue(String applicantEmail) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String candidate = generateApplicationKeyPort.generate();
            if (!loadApplicationPort.existsByApplicantEmailAndApplicationKey(applicantEmail, candidate)) {
                return candidate;
            }
        }
        throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_KEY_ISSUE_FAILED);
    }
}
