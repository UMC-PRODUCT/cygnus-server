package com.umc.product.organization.application.port.in.query.dto.gisu;

import java.time.Instant;

import com.umc.product.common.domain.enums.GisuLearningType;
import com.umc.product.organization.domain.Gisu;
import com.umc.product.organization.domain.GisuActivityDays;

public record GisuInfo(
    Long gisuId,
    Long generation,
    Instant startAt,
    Instant endAt,
    boolean isActive,
    GisuLearningType learningType) {

    public GisuInfo(Long gisuId, Long generation, Instant startAt, Instant endAt, boolean isActive) {
        this(gisuId, generation, startAt, endAt, isActive, GisuLearningType.PART);
    }

    public static GisuInfo from(Gisu gisu) {
        return new GisuInfo(
            gisu.getId(),
            gisu.getGeneration(),
            gisu.getStartAt(),
            gisu.getEndAt(),
            gisu.isActive(),
            gisu.getLearningType()
        );
    }

    /**
     * 주어진 시점(now) 기준의 활동일을 반환합니다. {@link Gisu#activityDays(Instant)}와 동일한 규칙을 따릅니다.
     */
    public long activityDays(Instant now) {
        return GisuActivityDays.calculate(startAt, endAt, now);
    }
}
