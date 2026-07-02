package com.umc.product.challenger.application.port.in.query;

public interface GetUnusedChallengerRecordStatisticsUseCase {

    /**
     * 아직 사용되지 않은(isUsed=false) 챌린저 기록 코드의 전체 개수를 반환합니다.
     */
    long getUnusedRecordCount();
}
