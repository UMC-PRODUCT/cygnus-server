package com.umc.product.community.application.service.evaluator;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.community.application.port.in.query.GetPostDetailUseCase;
import com.umc.product.community.application.port.in.query.dto.PostInfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class CommunityPostPermissionEvaluator implements ResourcePermissionEvaluator {

    private final GetPostDetailUseCase getPostDetailUseCase;
    private final GetChallengerUseCase getChallengerUseCase;


    /**
     * 이 Evaluator가 처리할 수 있는 ResourceType
     */
    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.COMMUNITY_POST;
    }

    @Override
    public boolean evaluate(SubjectAttributes subjectAttributes, ResourcePermission resourcePermission) {
        Long postId = resourcePermission.getResourceIdAsLong();

        PostInfo postInfo = getPostDetailUseCase.getPostDetail(postId);
        Long authorChallengerId = postInfo.authorChallengerId();
        Long authorMemberId = getChallengerUseCase.getById(authorChallengerId).memberId();

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
                return authorMemberId.equals(subjectAttributes.memberId());
            }
            case DELETE -> {
                // 삭제는 게시글 작성자나 총괄단이 가능
                return subjectAttributes.toAuthoritySnapshot().isCentralCoreInAnyGisu()
                    || authorMemberId.equals(subjectAttributes.memberId());
            }
            default -> {
                log.warn("CommunityPermissionEvaluator에서 지원하지 않는 PermissionType: {}", resourcePermission.permission());
                return false;
            }
        }
    }
}
