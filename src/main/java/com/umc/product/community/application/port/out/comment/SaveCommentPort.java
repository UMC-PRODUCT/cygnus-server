package com.umc.product.community.application.port.out.comment;

import com.umc.product.community.application.port.in.command.comment.ToggleCommentLikeUseCase.LikeResult;
import com.umc.product.community.domain.Comment;

public interface SaveCommentPort {
    Comment save(Comment comment);

    void delete(Comment comment);

    void deleteByPostId(Long postId);

    LikeResult toggleLike(Long commentId, Long challengerId);
}
