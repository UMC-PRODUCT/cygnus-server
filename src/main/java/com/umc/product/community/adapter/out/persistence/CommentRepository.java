package com.umc.product.community.adapter.out.persistence;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.community.domain.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByPostIdOrderByCreatedAtDesc(Long postId, Pageable pageable);

    int countByPostId(Long postId);

    @Query("SELECT c.postId, COUNT(c) FROM Comment c WHERE c.postId IN :postIds GROUP BY c.postId")
    List<Object[]> countByPostIdIn(@Param("postIds") List<Long> postIds);
}
