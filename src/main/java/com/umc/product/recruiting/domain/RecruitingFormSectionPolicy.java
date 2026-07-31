package com.umc.product.recruiting.domain;

import com.umc.product.common.BaseEntity;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.enums.RecruitingFormSectionType;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "recruiting_form_section_policy",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_recruiting_form_section_policy_form_section",
        columnNames = "form_section_id"
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitingFormSectionPolicy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiting_application_form_id", nullable = false)
    private RecruitingApplicationForm applicationForm;

    @Column(name = "form_section_id", nullable = false)
    private Long formSectionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecruitingFormSectionType type;

    @Enumerated(EnumType.STRING)
    private ChallengerTrack track;

    @Builder(access = AccessLevel.PRIVATE)
    private RecruitingFormSectionPolicy(
        RecruitingApplicationForm applicationForm,
        Long formSectionId,
        RecruitingFormSectionType type,
        ChallengerTrack track
    ) {
        validateRequired(applicationForm, formSectionId);
        validatePolicy(applicationForm, type, track);
        this.applicationForm = applicationForm;
        this.formSectionId = formSectionId;
        this.type = type;
        this.track = track;
    }

    public static RecruitingFormSectionPolicy createCommon(
        RecruitingApplicationForm applicationForm,
        Long formSectionId
    ) {
        return RecruitingFormSectionPolicy.builder()
            .applicationForm(applicationForm)
            .formSectionId(formSectionId)
            .type(RecruitingFormSectionType.COMMON)
            .build();
    }

    public static RecruitingFormSectionPolicy createTrack(
        RecruitingApplicationForm applicationForm,
        Long formSectionId,
        ChallengerTrack track
    ) {
        return RecruitingFormSectionPolicy.builder()
            .applicationForm(applicationForm)
            .formSectionId(formSectionId)
            .type(RecruitingFormSectionType.TRACK)
            .track(track)
            .build();
    }

    public boolean appliesTo(ChallengerTrack firstChoice, ChallengerTrack secondChoice) {
        return type == RecruitingFormSectionType.COMMON
            || track == firstChoice
            || track == secondChoice;
    }

    public void updatePolicy(RecruitingFormSectionType type, ChallengerTrack track) {
        validatePolicy(applicationForm, type, track);
        this.type = type;
        this.track = track;
    }

    private static void validateRequired(RecruitingApplicationForm applicationForm, Long formSectionId) {
        if (applicationForm == null || formSectionId == null || formSectionId <= 0) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_FORM_SECTION_POLICY_INVALID);
        }
    }

    private static void validatePolicy(
        RecruitingApplicationForm applicationForm,
        RecruitingFormSectionType type,
        ChallengerTrack track
    ) {
        if (type == RecruitingFormSectionType.COMMON && track == null) {
            return;
        }
        if (type == RecruitingFormSectionType.TRACK
            && track != null
            && applicationForm.getRound().isRecruitableTrack(track)) {
            return;
        }
        if (type == RecruitingFormSectionType.TRACK) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_FORM_SECTION_POLICY_INVALID_TRACK);
        }
        throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_FORM_SECTION_POLICY_INVALID);
    }
}
