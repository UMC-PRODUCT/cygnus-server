package com.umc.product.community.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.umc.product.community.application.port.in.command.comment.ToggleCommentLikeUseCase.LikeResult;
import com.umc.product.community.domain.Comment;
import com.umc.product.community.domain.Post;
import com.umc.product.community.domain.enums.Category;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@Import(CommentPersistenceAdapter.class)
@DisplayName("CommentPersistenceAdapter")
class CommentPersistenceAdapterTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    CommentPersistenceAdapter sut;

    @Test
    @DisplayName("대댓글 parentId와 좋아요를 직접 JPA 도메인으로 저장한다")
    void 대댓글_parent_id와_좋아요를_직접_jpa_도메인으로_저장한다() {
        // given
        Post post = em.persist(Post.createPost("글", "본문", Category.FREE, 201L));
        Comment parent = em.persist(Comment.create(post, 202L, "부모", null));
        Comment child = sut.save(Comment.create(post, 203L, "자식", parent.getId()));

        // when
        LikeResult result = sut.toggleLike(child.getId(), 204L);
        em.flush();
        Long childId = child.getId();
        em.clear();
        Comment reloaded = sut.findById(childId).orElseThrow();

        // then
        assertThat(reloaded.getPostId()).isEqualTo(post.getId());
        assertThat(reloaded.getParentId()).isEqualTo(parent.getId());
        assertThat(reloaded.getContent()).isEqualTo("자식");
        assertThat(reloaded.getCreatedAt()).isNotNull();
        assertThat(result.liked()).isTrue();
        assertThat(result.likeCount()).isOne();
        assertThat(reloaded.isLikedBy(204L)).isTrue();
    }
}
