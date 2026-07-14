package com.umc.product.organization.adapter.out.persistence.chapter;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.umc.product.organization.domain.Chapter;

public interface ChapterJpaRepository extends Repository<Chapter, Long> {

    boolean existsById(Long chapterId);

    Optional<Chapter> findById(Long chapterId);

    @Query("SELECT c FROM Chapter c " +
        "JOIN FETCH c.gisu " +
        "WHERE c.id = :chapterId")
    Optional<Chapter> findByIdWithGisu(@Param("chapterId") Long chapterId);

    Chapter save(Chapter chapter);

    List<Chapter> findAll();

    List<Chapter> findByGisuId(Long gisuId);

    @Query("SELECT c FROM Chapter c " +
        "JOIN FETCH c.gisu " +
        "WHERE c.gisu.id IN :gisuIds")
    List<Chapter> findByGisuIdInWithGisu(@Param("gisuIds") Set<Long> gisuIds);

    boolean existsByGisuId(Long gisuId);

    boolean existsByIdAndGisuId(Long chapterId, Long gisuId);

    void delete(Chapter chapter);
}
