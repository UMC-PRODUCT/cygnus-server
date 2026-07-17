package com.umc.product.community.adapter.out.persistence;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.umc.product.community.application.port.in.command.post.TogglePostLikeUseCase.LikeResult;
import com.umc.product.community.application.port.in.query.dto.PostSearchQuery;
import com.umc.product.community.application.port.out.dto.PostWithAuthor;
import com.umc.product.community.application.port.out.post.LoadPostPort;
import com.umc.product.community.application.port.out.post.SavePostPort;
import com.umc.product.community.domain.Post;
import com.umc.product.community.domain.enums.Category;
import com.umc.product.community.domain.exception.CommunityDomainException;
import com.umc.product.community.domain.exception.CommunityErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PostPersistenceAdapter implements LoadPostPort, SavePostPort {

    private final PostRepository postRepository;
    private final PostQueryRepository postQueryRepository;

    // ================= SavePostPort 구현 =================

    @Override
    public Post save(Post post) {
        return postRepository.save(post);
    }

    @Override
    public void delete(Post post) {
        if (post.getId() != null) {
            postRepository.deleteById(post.getId());
        }
    }

    @Override
    public void deleteById(Long postId) {
        postRepository.deleteById(postId);
    }

    // ================= LoadPostPort 구현 =================

    @Override
    public Page<Post> findAllByQuery(PostSearchQuery query, Pageable pageable) {
        return postQueryRepository.findAllByQuery(query, pageable);
    }

    @Override
    public Optional<Post> findById(Long postId) {
        return postRepository.findById(postId);
    }

    @Override
    public Page<Post> findByAuthorChallengerId(Long challengerId, Pageable pageable) {
        return postQueryRepository.findByAuthorChallengerId(challengerId, pageable);
    }

    @Override
    public Page<Post> findCommentedPostsByChallengerId(Long challengerId, Pageable pageable) {
        return postQueryRepository.findCommentedPostsByChallengerId(challengerId, pageable);
    }

    @Override
    public Page<Post> findScrappedPostsByChallengerId(Long challengerId, Pageable pageable) {
        return postQueryRepository.findScrappedPostsByChallengerId(challengerId, pageable);
    }

    @Override
    public Optional<PostWithAuthor> findByIdWithAuthor(Long postId) {
        return postRepository.findById(postId)
            .map(post -> new PostWithAuthor(post, post.getAuthorChallengerId()));
    }

    @Override
    public Optional<PostWithAuthor> findByIdWithAuthor(Long postId, Long viewerChallengerId) {
        return postRepository.findById(postId)
            .map(post -> new PostWithAuthor(
                post,
                post.getAuthorChallengerId(),
                viewerChallengerId != null && post.isLikedBy(viewerChallengerId)
            ));
    }

    @Override
    public List<Post> findByCategory(Category category) {
        return postRepository.findByCategory(category);
    }

    @Override
    public LikeResult toggleLike(Long postId, Long challengerId) {
        Post entity = postRepository.findById(postId)
            .orElseThrow(() -> new CommunityDomainException(CommunityErrorCode.POST_NOT_FOUND));
        boolean liked = entity.toggleLike(challengerId);
        return new LikeResult(liked, entity.getLikeCount());
    }

    @Override
    public Page<Post> searchByKeyword(String keyword, Pageable pageable) {
        return postQueryRepository.searchByKeyword(keyword, pageable);
    }

    @Override
    public Long findAuthorIdByPostId(Long postId) {
        return postRepository.findById(postId)
            .map(Post::getAuthorChallengerId)
            .orElseThrow(() -> new CommunityDomainException(CommunityErrorCode.POST_NOT_FOUND));
    }

    @Override
    public Map<Long, Long> findAuthorIdsByPostIds(List<Long> postIds) {
        return postRepository.findAuthorIdsMapByPostIds(postIds);
    }
}
