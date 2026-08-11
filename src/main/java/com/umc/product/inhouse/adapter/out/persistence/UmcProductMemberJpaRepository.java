package com.umc.product.inhouse.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.inhouse.domain.UmcProductMember;

import jakarta.persistence.LockModeType;

public interface UmcProductMemberJpaRepository extends JpaRepository<UmcProductMember, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM UmcProductMember m WHERE m.id = :umcProductMemberId")
    Optional<UmcProductMember> findByIdWithLock(
        @Param("umcProductMemberId") Long umcProductMemberId
    );

    List<UmcProductMember> findByIdIn(Collection<Long> ids);
}
