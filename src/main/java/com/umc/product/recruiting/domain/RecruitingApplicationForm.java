package com.umc.product.recruiting.domain;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.umc.product.common.BaseEntity;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationFormStatus;
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
    name = "recruiting_application_form",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_recruiting_application_form_round",
        columnNames = "recruiting_round_id"
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitingApplicationForm extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiting_round_id", nullable = false)
    private RecruitingRound round;

    @Column(nullable = false, name = "form_id")
    private Long formId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecruitingApplicationFormStatus status;

    @Builder(access = AccessLevel.PRIVATE)
    private RecruitingApplicationForm(RecruitingRound round, Long formId) {
        if (round == null || formId == null || formId <= 0) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_FORM_INVALID);
        }
        this.round = round;
        this.formId = formId;
        this.status = RecruitingApplicationFormStatus.DRAFT;
    }

    public static RecruitingApplicationForm create(RecruitingRound round, Long formId) {
        return RecruitingApplicationForm.builder()
            .round(round)
            .formId(formId)
            .build();
    }

    public void validatePoliciesForPublish(List<RecruitingFormSectionPolicy> policies) {
        Set<ChallengerTrack> sectionTracks = policies.stream()
            .filter(policy -> policy.getType() == RecruitingFormSectionType.TRACK)
            .map(RecruitingFormSectionPolicy::getTrack)
            .collect(Collectors.toSet());
        if (!sectionTracks.containsAll(round.getRecruitableTracks())) {
            throw new RecruitingDomainException(
                RecruitingErrorCode.RECRUITING_APPLICATION_FORM_TRACK_SECTION_REQUIRED
            );
        }
    }

    public void validateStructureMutable() {
        validateStatus(RecruitingApplicationFormStatus.DRAFT);
    }

    public void publish() {
        validateStatus(RecruitingApplicationFormStatus.DRAFT);
        this.status = RecruitingApplicationFormStatus.PUBLISHED;
    }

    public void close() {
        validateStatus(RecruitingApplicationFormStatus.PUBLISHED);
        this.status = RecruitingApplicationFormStatus.CLOSED;
    }

    private void validateStatus(RecruitingApplicationFormStatus expectedStatus) {
        if (status != expectedStatus) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_FORM_INVALID_TRANSITION);
        }
    }
}
