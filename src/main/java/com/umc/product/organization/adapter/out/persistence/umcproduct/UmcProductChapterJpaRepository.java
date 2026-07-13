package com.umc.product.organization.adapter.out.persistence.umcproduct;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.organization.domain.UmcProductChapter;

import jakarta.persistence.LockModeType;

public interface UmcProductChapterJpaRepository extends JpaRepository<UmcProductChapter, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM UmcProductChapter c WHERE c.id = :chapterId")
    java.util.Optional<UmcProductChapter> findByIdWithLock(@Param("chapterId") Long chapterId);

    @Query("""
        SELECT c
        FROM UmcProductChapter c
        WHERE :active IS NULL OR c.isActive = :active
        ORDER BY c.sortOrder ASC, c.id ASC
        """)
    List<UmcProductChapter> findAll(@Param("active") Boolean active);

    List<UmcProductChapter> findByIdIn(Collection<Long> chapterIds);

    @Query("""
        SELECT COUNT(c) > 0
        FROM UmcProductChapter c
        WHERE c.code = :code
          AND (:excludedChapterId IS NULL OR c.id <> :excludedChapterId)
        """)
    boolean existsByCode(
        @Param("code") String code,
        @Param("excludedChapterId") Long excludedChapterId
    );
}
