package com.umc.product.community.adapter.out.persistence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.umc.product.community.application.port.in.command.comment.ToggleCommentLikeUseCase.LikeResult;
import com.umc.product.community.application.port.out.comment.LoadCommentPort;
import com.umc.product.community.application.port.out.comment.SaveCommentPort;
import com.umc.product.community.domain.Comment;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CommentPersistenceAdapter implements LoadCommentPort, SaveCommentPort {

    private final CommentRepository commentRepository;

    @Override
    public Optional<Comment> findById(Long commentId) {
        return commentRepository.findById(commentId);
    }

    @Override
    public Page<Comment> findByPostId(Long postId, Pageable pageable) {
        return commentRepository.findByPostIdOrderByCreatedAtDesc(postId, pageable);
    }

    @Override
    public int countByPostId(Long postId) {
        return commentRepository.countByPostId(postId);
    }

    @Override
    public Map<Long, Integer> countByPostIds(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return new HashMap<>();
        }

        List<Object[]> results = commentRepository.countByPostIdIn(postIds);
        return results.stream()
            .collect(Collectors.toMap(
                row -> (Long) row[0],           // postId
                row -> ((Number) row[1]).intValue()  // count
            ));
    }

    @Override
    public Comment save(Comment comment) {
        return commentRepository.save(comment);
    }

    @Override
    public void delete(Comment comment) {
        if (comment.getId() != null) {
            commentRepository.deleteById(comment.getId());
        }
    }

    @Override
    public LikeResult toggleLike(Long commentId, Long challengerId) {
        Comment entity = commentRepository.findById(commentId)
            .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
        boolean liked = entity.toggleLike(challengerId);
        return new LikeResult(liked, entity.getLikeCount());
    }
}
