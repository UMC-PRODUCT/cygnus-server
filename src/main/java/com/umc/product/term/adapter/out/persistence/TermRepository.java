package com.umc.product.term.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.term.domain.Term;
import com.umc.product.term.domain.enums.TermType;

import jakarta.persistence.LockModeType;

public interface TermRepository extends JpaRepository<Term, Long> {

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT term FROM Term term WHERE term.id = :id")
    Optional<Term> findByIdWithSharedLock(@Param("id") Long id);

    /**
     * IN 절을 사용하여 전달받은 타입들에 해당하는 활성 약관을 한 번에 조회합니다.
     */
    List<Term> findAllByTypeInAndActiveIsTrue(List<TermType> types);

    /**
     * 현재 활성화된 모든 약관을 ID 오름차순으로 조회합니다.
     */
    List<Term> findAllByActiveIsTrueOrderByIdAsc();
}
