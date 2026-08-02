package com.umc.product.recruiting.domain;

import java.time.Instant;

import com.umc.product.common.BaseEntity;
import com.umc.product.recruiting.domain.enums.RecruitingInterviewMode;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "recruiting_interview_session")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitingInterviewSession extends BaseEntity {

    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_LOCATION_LENGTH = 255;
    private static final int SLOT_UNIT_MINUTES = 15;
    private static final long SLOT_UNIT_SECONDS = SLOT_UNIT_MINUTES * 60L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recruiting_round_id", nullable = false)
    private Long roundId;

    @Column(nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "slot_duration_minutes", nullable = false)
    private Integer slotDurationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecruitingInterviewMode mode;

    @Column(nullable = false, length = MAX_LOCATION_LENGTH)
    private String location;

    @Builder(access = AccessLevel.PRIVATE)
    private RecruitingInterviewSession(
        Long roundId,
        String name,
        Instant startsAt,
        Instant endsAt,
        Integer slotDurationMinutes,
        RecruitingInterviewMode mode,
        String location
    ) {
        this.roundId = roundId;
        apply(name, startsAt, endsAt, slotDurationMinutes, mode, location);
    }

    public static RecruitingInterviewSession create(
        Long roundId,
        String name,
        Instant startsAt,
        Instant endsAt,
        Integer slotDurationMinutes,
        RecruitingInterviewMode mode,
        String location,
        Instant roundInterviewStartsAt,
        Instant roundInterviewEndsAt
    ) {
        validateRoundId(roundId);
        validateInterviewWindow(startsAt, endsAt, roundInterviewStartsAt, roundInterviewEndsAt);
        return RecruitingInterviewSession.builder()
            .roundId(roundId)
            .name(name)
            .startsAt(startsAt)
            .endsAt(endsAt)
            .slotDurationMinutes(slotDurationMinutes)
            .mode(mode)
            .location(location)
            .build();
    }

    public void update(
        String name,
        Instant startsAt,
        Instant endsAt,
        Integer slotDurationMinutes,
        RecruitingInterviewMode mode,
        String location,
        Instant roundInterviewStartsAt,
        Instant roundInterviewEndsAt
    ) {
        validateInterviewWindow(startsAt, endsAt, roundInterviewStartsAt, roundInterviewEndsAt);
        apply(name, startsAt, endsAt, slotDurationMinutes, mode, location);
    }

    private void apply(
        String name,
        Instant startsAt,
        Instant endsAt,
        Integer slotDurationMinutes,
        RecruitingInterviewMode mode,
        String location
    ) {
        validateSession(name, startsAt, endsAt, mode, location);
        validateSlot(startsAt, endsAt, slotDurationMinutes);
        this.name = name.trim();
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.slotDurationMinutes = slotDurationMinutes;
        this.mode = mode;
        this.location = location.trim();
    }

    private static void validateRoundId(Long roundId) {
        if (roundId == null || roundId <= 0) {
            throw invalidSession();
        }
    }

    private static void validateSession(
        String name,
        Instant startsAt,
        Instant endsAt,
        RecruitingInterviewMode mode,
        String location
    ) {
        if (name == null
            || name.isBlank()
            || name.trim().length() > MAX_NAME_LENGTH
            || startsAt == null
            || endsAt == null
            || !startsAt.isBefore(endsAt)
            || mode == null
            || location == null
            || location.isBlank()
            || location.trim().length() > MAX_LOCATION_LENGTH) {
            throw invalidSession();
        }
    }

    private static void validateSlot(Instant startsAt, Instant endsAt, Integer slotDurationMinutes) {
        if (slotDurationMinutes == null
            || slotDurationMinutes <= 0
            || slotDurationMinutes % SLOT_UNIT_MINUTES != 0
            || !isAligned(startsAt)
            || !isAligned(endsAt)) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_INTERVIEW_SESSION_INVALID_SLOT);
        }
    }

    private static boolean isAligned(Instant time) {
        return time.getNano() == 0 && Math.floorMod(time.getEpochSecond(), SLOT_UNIT_SECONDS) == 0;
    }

    private static void validateInterviewWindow(
        Instant startsAt,
        Instant endsAt,
        Instant roundInterviewStartsAt,
        Instant roundInterviewEndsAt
    ) {
        if (startsAt == null
            || endsAt == null
            || roundInterviewStartsAt == null
            || roundInterviewEndsAt == null
            || !roundInterviewStartsAt.isBefore(roundInterviewEndsAt)
            || startsAt.isBefore(roundInterviewStartsAt)
            || endsAt.isAfter(roundInterviewEndsAt)) {
            throw invalidSession();
        }
    }

    private static RecruitingDomainException invalidSession() {
        return new RecruitingDomainException(RecruitingErrorCode.RECRUITING_INTERVIEW_SESSION_INVALID);
    }
}
