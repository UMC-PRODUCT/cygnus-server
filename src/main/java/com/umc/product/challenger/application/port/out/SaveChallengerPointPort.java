package com.umc.product.challenger.application.port.out;

import java.util.List;

import com.umc.product.challenger.domain.ChallengerPoint;

public interface SaveChallengerPointPort {

    /**
     * 챌린저 포인트 저장
     */
    ChallengerPoint save(ChallengerPoint challengerPoint);

    List<ChallengerPoint> saveAll(List<ChallengerPoint> challengerPoints);

    void deleteAllByChallengerId(Long challengerId);

    /**
     * 챌린저 포인트 삭제
     */
    void delete(ChallengerPoint challengerPoint);
}
