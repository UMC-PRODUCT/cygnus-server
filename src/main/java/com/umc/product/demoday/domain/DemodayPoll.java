package com.umc.product.demoday.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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
            // 생성 시점에는 행사 전 코드 발급이 가능한 준비 상태여야 하고,
            // 명시적인 행위를 통해서만 상태를 변경한다.
            .status(DemodayPollStatus.READY)
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

    /**
     * 등록된 프로젝트에 연결되는 부스를 만든다.
     *
     * <p>부스 생성 자체는 {@link DemodayBooth#forProject(Long, Integer, Long)}가 담당하지만, 진입점을 투표에 두는 이유는
     * 부스를 더 받을 수 있는지 판단하는 근거가 투표의 운영 상태이기 때문이다. 식별자만 넘기는 팩토리를 직접
     * 호출하면 이 판단을 호출자가 대신해야 하고, 호출자가 빠뜨리면 규칙이 사라진다.
     *
     * <p>반환된 부스는 아직 저장되지 않았고 {@link #getBooths()}에도 반영되지 않는다. 이 컬렉션은 조회 전용
     * 뷰이므로 저장은 호출자가 부스 저장 Port로 수행한다.
     */
    public DemodayBooth registerProjectBooth(Integer boothCode, Long projectId) {
        requireBoothRegistrable();
        return DemodayBooth.forProject(requirePersistedId(), boothCode, projectId);
    }

    /**
     * UPMS에 등록되지 않은 외부 참가팀의 부스를 표시 이름으로 만든다.
     *
     * @see #registerProjectBooth(Integer, Long)
     */
    public DemodayBooth registerExternalBooth(Integer boothCode, String displayName) {
        requireBoothRegistrable();
        return DemodayBooth.forExternal(requirePersistedId(), boothCode, displayName);
    }

    /**
     * 부스를 더 등록할 수 있는지 여부다.
     *
     * <p>투표가 시작된 뒤에 부스가 늘어나면 먼저 투표한 사람은 그 부스를 보지 못한 채 표를 던진 것이 되어,
     * 같은 투표 안에서 사람마다 선택지가 달라진다. 그러면 순위를 비교할 근거가 사라지므로 OPEN이 되는 순간부터
     * 부스 추가를 막는다.
     */
    public boolean isBoothRegistrable() {
        return DemodayPollStatus.OPEN != status;
    }

    private void requireBoothRegistrable() {
        if (!isBoothRegistrable()) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_POLL_BOOTH_LOCKED);
        }
    }

    private Long requirePersistedId() {
        return Objects.requireNonNull(id, "poll must be persisted before registering a booth");
    }

    public void open() {
        if (DemodayPollStatus.OPEN == status) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_POLL_ALREADY_OPEN);
        }
        if (DemodayPollStatus.READY != status) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_POLL_INVALID_STATUS_TRANSITION);
        }

        status = DemodayPollStatus.OPEN;
    }

    public void close() {
        if (DemodayPollStatus.CLOSED == status) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_POLL_ALREADY_CLOSED);
        }
        if (DemodayPollStatus.OPEN != status) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_POLL_INVALID_STATUS_TRANSITION);
        }

        status = DemodayPollStatus.CLOSED;
    }

    public boolean canGenerateEtnryCode() {
        return status == DemodayPollStatus.READY || status == DemodayPollStatus.OPEN;
    }

    public void validEntryCodeGenerationAvailable(Instant now) {
        if (!now.isBefore(closesAt)) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_ENTRY_CODE_GENERATION_NOT_ALLOWED);
        }
    }

    public void validParticipationAvailable(Instant now) {
        if (!isOpen() || now.isBefore(opensAt) || !now.isBefore(closesAt)) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_POLL_NOT_FOUND);
        }
    }

    /**
     * INFO QR을 표시할 수 있는지 검증한다.
     *
     * <p>INFO QR은 참여자가 투표 재인증에 사용하므로 {@link #validParticipationAvailable(Instant)}와 같은
     * 조건(OPEN 상태이면서 투표 기간 안)을 요구한다. 이 창 밖에서 QR을 보여줘도 뒤이은 재인증이 성공할 수
     * 없으므로, 조회 시점에 운영진 화면에 명시적인 오류로 알린다.
     */
    public void validVoteQrAvailable(Instant now) {
        if (!isOpen() || now.isBefore(opensAt) || !now.isBefore(closesAt)) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_POLL_NOT_OPEN);
        }
    }

    /**
     * 참여자가 새 투표 권한을 발급받거나 최종 표를 저장할 수 있는 시간인지 검증한다.
     *
     * <p>투표 권한은 INFO QR의 만료와 독립적으로 5분간 유효하지만, Poll 종료까지 연장하는 권리는 아니다.
     * 따라서 권한 발급과 최종 저장 양쪽에서 이 규칙을 다시 확인한다.
     */
    public void validateVotingAvailable(Instant now) {
        Objects.requireNonNull(now, "now must not be null");

        if (status == DemodayPollStatus.CLOSED || !now.isBefore(closesAt)) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_VOTE_CLOSED);
        }
        if (status != DemodayPollStatus.OPEN || now.isBefore(opensAt)) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_VOTE_NOT_OPENED_YET);
        }
    }

    private boolean isOpen() {
        return status == DemodayPollStatus.OPEN;
    }
}
