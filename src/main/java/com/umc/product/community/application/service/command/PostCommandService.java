package com.umc.product.community.application.service.command;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.community.application.port.in.command.post.CreatePostUseCase;
import com.umc.product.community.application.port.in.command.post.DeletePostUseCase;
import com.umc.product.community.application.port.in.command.post.TogglePostLikeUseCase;
import com.umc.product.community.application.port.in.command.post.UpdateLightningUseCase;
import com.umc.product.community.application.port.in.command.post.UpdatePostUseCase;
import com.umc.product.community.application.port.in.command.post.dto.CreateLightningCommand;
import com.umc.product.community.application.port.in.command.post.dto.CreatePostCommand;
import com.umc.product.community.application.port.in.command.post.dto.UpdateLightningCommand;
import com.umc.product.community.application.port.in.command.post.dto.UpdatePostCommand;
import com.umc.product.community.application.port.in.query.dto.PostInfo;
import com.umc.product.community.application.port.out.comment.SaveCommentPort;
import com.umc.product.community.application.port.out.dto.PostWithAuthor;
import com.umc.product.community.application.port.out.post.LoadPostPort;
import com.umc.product.community.application.port.out.post.SavePostPort;
import com.umc.product.community.application.port.out.scrap.SaveScrapPort;
import com.umc.product.community.application.service.AuthorInfoProvider;
import com.umc.product.community.domain.Post;
import com.umc.product.community.domain.Post.LightningInfo;
import com.umc.product.community.domain.exception.CommunityDomainException;
import com.umc.product.community.domain.exception.CommunityErrorCode;
import com.umc.product.global.exception.constant.Domain;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PostCommandService implements CreatePostUseCase, UpdatePostUseCase, UpdateLightningUseCase,
    DeletePostUseCase, TogglePostLikeUseCase {

    private final LoadPostPort loadPostPort;
    private final SavePostPort savePostPort;
    private final SaveCommentPort saveCommentPort;
    private final SaveScrapPort saveScrapPort;
    private final AuthorInfoProvider authorInfoProvider;

    @Audited(
        domain = Domain.COMMUNITY,
        action = AuditAction.CREATE,
        targetType = "Post",
        targetId = "#result.postId()",
        description = "'커뮤니티 게시글을 생성했습니다.'"
    )
    @Override
    public PostInfo createPost(CreatePostCommand command) {
        Post post = Post.createPost(
            command.title(),
            command.content(),
            command.category(),
            command.authorChallengerId()
        );

        Post savedPost = savePostPort.save(post);
        String authorName = authorInfoProvider.getAuthorName(command.authorChallengerId());
        return PostInfo.from(savedPost, command.authorChallengerId(), authorName);
    }

    @Audited(
        domain = Domain.COMMUNITY,
        action = AuditAction.CREATE,
        targetType = "Post",
        targetId = "#result.postId()",
        description = "'커뮤니티 번개 게시글을 생성했습니다.'"
    )
    @Override
    public PostInfo createLightningPost(CreateLightningCommand command) {
        LightningInfo lightningInfo = new LightningInfo(
            command.meetAt(),
            command.location(),
            command.maxParticipants(),
            command.openChatUrl()
        );

        // Service 레이어에서 시간 검증 (테스트 용이성)
        lightningInfo.validateMeetAtIsFuture(Instant.now());

        Post post = Post.createLightning(
            command.title(),
            command.content(),
            lightningInfo,
            command.authorChallengerId()
        );

        Post savedPost = savePostPort.save(post);
        String authorName = authorInfoProvider.getAuthorName(command.authorChallengerId());
        return PostInfo.from(savedPost, command.authorChallengerId(), authorName);
    }

    @Audited(
        domain = Domain.COMMUNITY,
        action = AuditAction.UPDATE,
        targetType = "Post",
        targetId = "#command.postId()",
        description = "'커뮤니티 게시글을 수정했습니다.'"
    )
    @Override
    public PostInfo updatePost(UpdatePostCommand command) {
        PostWithAuthor postWithAuthor = loadPostPort.findByIdWithAuthor(command.postId())
            .orElseThrow(() -> new CommunityDomainException(CommunityErrorCode.POST_NOT_FOUND));

        Post post = postWithAuthor.post();
        post.update(
            command.title(),
            command.content(),
            command.category()
        );

        Post savedPost = savePostPort.save(post);
        String authorName = authorInfoProvider.getAuthorName(postWithAuthor.authorChallengerId());
        return PostInfo.from(savedPost, postWithAuthor.authorChallengerId(), authorName);
    }

    @Audited(
        domain = Domain.COMMUNITY,
        action = AuditAction.UPDATE,
        targetType = "Post",
        targetId = "#command.postId()",
        description = "'커뮤니티 번개 게시글을 수정했습니다.'"
    )
    @Override
    public PostInfo updateLightning(UpdateLightningCommand command) {
        PostWithAuthor postWithAuthor = loadPostPort.findByIdWithAuthor(command.postId())
            .orElseThrow(() -> new CommunityDomainException(CommunityErrorCode.POST_NOT_FOUND));

        Post post = postWithAuthor.post();

        LightningInfo lightningInfo = new LightningInfo(
            command.meetAt(),
            command.location(),
            command.maxParticipants(),
            command.openChatUrl()
        );

        // Service 레이어에서 시간 검증 (테스트 용이성)
        lightningInfo.validateMeetAtIsFuture(Instant.now());

        post.updateLightning(
            command.title(),
            command.content(),
            lightningInfo
        );

        Post savedPost = savePostPort.save(post);
        String authorName = authorInfoProvider.getAuthorName(postWithAuthor.authorChallengerId());
        return PostInfo.from(savedPost, postWithAuthor.authorChallengerId(), authorName);
    }

    @Audited(
        domain = Domain.COMMUNITY,
        action = AuditAction.DELETE,
        targetType = "Post",
        targetId = "#postId",
        description = "'커뮤니티 게시글을 삭제했습니다.'"
    )
    @Override
    public void deletePost(Long postId) {
        Post post = loadPostPort.findById(postId)
            .orElseThrow(() -> new CommunityDomainException(CommunityErrorCode.POST_NOT_FOUND));

        saveCommentPort.deleteByPostId(postId);
        saveScrapPort.deleteByPostId(postId);
        savePostPort.delete(post);
    }

    @Override
    public LikeResult toggleLike(Long postId, Long challengerId) {
        loadPostPort.findById(postId)
            .orElseThrow(() -> new CommunityDomainException(CommunityErrorCode.POST_NOT_FOUND));

        return savePostPort.toggleLike(postId, challengerId);
    }
}
