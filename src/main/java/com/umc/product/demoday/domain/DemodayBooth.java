package com.umc.product.demoday.domain;

import java.time.Instant;
import java.util.Objects;

import com.umc.product.common.BaseEntity;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "demoday_booth")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DemodayBooth extends BaseEntity {

    private static final int MAX_DISPLAY_NAME_LENGTH = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 소속 투표를 식별자로만 참조한다. 역방향 조회는 {@link DemodayPoll#getBooths()}가 담당한다.
     */
    @Column(name = "demoday_poll_id", nullable = false)
    private Long pollId;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "stamp_credential_hash")
    private String stampCredentialHash;

    @Column(name = "stamp_credential_cipher")
    private String stampCredentialCipher;

    @Column(name = "stamp_credential_generated_at")
    private Instant stampCredentialGeneratedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private DemodayBooth(Long pollId, Long projectId, String displayName) {
        this.pollId = pollId;
        this.projectId = projectId;
        this.displayName = displayName;
    }

    public static DemodayBooth forProject(Long pollId, Long projectId) {
        Objects.requireNonNull(pollId, "pollId must not be null");
        validateProject(projectId);
        return DemodayBooth.builder()
            .pollId(pollId)
            .projectId(projectId)
            .build();
    }

    public static DemodayBooth forExternal(Long pollId, String displayName) {
        Objects.requireNonNull(pollId, "pollId must not be null");
        String normalizedName = requireDisplayName(displayName);
        return DemodayBooth.builder()
            .pollId(pollId)
            .displayName(normalizedName)
            .build();
    }

    public void applyStampCredential(
        String stampCredentialHash,
        String stampCredentialCipher,
        Instant stampCredentialGeneratedAt
    ) {
        this.stampCredentialHash = Objects.requireNonNull(stampCredentialHash);
        this.stampCredentialCipher = Objects.requireNonNull(stampCredentialCipher);
        this.stampCredentialGeneratedAt = Objects.requireNonNull(stampCredentialGeneratedAt);
    }

    public boolean hasStampCredential() {
        return stampCredentialHash != null
            && stampCredentialCipher != null
            && stampCredentialGeneratedAt != null;
    }

    public boolean isProjectBooth() {
        return projectId != null;
    }

    public void validateVoteTarget() {
        if (!isProjectBooth()) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_VOTE_EXTERNAL_BOOTH_NOT_ALLOWED);
        }
    }

    //=== Private Method ===

    private static void validateProject(Long projectId) {
        if (projectId == null) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_BOOTH_INVALID_IDENTIFIER);
        }
    }

    private static String requireDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_BOOTH_INVALID_NAME);
        }

        return normalizeDisplayName(displayName);
    }

    private static String normalizeDisplayName(String displayName) {
        String normalized = displayName.strip();

        if (normalized.codePointCount(0, normalized.length()) > MAX_DISPLAY_NAME_LENGTH) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_BOOTH_INVALID_NAME);
        }

        return normalized;
    }

}
