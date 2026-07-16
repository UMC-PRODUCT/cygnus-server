package com.umc.product.recruiting.domain;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitingApplicantProfile {

    @Column(name = "applicant_name", nullable = false, length = 100)
    private String applicantName;

    @Column(name = "applicant_email", nullable = false, length = 255)
    private String applicantEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "first_choice", nullable = false)
    private ChallengerTrack firstChoice;

    @Enumerated(EnumType.STRING)
    @Column(name = "second_choice")
    private ChallengerTrack secondChoice;

    private RecruitingApplicantProfile(
        RecruitingRound round,
        String applicantName,
        RecruitingApplicantEmail applicantEmail,
        ChallengerTrack firstChoice,
        ChallengerTrack secondChoice
    ) {
        validateApplicantName(applicantName);
        validateChoices(round, firstChoice, secondChoice);
        this.applicantName = applicantName;
        this.applicantEmail = applicantEmail.value();
        this.firstChoice = firstChoice;
        this.secondChoice = secondChoice;
    }

    public static RecruitingApplicantProfile create(
        RecruitingRound round,
        String applicantName,
        RecruitingApplicantEmail applicantEmail,
        ChallengerTrack firstChoice,
        ChallengerTrack secondChoice
    ) {
        if (round == null || applicantEmail == null) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_REQUIRED_FIELD);
        }
        return new RecruitingApplicantProfile(round, applicantName, applicantEmail, firstChoice, secondChoice);
    }

    public boolean includesTrack(ChallengerTrack track) {
        return track != null && (track == firstChoice || track == secondChoice);
    }

    private static void validateApplicantName(String applicantName) {
        if (applicantName == null
            || applicantName.isBlank()
            || applicantName.length() > 100
            || applicantName.codePoints().anyMatch(Character::isWhitespace)) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_INVALID_APPLICANT_NAME);
        }
    }

    public static void validateChoices(
        RecruitingRound round,
        ChallengerTrack firstChoice,
        ChallengerTrack secondChoice
    ) {
        if (!round.isRecruitableTrack(firstChoice)) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_INVALID_FIRST_CHOICE);
        }
        if (secondChoice != null
            && (!round.isSecondChoiceEnabled()
                || secondChoice == firstChoice
                || !round.isRecruitableTrack(secondChoice))) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_INVALID_SECOND_CHOICE);
        }
    }
}
