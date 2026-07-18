package com.umc.product.community.domain;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.BatchSize;

import com.umc.product.common.BaseEntity;
import com.umc.product.community.domain.exception.CommunityDomainException;
import com.umc.product.community.domain.exception.CommunityErrorCode;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false, updatable = false)
    @Getter(AccessLevel.NONE)
    private Post post;

    @Column(name = "challenger_id", nullable = false)
    private Long challengerId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "parent_id")
    private Long parentId;

    @ElementCollection
    @BatchSize(size = 100)
    @CollectionTable(name = "comment_like", joinColumns = @JoinColumn(name = "comment_id"))
    @Column(name = "challenger_id")
    @Getter(AccessLevel.NONE)
    private Set<Long> likedChallengerIds = new HashSet<>();

    private Comment(Post post, Long challengerId, String content, Long parentId) {
        this.post = post;
        this.challengerId = challengerId;
        this.content = content;
        this.parentId = parentId;
    }

    public static Comment create(Post post, Long challengerId, String content, Long parentId) {
        validateRequired(post, challengerId, content);
        return new Comment(post, challengerId, content, parentId);
    }

    public Long getPostId() {
        return post.getId();
    }

    public void updateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new CommunityDomainException(CommunityErrorCode.INVALID_COMMENT_CONTENT);
        }
        this.content = content;
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

    private static void validateRequired(Post post, Long challengerId, String content) {
        if (post == null || post.getId() == null || post.getId() <= 0) {
            throw new CommunityDomainException(CommunityErrorCode.INVALID_COMMENT_POST_ID);
        }
        if (challengerId == null || challengerId <= 0) {
            throw new CommunityDomainException(CommunityErrorCode.INVALID_COMMENT_CHALLENGER_ID);
        }
        if (content == null || content.isBlank()) {
            throw new CommunityDomainException(CommunityErrorCode.INVALID_COMMENT_CONTENT);
        }
    }

}
