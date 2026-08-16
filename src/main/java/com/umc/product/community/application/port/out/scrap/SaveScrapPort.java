package com.umc.product.community.application.port.out.scrap;

import com.umc.product.community.domain.Scrap;

public interface SaveScrapPort {
    /**
     * 스크랩 토글
     *
     * @param postId       게시글 ID
     * @param challengerId 챌린저 ID
     * @return true: 스크랩됨, false: 스크랩 취소됨
     */
    boolean toggleScrap(Long postId, Long challengerId);

    Scrap save(Scrap scrap);

    void delete(Scrap scrap);

    void deleteByPostIdAndChallengerId(Long postId, Long challengerId);

    void deleteByPostId(Long postId);
}
