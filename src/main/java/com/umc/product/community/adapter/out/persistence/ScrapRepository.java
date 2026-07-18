package com.umc.product.community.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.community.domain.Scrap;

public interface ScrapRepository extends JpaRepository<Scrap, Long> {

    Optional<Scrap> findByPost_IdAndChallengerId(Long postId, Long challengerId);

    boolean existsByPost_IdAndChallengerId(Long postId, Long challengerId);

    int countByPost_Id(Long postId);

    void deleteByPost_IdAndChallengerId(Long postId, Long challengerId);

    void deleteAllByPost_Id(Long postId);

    @Query("SELECT s.post.id FROM Scrap s WHERE s.challengerId = :challengerId ORDER BY s.createdAt DESC")
    Page<Long> findPostIdsByChallengerId(@Param("challengerId") Long challengerId, Pageable pageable);
}
