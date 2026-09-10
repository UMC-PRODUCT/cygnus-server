package com.umc.product.organization.domain;

import java.time.Instant;

import com.umc.product.common.BaseEntity;
import com.umc.product.common.domain.enums.GisuLearningType;
import com.umc.product.organization.domain.vo.GisuPeriod;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "gisu")
public class Gisu extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long generation;

    @Column(name = "is_active")
    private boolean isActive;

    @Embedded
    private GisuPeriod period;

    @Enumerated(EnumType.STRING)
    @Column(name = "learning_type", nullable = false)
    private GisuLearningType learningType;

    @Builder(access = AccessLevel.PRIVATE)
    private Gisu(Long generation, GisuPeriod period, boolean isActive, GisuLearningType learningType) {
        this.generation = generation;
        this.isActive = isActive;
        this.period = period;
        this.learningType = learningType == null ? GisuLearningType.PART : learningType;
    }

    public static Gisu create(Long generation, Instant startAt, Instant endAt, boolean isActive) {
        return create(generation, startAt, endAt, isActive, GisuLearningType.PART);
    }

    public static Gisu create(
        Long generation, Instant startAt, Instant endAt, boolean isActive, GisuLearningType learningType
    ) {
        return new Gisu(generation, GisuPeriod.of(startAt, endAt), isActive, learningType);
    }

    public Instant getStartAt() {
        return period.getStartAt();
    }

    public Instant getEndAt() {
        return period.getEndAt();
    }

    public void active() {
        this.isActive = true;
    }

    public void inactive() {
        this.isActive = false;
    }

    /**
     * 주어진 시점(now)을 기준으로 이 기수에서의 활동일 수를 계산합니다.
     * <p>
     * - now < startAt : 0 (아직 시작 전)
     * <p>
     * - startAt ≤ now < endAt : now - startAt (진행 중인 기수는 현재까지)
     * <p>
     * - now ≥ endAt : endAt - startAt (종료된 기수는 전체 기간)
     */
    public long activityDays(Instant now) {
        Instant start = getStartAt();
        Instant end = getEndAt();
        return GisuActivityDays.calculate(start, end, now);
    }
}
