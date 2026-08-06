package com.umc.product.community.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.umc.product.community.application.port.in.command.post.TogglePostLikeUseCase.LikeResult;
import com.umc.product.community.application.port.out.dto.PostWithAuthor;
import com.umc.product.community.domain.Post;
import com.umc.product.community.domain.enums.Category;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@Import({PostPersistenceAdapter.class, PostQueryRepository.class})
@DisplayName("PostPersistenceAdapter")
class PostPersistenceAdapterTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    PostPersistenceAdapter sut;

    @Test
    @DisplayName("Post 도메인을 별도 매퍼 없이 저장하고 조회한다")
    void post_도메인을_별도_매퍼_없이_저장하고_조회한다() {
        // given
        Post post = Post.createPost("직접 저장", "도메인 객체", Category.FREE, 101L);

        // when
        Post saved = sut.save(post);
        em.flush();
        Long postId = saved.getId();
        em.clear();
        Post reloaded = sut.findById(postId).orElseThrow();

        // then
        assertThat(reloaded.getId()).isEqualTo(postId);
        assertThat(reloaded.getTitle()).isEqualTo("직접 저장");
        assertThat(reloaded.getAuthorChallengerId()).isEqualTo(101L);
        assertThat(reloaded.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Post 직접 엔티티 수정값은 flush-clear 후에도 유지된다")
    void post_직접_엔티티_수정값은_flush_clear_후에도_유지된다() {
        // given
        Post saved = sut.save(Post.createPost("수정 전", "수정 전 본문", Category.FREE, 121L));
        em.flush();
        Long postId = saved.getId();
        em.clear();
        Post post = sut.findById(postId).orElseThrow();

        // when
        post.update("수정 후", "수정 후 본문", Category.QUESTION);
        sut.save(post);
        em.flush();
        em.clear();
        Post reloaded = sut.findById(postId).orElseThrow();

        // then
        assertThat(reloaded.getTitle()).isEqualTo("수정 후");
        assertThat(reloaded.getContent()).isEqualTo("수정 후 본문");
        assertThat(reloaded.getCategory()).isEqualTo(Category.QUESTION);
    }

    @Test
    @DisplayName("Post 직접 엔티티를 삭제하면 flush-clear 후 조회되지 않는다")
    void post_직접_엔티티를_삭제하면_flush_clear_후_조회되지_않는다() {
        // given
        Post saved = sut.save(Post.createPost("삭제할 글", "삭제할 본문", Category.FREE, 131L));
        em.flush();
        Long postId = saved.getId();
        em.clear();
        Post post = sut.findById(postId).orElseThrow();

        // when
        sut.delete(post);
        em.flush();
        em.clear();

        // then
        assertThat(sut.findById(postId)).isEmpty();
    }

    @Test
    @DisplayName("조회자별 좋아요 상태는 Post가 아닌 조회 DTO에 담긴다")
    void 조회자별_좋아요_상태는_post가_아닌_조회_dto에_담긴다() {
        // given
        Post post = sut.save(Post.createPost("좋아요", "본문", Category.FREE, 111L));
        LikeResult result = sut.toggleLike(post.getId(), 112L);
        em.flush();
        em.clear();

        // when
        PostWithAuthor likedViewer = sut.findByIdWithAuthor(post.getId(), 112L).orElseThrow();
        PostWithAuthor otherViewer = sut.findByIdWithAuthor(post.getId(), 113L).orElseThrow();

        // then
        assertThat(result.liked()).isTrue();
        assertThat(result.likeCount()).isOne();
        assertThat(likedViewer.liked()).isTrue();
        assertThat(otherViewer.liked()).isFalse();
        assertThat(likedViewer.post().getId()).isEqualTo(otherViewer.post().getId());
    }
}
