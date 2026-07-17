package com.umc.product.community.domain;

import com.umc.product.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "challenger_id", nullable = false)
    private Long challengerId;

    private Scrap(Long postId, Long challengerId) {
        this.postId = postId;
        this.challengerId = challengerId;
    }

    public static Scrap create(Long postId, Long challengerId) {
        validatePostId(postId);
        validateChallengerId(challengerId);
        return new Scrap(postId, challengerId);
    }

    private static void validatePostId(Long postId) {
        if (postId == null || postId <= 0) {
            throw new IllegalArgumentException("게시글 ID는 필수이며 양수여야 합니다.");
        }
    }

    private static void validateChallengerId(Long challengerId) {
        if (challengerId == null || challengerId <= 0) {
            throw new IllegalArgumentException("챌린저 ID는 필수이며 양수여야 합니다.");
        }
    }

}
