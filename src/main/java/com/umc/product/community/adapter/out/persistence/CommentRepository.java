package com.umc.product.community.adapter.out.persistence;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.community.domain.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByPost_IdOrderByCreatedAtDesc(Long postId, Pageable pageable);

    int countByPost_Id(Long postId);

    @Query("SELECT c.post.id, COUNT(c) FROM Comment c WHERE c.post.id IN :postIds GROUP BY c.post.id")
    List<Object[]> countByPostIdIn(@Param("postIds") List<Long> postIds);

    void deleteAllByPost_Id(Long postId);
}
