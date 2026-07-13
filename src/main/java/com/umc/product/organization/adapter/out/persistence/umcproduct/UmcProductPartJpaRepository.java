package com.umc.product.organization.adapter.out.persistence.umcproduct;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.organization.domain.UmcProductPart;

import jakarta.persistence.LockModeType;

public interface UmcProductPartJpaRepository extends JpaRepository<UmcProductPart, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM UmcProductPart p JOIN FETCH p.chapter WHERE p.id = :partId")
    java.util.Optional<UmcProductPart> findByIdWithLock(@Param("partId") Long partId);

    @Query("""
        SELECT p
        FROM UmcProductPart p
        JOIN FETCH p.chapter chapter
        WHERE (:chapterId IS NULL OR chapter.id = :chapterId)
          AND (:active IS NULL OR p.isActive = :active)
        ORDER BY chapter.sortOrder ASC, chapter.id ASC, p.sortOrder ASC, p.id ASC
        """)
    List<UmcProductPart> findAll(
        @Param("chapterId") Long chapterId,
        @Param("active") Boolean active
    );

    @Query("""
        SELECT p
        FROM UmcProductPart p
        JOIN FETCH p.chapter chapter
        WHERE chapter.id IN :chapterIds
          AND (:active IS NULL OR p.isActive = :active)
        ORDER BY chapter.sortOrder ASC, chapter.id ASC, p.sortOrder ASC, p.id ASC
        """)
    List<UmcProductPart> findAllByChapterIds(
        @Param("chapterIds") Collection<Long> chapterIds,
        @Param("active") Boolean active
    );

    @Query("SELECT p FROM UmcProductPart p JOIN FETCH p.chapter WHERE p.id IN :partIds")
    List<UmcProductPart> findByIdIn(@Param("partIds") Collection<Long> partIds);

    boolean existsByChapterId(Long chapterId);

    @Query("""
        SELECT COUNT(p) > 0
        FROM UmcProductPart p
        WHERE p.chapter.id = :chapterId
          AND p.code = :code
          AND (:excludedPartId IS NULL OR p.id <> :excludedPartId)
        """)
    boolean existsByChapterIdAndCode(
        @Param("chapterId") Long chapterId,
        @Param("code") String code,
        @Param("excludedPartId") Long excludedPartId
    );
}
