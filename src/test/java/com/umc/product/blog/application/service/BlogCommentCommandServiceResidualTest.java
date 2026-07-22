package com.umc.product.blog.application.service;

import static com.umc.product.support.fixture.BlogUnitFixture.content;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.blog.application.port.in.command.dto.CreateBlogCommentCommand;
import com.umc.product.blog.application.port.out.LoadBlogCommentPort;
import com.umc.product.blog.application.port.out.LoadBlogContentPort;
import com.umc.product.blog.application.port.out.LoadBlogLikePort;
import com.umc.product.blog.application.port.out.SaveBlogCommentPort;
import com.umc.product.blog.application.port.out.SaveBlogLikePort;
import com.umc.product.blog.domain.BlogComment;
import com.umc.product.blog.domain.BlogContent;
import com.umc.product.blog.domain.BlogDomainException;

@ExtendWith(MockitoExtension.class)
@DisplayName("Blog 댓글 command 잔여 경계 테스트")
class BlogCommentCommandServiceResidualTest {

    @Mock
    LoadBlogContentPort loadBlogContentPort;

    @Mock
    LoadBlogCommentPort loadBlogCommentPort;

    @Mock
    SaveBlogCommentPort saveBlogCommentPort;

    @Mock
    LoadBlogLikePort loadBlogLikePort;

    @Mock
    SaveBlogLikePort saveBlogLikePort;

    @Mock
    BlogCommentInfoAssembler commentInfoAssembler;

    @Mock
    GetChallengerRoleUseCase getChallengerRoleUseCase;

    @InjectMocks
    BlogCommentCommandService service;

    @Test
    @DisplayName("대댓글에 다시 답글을 작성하는 2단계 댓글은 거부한다")
    void 이단계_댓글은_거부한다() {
        BlogContent target = content();
        ReflectionTestUtils.setField(target, "id", 10L);
        BlogComment reply = BlogComment.create(10L, 1L, 1L, false, null, "대댓글");
        ReflectionTestUtils.setField(reply, "id", 20L);
        given(loadBlogContentPort.findPublishedByTypeAndSlug(target.getContentType(), target.getSlug()))
            .willReturn(Optional.of(target));
        given(loadBlogCommentPort.getByIdAndContentId(20L, 10L)).willReturn(reply);
        CreateBlogCommentCommand command = CreateBlogCommentCommand.of(
            "engineering", target.getSlug(), 20L, 1L, false, null, "2단계 댓글"
        );

        assertThatThrownBy(() -> service.create(command)).isInstanceOf(BlogDomainException.class);
    }
}
