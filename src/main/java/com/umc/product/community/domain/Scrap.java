package com.umc.product.community.domain;

import com.umc.product.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "scrap",
    uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "challenger_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Scrap extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false, updatable = false)
    @Getter(AccessLevel.NONE)
    private Post post;

    @Column(name = "challenger_id", nullable = false)
    private Long challengerId;

    private Scrap(Post post, Long challengerId) {
        this.post = post;
        this.challengerId = challengerId;
    }

    public static Scrap create(Post post, Long challengerId) {
        validatePost(post);
        validateChallengerId(challengerId);
        return new Scrap(post, challengerId);
    }

    public Long getPostId() {
        return post.getId();
    }

    private static void validatePost(Post post) {
        if (post == null || post.getId() == null || post.getId() <= 0) {
            throw new IllegalArgumentException("게시글 ID는 필수이며 양수여야 합니다.");
        }
    }

    private static void validateChallengerId(Long challengerId) {
        if (challengerId == null || challengerId <= 0) {
            throw new IllegalArgumentException("챌린저 ID는 필수이며 양수여야 합니다.");
        }
    }

}
