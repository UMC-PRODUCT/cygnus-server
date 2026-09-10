package com.umc.product.challenger.domain;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.umc.product.challenger.domain.exception.ChallengerDomainException;
import com.umc.product.challenger.domain.exception.ChallengerErrorCode;
import com.umc.product.common.BaseEntity;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.common.domain.enums.ChallengerTrack;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "challenger",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_challenger_member_id_gisu_id",
        columnNames = {"member_id", "gisu_id"}
    )
)
public class Challenger extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "member_id")
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "part")
    private ChallengerPart part;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, name = "tracks", columnDefinition = "text[]")
    private List<ChallengerTrack> tracks = new ArrayList<>();

    @Column(nullable = false, name = "gisu_id")
    private Long gisuId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "status")
    private ChallengerStatus status;

    @Column(name = "modification_reason")
    private String modificationReason;

    @Column(name = "modified_by")
    private Long modifiedBy;

    @Builder
    public Challenger(Long memberId, ChallengerPart part, List<ChallengerTrack> tracks, Long gisuId) {
        List<ChallengerTrack> normalizedTracks = normalizeTracks(tracks);
        if (part == null && normalizedTracks.isEmpty()) {
            throw new ChallengerDomainException(ChallengerErrorCode.CHALLENGER_PART_NOT_FOUND);
        }
        this.memberId = memberId;
        this.part = part;
        this.tracks = normalizedTracks;
        this.gisuId = gisuId;
        this.status = ChallengerStatus.ACTIVE;
    }

    public Challenger(Long memberId, ChallengerPart part, Long gisuId) {
        this(memberId, part, List.of(), gisuId);
    }

    /**
     * 수강 없이 기수에 소속된 챌린저를 생성한다. 운영진 권한은 별도 역할로 부여한다.
     */
    public static Challenger createWithoutEnrollment(Long memberId, Long gisuId) {
        Challenger challenger = new Challenger();
        challenger.memberId = memberId;
        challenger.gisuId = gisuId;
        challenger.status = ChallengerStatus.ACTIVE;
        return challenger;
    }

    public void validateChallengerStatus() {
        if (this.status != ChallengerStatus.ACTIVE) {
            throw new ChallengerDomainException(ChallengerErrorCode.CHALLENGER_NOT_ACTIVE);
        }
    }

    /**
     * 챌린저의 파트를 변경합니다.
     */
    public void changePart(ChallengerPart newPart) {
        validateChallengerStatus();
        this.part = newPart;
    }

    public boolean addTrack(ChallengerTrack newTrack) {
        validateChallengerStatus();
        if (newTrack == null) {
            throw new IllegalArgumentException("추가할 트랙은 null일 수 없습니다.");
        }
        if (this.tracks.contains(newTrack)) {
            return false;
        }
        this.tracks.add(newTrack);
        return true;
    }

    public List<ChallengerTrack> getTracks() {
        return List.copyOf(this.tracks);
    }

    public List<ChallengerTrack> getEffectiveTracks() {
        if (!this.tracks.isEmpty()) {
            return List.copyOf(this.tracks);
        }
        if (this.part == null || this.part == ChallengerPart.ADMIN) {
            return List.of();
        }
        return List.of(ChallengerTrack.from(this.part));
    }

    /**
     * 챌린저의 상태를 변경합니다.
     */
    public void changeStatus(ChallengerStatus newStatus, Long modifiedBy, String reason) {
        validateChallengerStatus();

        this.status = newStatus;
        this.modifiedBy = modifiedBy;
        this.modificationReason = reason;
    }

    private static List<ChallengerTrack> normalizeTracks(List<ChallengerTrack> tracks) {
        if (tracks == null || tracks.isEmpty()) {
            return new ArrayList<>();
        }
        if (tracks.stream().anyMatch(track -> track == null)) {
            throw new IllegalArgumentException("트랙 목록에 null을 포함할 수 없습니다.");
        }
        return new ArrayList<>(new LinkedHashSet<>(tracks));
    }
}
