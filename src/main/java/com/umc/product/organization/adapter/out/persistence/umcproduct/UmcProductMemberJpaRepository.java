package com.umc.product.organization.adapter.out.persistence.umcproduct;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.organization.domain.UmcProductMember;

import jakarta.persistence.LockModeType;

public interface UmcProductMemberJpaRepository extends JpaRepository<UmcProductMember, Long> {

    Optional<UmcProductMember> findByMemberId(Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM UmcProductMember m WHERE m.id = :umcProductMemberId")
    Optional<UmcProductMember> findByIdWithLock(
        @Param("umcProductMemberId") Long umcProductMemberId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM UmcProductMember m WHERE m.memberId = :memberId")
    Optional<UmcProductMember> findByMemberIdWithLock(@Param("memberId") Long memberId);

    boolean existsByMemberId(Long memberId);

    List<UmcProductMember> findByIdIn(Collection<Long> ids);
}
