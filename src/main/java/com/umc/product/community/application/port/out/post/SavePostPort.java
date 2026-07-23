package com.umc.product.community.application.port.out.post;

import com.umc.product.community.application.port.in.command.post.TogglePostLikeUseCase.LikeResult;
import com.umc.product.community.domain.Post;

public interface SavePostPort {
    Post save(Post post);

    void delete(Post post);

    void deleteById(Long postId);

    LikeResult toggleLike(Long postId, Long challengerId);
}
