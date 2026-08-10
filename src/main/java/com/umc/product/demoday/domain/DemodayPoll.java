package com.umc.product.demoday.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.umc.product.common.BaseEntity;
import com.umc.product.demoday.domain.enums.DemodayPollStatus;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "demoday_poll")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DemodayPoll extends BaseEntity {

    private static final int MAX_NAME_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "gisu_id", nullable = false)
    private Long gisuId;

    @Column(name = "name", length = MAX_NAME_LENGTH, nullable = false)
    private String name;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private DemodayPollStatus status;

    @Column(name = "opens_at", nullable = false)
    private Instant opensAt;

    @Column(name = "closes_at", nullable = false)
    private Instant closesAt;

    /**
     * 투표에 속한 부스 목록을 읽기 전용으로 노출한다.
     *
     * <p>FK(demoday_booth.demoday_poll_id)의 쓰기 주체는 {@link DemodayBooth#getPollId()}이고
     * 이 컬렉션은 조회 전용 뷰다. 컬렉션이 FK를 소유하면 두 가지의 문제가 생긴다.
     * 첫째, 부스 INSERT 뒤에 FK를 채우는 UPDATE가 한 번 더 나간다.
     * 둘째, 영속화 전까지 부스가 자기 투표를 알 수 없어 표·스탬프의 투표 일치 검증이 불가능해진다.
     */
    @Getter(AccessLevel.NONE)
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "demoday_poll_id", insertable = false, updatable = false)
    private List<DemodayBooth> booths = new ArrayList<>();

    @Builder(access = AccessLevel.PRIVATE)
    private DemodayPoll(Long gisuId, String name, DemodayPollStatus status, Instant opensAt, Instant closesAt) {
        this.gisuId = gisuId;
        this.name = name;
        this.status = status;
        this.opensAt = opensAt;
        this.closesAt = closesAt;
    }

    public static DemodayPoll create(Long gisuId, String name, Instant opensAt, Instant closesAt) {
        validateGisu(gisuId);
        validateWindow(opensAt, closesAt);
        return DemodayPoll.builder()
            .gisuId(gisuId)
            .name(normalizeName(name))
            // status는 창에서 파생하지 않는다.
            // 창(opensAt ~ closesAt)은 예정 시각이고, status는 운영진의 활성화 의사다.
            // 생성 시점에는 항상 close 상태여야 하고, 명시적인 행위를 통해서만 상태를 변경한다.
            .status(DemodayPollStatus.CLOSED)
            .opensAt(opensAt)
            .closesAt(closesAt)
            .build();
    }

    private static void validateGisu(Long gisuId) {
        if (gisuId == null) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_POLL_GISU_REQUIRED);
        }
    }

    private static void validateWindow(Instant opensAt, Instant closesAt) {
        if (opensAt == null) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_POLL_OPEN_AT_REQUIRED);
        }

        if (closesAt == null) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_POLL_CLOSE_AT_REQUIRED);
        }

        if (!opensAt.isBefore(closesAt)) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_POLL_INVALID_WINDOW);
        }
    }

    private static String normalizeName(String name) {

        if (name == null || name.isBlank()) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_POLL_INVALID_NAME);
        }

        String normalize = name.strip();
        if (normalize.codePointCount(0, normalize.length()) > MAX_NAME_LENGTH) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_POLL_INVALID_NAME);
        }

        return normalize;
    }

    public List<DemodayBooth> getBooths() {
        return Collections.unmodifiableList(booths);
    }

    // TODO: 투표 시작과 종료 상태 전이 메서드 제작
}
