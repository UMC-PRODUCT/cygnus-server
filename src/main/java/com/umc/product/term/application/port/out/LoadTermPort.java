package com.umc.product.term.application.port.out;

import java.util.List;
import java.util.Optional;

import com.umc.product.term.domain.Term;
import com.umc.product.term.domain.enums.TermType;

public interface LoadTermPort {
    /**
     * ID로 약관을 조회합니다.
     */
    Optional<Term> findById(Long id);

    /**
     * 재동의 검증과 저장이 끝날 때까지 약관 비활성화와 직렬화되도록 shared lock으로 조회합니다.
     */
    Optional<Term> findByIdWithSharedLock(Long id);

    /**
     * 특정 타입의 활성화된 약관을 조회합니다. 여러 개일 경우 가장 최신 것을 반환합니다.
     */
    Optional<Term> findActiveByType(TermType type);

    /**
     * 현재 활성화된 모든 약관을 조회합니다.
     */
    List<Term> listActive();

    /**
     * ID로 약관이 존재하는지 확인합니다.
     */
    boolean existsById(Long id);

    /**
     * 전달받은 타입들에 해당하는 활성 약관을 한 번에 조회합니다.
     */
    List<Term> findAllActiveByTypes(List<TermType> types);

    /**
     * 전달받은 ID에 해당하는 약관을 한 번에 조회합니다.
     */
    List<Term> listByIds(List<Long> ids);

    /**
     * 현재 활성화된 필수 약관 목록을 조회합니다.
     */
    List<Term> findAllActiveRequired();
}
