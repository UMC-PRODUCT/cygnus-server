package com.umc.product.recruiting.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.recruiting.application.port.out.GenerateRecruitingApplicationKeyPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

class RecruitingApplicationKeyIssuerTest {

    @Test
    @DisplayName("이메일과 충돌하지 않는 6자리 지원 키를 발급한다")
    void issueNonConflictingKey() {
        GenerateRecruitingApplicationKeyPort generator = mock(GenerateRecruitingApplicationKeyPort.class);
        LoadRecruitingApplicationPort loadPort = mock(LoadRecruitingApplicationPort.class);
        given(generator.generate()).willReturn("A1B2C3");
        given(loadPort.existsByApplicantEmailAndApplicationKey("applicant@example.com", "A1B2C3"))
            .willReturn(false);
        RecruitingApplicationKeyIssuer issuer = new RecruitingApplicationKeyIssuer(generator, loadPort);

        String issued = issuer.issue("applicant@example.com");

        assertThat(issued).isEqualTo("A1B2C3");
    }

    @Test
    @DisplayName("지원 키가 10회 연속 충돌하면 명시적인 오류를 던진다")
    void failAfterTenCollisions() {
        GenerateRecruitingApplicationKeyPort generator = mock(GenerateRecruitingApplicationKeyPort.class);
        LoadRecruitingApplicationPort loadPort = mock(LoadRecruitingApplicationPort.class);
        given(generator.generate()).willReturn("A1B2C3");
        given(loadPort.existsByApplicantEmailAndApplicationKey("applicant@example.com", "A1B2C3"))
            .willReturn(true);
        RecruitingApplicationKeyIssuer issuer = new RecruitingApplicationKeyIssuer(generator, loadPort);

        assertThatThrownBy(() -> issuer.issue("applicant@example.com"))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_APPLICATION_KEY_ISSUE_FAILED);
        then(generator).should(times(10)).generate();
        then(loadPort).should(times(10))
            .existsByApplicantEmailAndApplicationKey("applicant@example.com", "A1B2C3");
    }

    @Test
    @DisplayName("9회 충돌 후 10번째 후보가 유일하면 발급한다")
    void issueTenthCandidateAfterNineCollisions() {
        GenerateRecruitingApplicationKeyPort generator = mock(GenerateRecruitingApplicationKeyPort.class);
        LoadRecruitingApplicationPort loadPort = mock(LoadRecruitingApplicationPort.class);
        AtomicInteger attempts = new AtomicInteger();
        given(generator.generate()).willAnswer(ignored ->
            attempts.incrementAndGet() < 10 ? "A1B2C3" : "D4E5F6");
        given(loadPort.existsByApplicantEmailAndApplicationKey("applicant@example.com", "A1B2C3"))
            .willReturn(true);
        given(loadPort.existsByApplicantEmailAndApplicationKey("applicant@example.com", "D4E5F6"))
            .willReturn(false);
        RecruitingApplicationKeyIssuer issuer = new RecruitingApplicationKeyIssuer(generator, loadPort);

        String issued = issuer.issue("applicant@example.com");

        assertThat(issued).isEqualTo("D4E5F6");
        then(generator).should(times(10)).generate();
    }
}
