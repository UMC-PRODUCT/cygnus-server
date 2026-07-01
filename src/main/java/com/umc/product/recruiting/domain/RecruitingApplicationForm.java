package com.umc.product.recruiting.domain;

import com.umc.product.common.BaseEntity;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationFormStatus;
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
        name = "uk_recruiting_application_form_round_form",
        columnNames = {"recruiting_round_id", "form_id"}
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
    private ChallengerTrack track;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecruitingApplicationFormStatus status;

    @Builder(access = AccessLevel.PRIVATE)
    private RecruitingApplicationForm(RecruitingRound round, Long formId, ChallengerTrack track) {
        if (track == null) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_FORM_TRACK_REQUIRED);
        }
        this.round = round;
        this.formId = formId;
        this.track = track;
        this.status = RecruitingApplicationFormStatus.DRAFT;
    }

    public static RecruitingApplicationForm create(RecruitingRound round, Long formId, ChallengerTrack track) {
        return RecruitingApplicationForm.builder()
            .round(round)
            .formId(formId)
            .track(track)
            .build();
    }
}
