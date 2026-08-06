package com.umc.product.challenger.domain;

import com.umc.product.challenger.domain.enums.PointType;
import com.umc.product.common.BaseEntity;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 챌린저의 상벌점 점수 입니다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "challenger_point")
public class ChallengerPoint extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenger_id", nullable = false)
    private Challenger challenger;

    @Column(nullable = false, name = "point")
    @Enumerated(EnumType.STRING)
    private PointType type;

    // type가 custom인 경우를 위한 컬럼. 존재할 경우 ENUM 값에 우선하여 적용합니다.
    @Column(name = "point_value")
    private Integer pointValue;

    @Column(length = 200)
    private String description;

    private ChallengerPoint(Challenger challenger, PointType type, Integer pointValue, String description) {
        this.challenger = challenger;
        this.type = type;
        this.pointValue = pointValue;
        this.description = description;
    }

    /**
     * 새로운 상벌점을 생성합니다.
     */
    public static ChallengerPoint create(Challenger challenger, PointType type, String description) {
        return new ChallengerPoint(challenger, type, null, description);
    }

    public static ChallengerPoint create(
        Challenger challenger, PointType type, Integer pointValue,
        String description
    ) {
        return new ChallengerPoint(challenger, type, pointValue, description);
    }

    public Long getChallengerId() {
        return challenger.getId();
    }

    /**
     * 상벌점의 설명을 수정합니다.
     */
    public void updateDescription(String newDescription) {
        this.description = newDescription;
    }

    public Double getPointValue() {
        return pointValue != null
            ? pointValue
            : type.getValue();
    }
}
