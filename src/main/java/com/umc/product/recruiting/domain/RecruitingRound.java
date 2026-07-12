package com.umc.product.recruiting.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.umc.product.common.BaseEntity;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationFormStatus;
import com.umc.product.recruiting.domain.enums.RecruitingRoundStatus;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;
import com.umc.product.recruiting.domain.enums.RecruitingSeasonStatus;
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
    name = "recruiting_round",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_recruiting_round_season_type_no",
        columnNames = {"recruiting_season_id", "type", "round_no"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitingRound extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiting_season_id", nullable = false)
    private RecruitingSeason season;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecruitingRoundType type;

    @Column(nullable = false, name = "round_no")
    private Integer roundNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecruitingRoundStatus status;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "recruitable_tracks", columnDefinition = "text[]")
    private List<ChallengerTrack> recruitableTracks = new ArrayList<>();

    @Column(name = "second_choice_enabled", nullable = false)
    private boolean secondChoiceEnabled;

    @Column(name = "document_start_at")
    private Instant documentStartAt;

    @Column(name = "document_end_at")
    private Instant documentEndAt;

    @Column(name = "document_result_published_at")
    private Instant documentResultPublishedAt;

    @Column(name = "interview_required", nullable = false)
    private boolean interviewRequired;

    @Column(name = "interview_start_at")
    private Instant interviewStartAt;

    @Column(name = "interview_end_at")
    private Instant interviewEndAt;

    @Column(name = "final_result_published_at")
    private Instant finalResultPublishedAt;

    @Column(name = "availability_form_id")
    private Long availabilityFormId;

    @Column(columnDefinition = "TEXT")
    private String announcement;

    @Column(name = "contact_text", columnDefinition = "TEXT")
    private String contactText;

    @Builder(access = AccessLevel.PRIVATE)
    private RecruitingRound(RecruitingSeason season, RecruitingRoundType type, Integer roundNo) {
        validateRoundNo(roundNo);
        this.season = season;
        this.type = type;
        this.roundNo = roundNo;
        this.status = RecruitingRoundStatus.DRAFT;
    }

    public static RecruitingRound createRegular(RecruitingSeason season) {
        return RecruitingRound.builder()
            .season(season)
            .type(RecruitingRoundType.REGULAR)
            .roundNo(1)
            .build();
    }

    public static RecruitingRound createRegular(
        RecruitingSeason season,
        RecruitingRoundConfiguration configuration
    ) {
        RecruitingRound round = createRegular(season);
        round.applyConfiguration(configuration);
        return round;
    }

    public static RecruitingRound createAdditional(RecruitingSeason season, Integer roundNo) {
        return RecruitingRound.builder()
            .season(season)
            .type(RecruitingRoundType.ADDITIONAL)
            .roundNo(roundNo)
            .build();
    }

    public static RecruitingRound createAdditional(
        RecruitingSeason season,
        Integer roundNo,
        RecruitingRoundConfiguration configuration
    ) {
        RecruitingRound round = createAdditional(season, roundNo);
        round.applyConfiguration(configuration);
        return round;
    }

    public void updateConfiguration(RecruitingRoundConfiguration configuration, boolean applicationExists) {
        if (configuration == null) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_ROUND_INVALID_SCHEDULE);
        }
        boolean recruitmentPolicyChanged = !Set.copyOf(recruitableTracks)
            .equals(Set.copyOf(configuration.recruitableTracks()))
            || secondChoiceEnabled != configuration.secondChoiceEnabled();
        if (recruitmentPolicyChanged && (status != RecruitingRoundStatus.DRAFT || applicationExists)) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_ROUND_RECRUITMENT_POLICY_LOCKED);
        }
        applyConfiguration(configuration);
    }

    private void applyConfiguration(RecruitingRoundConfiguration configuration) {
        this.recruitableTracks = new ArrayList<>(configuration.recruitableTracks());
        this.secondChoiceEnabled = configuration.secondChoiceEnabled();
        this.documentStartAt = configuration.documentStartAt();
        this.documentEndAt = configuration.documentEndAt();
        this.documentResultPublishedAt = configuration.documentResultPublishedAt();
        this.interviewRequired = configuration.interviewRequired();
        this.interviewStartAt = configuration.interviewStartAt();
        this.interviewEndAt = configuration.interviewEndAt();
        this.finalResultPublishedAt = configuration.finalResultPublishedAt();
        this.availabilityFormId = configuration.availabilityFormId();
        this.announcement = configuration.announcement();
        this.contactText = configuration.contactText();
    }

    /**
     * Recruiting 내부에 저장된 Season/Round/ApplicationForm 상태와 Round 서류 기간만 검사한다.
     * 인자로 받는 상태는 RecruitingApplicationForm의 local 상태이며 Survey Form 공개 상태가 아니다.
     * 실제 Survey Form의 공개 상태와 Form window는 이 메서드의 책임이 아니다.
     * TODO: Form 공개 상태/기간 조회 계약이 제공되면 application 호출 경계에서 별도로 함께 검증한다.
     */
    public boolean isLocalApplicationPeriodOpenAt(
        Instant currentTime,
        RecruitingApplicationFormStatus localApplicationFormStatus
    ) {
        if (currentTime == null || documentStartAt == null || documentEndAt == null) {
            return false;
        }
        return season.getStatus() == RecruitingSeasonStatus.ACTIVE
            && status == RecruitingRoundStatus.OPEN
            && localApplicationFormStatus == RecruitingApplicationFormStatus.PUBLISHED
            && !currentTime.isBefore(documentStartAt)
            && !currentTime.isAfter(documentEndAt);
    }

    public boolean isRecruitableTrack(ChallengerTrack track) {
        return track != null
            && track != ChallengerTrack.INFRA_PLUS
            && recruitableTracks.contains(track);
    }

    public void open() {
        validateStatus(RecruitingRoundStatus.DRAFT);
        this.status = RecruitingRoundStatus.OPEN;
    }

    public void close() {
        validateStatus(RecruitingRoundStatus.OPEN);
        this.status = RecruitingRoundStatus.CLOSED;
    }

    private static void validateRoundNo(Integer roundNo) {
        if (roundNo == null || roundNo < 1) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_ROUND_INVALID_ROUND_NO);
        }
    }

    private void validateStatus(RecruitingRoundStatus expectedStatus) {
        if (this.status != expectedStatus) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_ROUND_INVALID_TRANSITION);
        }
    }
}
