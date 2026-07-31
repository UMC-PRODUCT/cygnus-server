package com.umc.product.community.adapter.out.persistence;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.community.domain.Post;
import com.umc.product.community.domain.enums.Category;

public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findByCategory(Category category);

    @Query("SELECT p.id as postId, p.authorChallengerId as authorId FROM Post p WHERE p.id IN :postIds")
    List<PostAuthorProjection> findAuthorIdsByPostIds(@Param("postIds") List<Long> postIds);

    default Map<Long, Long> findAuthorIdsMapByPostIds(List<Long> postIds) {
        return findAuthorIdsByPostIds(postIds).stream()
            .collect(Collectors.toMap(
                PostAuthorProjection::getPostId,
                PostAuthorProjection::getAuthorId
            ));
    }

    interface PostAuthorProjection {
        Long getPostId();

        Long getAuthorId();
    }
}
