package com.umc.product.community.application.service.evaluator;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.community.application.port.in.query.GetCommentListUseCase;
import com.umc.product.community.application.port.in.query.dto.CommentInfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class CommunityCommentPermissionEvaluator implements ResourcePermissionEvaluator {

    private final GetChallengerUseCase getChallengerUseCase;
    private final GetCommentListUseCase getCommentListUseCase;


    /**
     * 이 Evaluator가 처리할 수 있는 ResourceType
     */
    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.COMMUNITY_COMMENT;
    }

    @Override
    public boolean evaluate(SubjectAttributes subjectAttributes, ResourcePermission resourcePermission) {
        Long commentId = resourcePermission.getResourceIdAsLong();

        CommentInfo commentInfo = getCommentListUseCase.getComment(commentId);
        Long authorChallengerId = commentInfo.challengerId();
        ChallengerInfo authorChallengerInfo =
            getChallengerUseCase.getById(authorChallengerId);

        Long authorMemberId = authorChallengerInfo.memberId();

        boolean isAuthor = subjectAttributes.memberId().equals(authorChallengerInfo.memberId());

        switch (resourcePermission.permission()) {
            case READ -> {
                // READ 권한은 모든 챌린저에게 허용
                return true;
            }
            case WRITE -> {
                // 게시글 작성은 챌린저라면 누구나 가능
                return !getChallengerUseCase
                    .getAllByMemberId(authorMemberId).isEmpty();
            }
            case EDIT -> {
                // 수정은 게시글 작성자만 가능
                return isAuthor;
            }
            case DELETE -> {
                // 삭제는 게시글 작성자나 총괄단이 가능
                return subjectAttributes.toAuthoritySnapshot().isCentralCoreInAnyGisu()
                    || isAuthor;
            }
            default -> {
                log.warn("CommunityPostPE에서 지원하지 않는 PermissionType: {}", resourcePermission.permission());
                return false;
            }
        }
    }
}
