package com.umc.product.community.application.port.out.dto;

import com.umc.product.community.domain.Post;

/**
 * Post와 작성자 챌린저 ID를 함께 담는 DTO 중복 조회를 방지하기 위해 사용됩니다.
 */
public record PostWithAuthor(
    Post post,
    Long authorChallengerId,
    boolean liked
) {
    public PostWithAuthor(Post post, Long authorChallengerId) {
        this(post, authorChallengerId, false);
    }
}
