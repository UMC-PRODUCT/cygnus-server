package com.umc.product.challenger.application.port.out;

import com.umc.product.challenger.application.port.in.query.dto.ListChallengerRecordsQuery;
import com.umc.product.challenger.domain.ChallengerRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;

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

    /**
     * 기수/학교/파트/역할 동적 조건으로 챌린저 기록 목록을 페이지 조회
     */
    Page<ChallengerRecord> search(ListChallengerRecordsQuery query);
}
