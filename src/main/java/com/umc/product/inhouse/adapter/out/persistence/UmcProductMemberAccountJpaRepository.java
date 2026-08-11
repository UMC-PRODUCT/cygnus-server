package com.umc.product.inhouse.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.inhouse.domain.UmcProductMemberAccount;

public interface UmcProductMemberAccountJpaRepository extends JpaRepository<UmcProductMemberAccount, Long> {

    @Query("""
        SELECT a
        FROM UmcProductMemberAccount a
        JOIN FETCH a.umcProductMember
        WHERE a.memberId = :memberId
        """)
    Optional<UmcProductMemberAccount> findByMemberId(@Param("memberId") Long memberId);

    @Query("""
        SELECT a
        FROM UmcProductMemberAccount a
        JOIN FETCH a.umcProductMember
        WHERE a.umcProductMember.id = :umcProductMemberId
        ORDER BY a.id ASC
        """)
    List<UmcProductMemberAccount> findAllByUmcProductMemberId(
        @Param("umcProductMemberId") Long umcProductMemberId
    );

    boolean existsByMemberId(Long memberId);

    boolean existsByUmcProductMemberIdAndMemberId(Long umcProductMemberId, Long memberId);

    @Modifying
    @Query("DELETE FROM UmcProductMemberAccount a WHERE a.umcProductMember.id = :umcProductMemberId")
    void deleteAllByUmcProductMemberId(@Param("umcProductMemberId") Long umcProductMemberId);
}
