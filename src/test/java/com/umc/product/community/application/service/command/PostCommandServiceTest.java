package com.umc.product.community.application.service.command;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.community.application.port.out.comment.SaveCommentPort;
import com.umc.product.community.application.port.out.post.LoadPostPort;
import com.umc.product.community.application.port.out.post.SavePostPort;
import com.umc.product.community.application.port.out.scrap.SaveScrapPort;
import com.umc.product.community.application.service.AuthorInfoProvider;
import com.umc.product.community.domain.Post;
import com.umc.product.community.domain.enums.Category;
import com.umc.product.community.domain.exception.CommunityDomainException;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostCommandService")
class PostCommandServiceTest {

    @Mock
    LoadPostPort loadPostPort;

    @Mock
    SavePostPort savePostPort;

    @Mock
    SaveCommentPort saveCommentPort;

    @Mock
    SaveScrapPort saveScrapPort;

    @Mock
    AuthorInfoProvider authorInfoProvider;

    @InjectMocks
    PostCommandService sut;

    @Test
    @DisplayName("게시글 삭제는 댓글과 스크랩을 먼저 정리하고 Post를 삭제한다")
    void deletePostAfterCommentsAndScraps() {
        // given
        Long postId = 1L;
        Post post = Post.createPost("제목", "본문", Category.FREE, 2L);
        when(loadPostPort.findById(postId)).thenReturn(Optional.of(post));

        // when
        sut.deletePost(postId);

        // then
        InOrder ordered = inOrder(loadPostPort, saveCommentPort, saveScrapPort, savePostPort);
        ordered.verify(loadPostPort).findById(postId);
        ordered.verify(saveCommentPort).deleteByPostId(postId);
        ordered.verify(saveScrapPort).deleteByPostId(postId);
        ordered.verify(savePostPort).delete(post);
    }

    @Test
    @DisplayName("존재하지 않는 게시글은 자식 데이터를 삭제하지 않는다")
    void rejectMissingPostBeforeDeletingChildren() {
        // given
        Long postId = 1L;
        when(loadPostPort.findById(postId)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> sut.deletePost(postId))
            .isInstanceOf(CommunityDomainException.class);
        verifyNoInteractions(savePostPort, saveCommentPort, saveScrapPort);
    }
}
