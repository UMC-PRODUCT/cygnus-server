package com.umc.product.demoday.domain;

import java.util.Objects;

import com.umc.product.common.BaseEntity;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demoday_vote_event_id", nullable = false)
    private DemodayVoteEvent voteEvent;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "display_name")
    private String displayName;

    @Builder(access = AccessLevel.PRIVATE)
    private DemodayBooth(DemodayVoteEvent voteEvent, Long projectId, String displayName) {
        this.voteEvent = voteEvent;
        this.projectId = projectId;
        this.displayName = displayName;
    }

    public static DemodayBooth forProject(DemodayVoteEvent voteEvent, Long projectId) {
        Objects.requireNonNull(voteEvent, "voteEvent must not be null");
        validateProject(projectId);
        return DemodayBooth.builder()
            .voteEvent(voteEvent)
            .projectId(projectId)
            .build();
    }

    public static DemodayBooth forExternal(DemodayVoteEvent voteEvent, String displayName) {
        Objects.requireNonNull(voteEvent, "voteEvent must not be null");
        String normalizedName = requireDisplayName(displayName);
        return DemodayBooth.builder()
            .voteEvent(voteEvent)
            .displayName(normalizedName)
            .build();
    }

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
