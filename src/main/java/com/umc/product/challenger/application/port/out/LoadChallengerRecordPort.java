package com.umc.product.challenger.application.port.out;

import java.util.List;
import java.util.Optional;

import com.umc.product.challenger.domain.ChallengerRecord;

public interface LoadChallengerRecordPort {

    /**
     * ID로 챌린저 기록 조회
     */
    Optional<ChallengerRecord> findById(Long id);

    /**
     * ID로 챌린저 기록 조회 - 없으면 예외
     */
    ChallengerRecord getById(Long id);

    /**
     * 코드로 챌린저 기록 조회
     */
    Optional<ChallengerRecord> findByCode(String code);

    /**
     * 코드로 챌린저 기록 조회 - 없으면 예외
     */
    ChallengerRecord getByCode(String code);

    /**
     * 코드 소비가 끝날 때까지 다른 소비 요청을 대기시키고 기록을 조회합니다.
     */
    ChallengerRecord getByCodeForUpdate(String code);

    /**
     * 코드 존재 여부 확인
     */
    boolean existsByCode(String code);

    /**
     * schoolId로 챌린저 기록 목록 조회
     */
    List<ChallengerRecord> findBySchoolId(Long schoolId);

    /**
     * chapterId로 챌린저 기록 목록 조회
     */
    List<ChallengerRecord> findByChapterId(Long chapterId);
}
