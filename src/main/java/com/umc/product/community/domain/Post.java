package com.umc.product.community.domain;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.BatchSize;

import com.umc.product.common.BaseEntity;
import com.umc.product.community.domain.enums.Category;
import com.umc.product.community.domain.exception.CommunityDomainException;
import com.umc.product.community.domain.exception.CommunityErrorCode;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "post")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseEntity {

    private static final String DEFAULT_REGION = "";
    private static final boolean DEFAULT_ANONYMOUS = false;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column(name = "author_challenger_id", nullable = false)
    private Long authorChallengerId;

    @Column(nullable = false)
    private String region = DEFAULT_REGION;

    @Column(nullable = false)
    private boolean anonymous = DEFAULT_ANONYMOUS;

    @Column(name = "meet_at")
    private LocalDateTime meetAt;

    private String location;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "open_chat_url")
    private String openChatUrl;

    @ElementCollection
    @BatchSize(size = 100)
    @CollectionTable(name = "post_like", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "challenger_id")
    @Getter(AccessLevel.NONE)
    private Set<Long> likedChallengerIds = new HashSet<>();

    private Post(
        String title,
        String content,
        Category category,
        Long authorChallengerId,
        LightningInfo lightningInfo
    ) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.authorChallengerId = authorChallengerId;
        applyLightningInfo(lightningInfo);
    }

    public static Post createPost(String title, String content, Category category, Long authorChallengerId) {
        if (category.isLightning()) {
            throw new CommunityDomainException(CommunityErrorCode.USE_LIGHTNING_API);
        }
        validateCommonFields(title, content);
        validateAuthorChallengerId(authorChallengerId);
        return new Post(title, content, category, authorChallengerId, null);
    }

    public static Post createLightning(String title, String content, LightningInfo info, Long authorChallengerId) {
        if (info == null) {
            throw new CommunityDomainException(CommunityErrorCode.LIGHTNING_INFO_REQUIRED);
        }

        validateCommonFields(title, content);
        validateAuthorChallengerId(authorChallengerId);
        return new Post(title, content, Category.LIGHTNING, authorChallengerId, info);
    }

    public LightningInfo getLightningInfo() {
        if (!isLightning() || meetAt == null) {
            return null;
        }
        return new LightningInfo(
            meetAt.toInstant(ZoneOffset.UTC),
            location,
            maxParticipants,
            openChatUrl
        );
    }

    public boolean isLightning() {
        return category == Category.LIGHTNING;
    }

    public LightningInfo getLightningInfoOrThrow() {
        LightningInfo lightningInfo = getLightningInfo();
        if (lightningInfo == null) {
            throw new CommunityDomainException(CommunityErrorCode.NOT_LIGHTNING_POST);
        }
        return lightningInfo;
    }

    public void update(String title, String content, Category category) {
        validateCommonFields(title, content);
        if (category == null) {
            throw new CommunityDomainException(CommunityErrorCode.INVALID_POST_CATEGORY);
        }
        if (category == Category.LIGHTNING && this.category != Category.LIGHTNING) {
            throw new CommunityDomainException(CommunityErrorCode.CANNOT_CHANGE_TO_LIGHTNING);
        }
        if (this.category == Category.LIGHTNING && category != Category.LIGHTNING) {
            throw new CommunityDomainException(CommunityErrorCode.CANNOT_CHANGE_FROM_LIGHTNING);
        }

        this.title = title;
        this.content = content;
        this.category = category;
    }

    public void updateLightning(String title, String content, LightningInfo newLightningInfo) {
        if (!isLightning()) {
            throw new CommunityDomainException(CommunityErrorCode.NOT_LIGHTNING_POST);
        }
        validateCommonFields(title, content);
        if (newLightningInfo == null) {
            throw new CommunityDomainException(CommunityErrorCode.LIGHTNING_INFO_REQUIRED);
        }

        this.title = title;
        this.content = content;
        applyLightningInfo(newLightningInfo);
    }

    public boolean toggleLike(Long challengerId) {
        if (!likedChallengerIds.remove(challengerId)) {
            likedChallengerIds.add(challengerId);
            return true;
        }
        return false;
    }

    public int getLikeCount() {
        return likedChallengerIds.size();
    }

    public boolean isLikedBy(Long challengerId) {
        return likedChallengerIds.contains(challengerId);
    }

    private void applyLightningInfo(LightningInfo lightningInfo) {
        if (lightningInfo == null) {
            meetAt = null;
            location = null;
            maxParticipants = null;
            openChatUrl = null;
            return;
        }
        meetAt = LocalDateTime.ofInstant(lightningInfo.meetAt(), ZoneOffset.UTC);
        location = lightningInfo.location();
        maxParticipants = lightningInfo.maxParticipants();
        openChatUrl = lightningInfo.openChatUrl();
    }

    private static void validateCommonFields(String title, String content) {
        if (title == null || title.isBlank()) {
            throw new CommunityDomainException(CommunityErrorCode.INVALID_POST_TITLE);
        }
        if (content == null || content.isBlank()) {
            throw new CommunityDomainException(CommunityErrorCode.INVALID_POST_CONTENT);
        }
    }

    private static void validateAuthorChallengerId(Long authorChallengerId) {
        if (authorChallengerId == null) {
            throw new CommunityDomainException(CommunityErrorCode.INVALID_POST_AUTHOR);
        }
    }

    @Builder
    public record LightningInfo(
        Instant meetAt,
        String location,
        Integer maxParticipants,
        String openChatUrl
    ) {
        public LightningInfo {
            if (meetAt == null) {
                throw new CommunityDomainException(CommunityErrorCode.INVALID_LIGHTNING_MEET_AT);
            }
            if (location == null || location.isBlank()) {
                throw new CommunityDomainException(CommunityErrorCode.INVALID_LIGHTNING_LOCATION);
            }
            if (maxParticipants == null || maxParticipants <= 0) {
                throw new CommunityDomainException(CommunityErrorCode.INVALID_LIGHTNING_MAX_PARTICIPANTS);
            }
            if (openChatUrl == null || openChatUrl.isBlank()) {
                throw new CommunityDomainException(CommunityErrorCode.INVALID_LIGHTNING_OPEN_CHAT_URL);
            }
            if (!openChatUrl.startsWith("https://") && !openChatUrl.startsWith("http://")) {
                throw new CommunityDomainException(CommunityErrorCode.INVALID_LIGHTNING_OPEN_CHAT_URL_FORMAT);
            }
        }

        public void validateMeetAtIsFuture(Instant now) {
            if (meetAt.isBefore(now)) {
                throw new CommunityDomainException(CommunityErrorCode.INVALID_LIGHTNING_MEET_AT_PAST);
            }
        }
    }
}
