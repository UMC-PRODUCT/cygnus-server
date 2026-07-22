package com.umc.product.community.application.service.query;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.community.application.port.in.query.GetCommentListUseCase;
import com.umc.product.community.application.port.in.query.dto.CommentInfo;
import com.umc.product.community.application.port.out.comment.LoadCommentPort;
import com.umc.product.community.application.port.out.post.LoadPostPort;
import com.umc.product.community.domain.Comment;
import com.umc.product.community.domain.exception.CommunityDomainException;
import com.umc.product.community.domain.exception.CommunityErrorCode;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentQueryService implements GetCommentListUseCase {

    private final LoadPostPort loadPostPort;
    private final LoadCommentPort loadCommentPort;

    private final GetChallengerUseCase getChallengerUseCase;
    private final GetMemberUseCase getMemberUseCase;

    @Override
    public List<CommentInfo> getComments(Long postId) {
        return getComments(postId, null);
    }

    @Override
    public List<CommentInfo> getComments(Long postId, Long currentChallengerId) {
        loadPostPort.findById(postId)
            .orElseThrow(() -> new CommunityDomainException(CommunityErrorCode.POST_NOT_FOUND));

        Page<Comment> comments = loadCommentPort.findByPostId(postId, Pageable.unpaged());

        // Comment 객체에서 CommentInfo를 만들어내야함

        if (comments.isEmpty()) {
            return List.of();
        }

        // TODO: 성능 개선 나중에 합시다
        return comments.stream()
            .map(comment -> {
                ChallengerInfo challengerInfo = getChallengerUseCase.findByIdOrNull(comment.getChallengerId());
                MemberInfo memberInfo = challengerInfo != null
                    ? getMemberUseCase.findByIdOrNull(challengerInfo.memberId())
                    : null;

                boolean isAuthor = comment.getChallengerId().equals(currentChallengerId);
                return CommentInfo.of(comment, memberInfo, challengerInfo, isAuthor);
            }).toList();

//        // 1. 챌린저 ID 목록 추출
//        Set<Long> challengerIds = comments.stream()
//            .map(Comment::getChallengerId)
//            .collect(Collectors.toSet());
//
//        // 2. 챌린저 ID -> 챌린저 정보 매핑
//        Map<Long, ChallengerInfo> challengerInfoMap = getChallengerUseCase.getChallengerPublicInfoByIds(challengerIds);
//        Map<Long, ChallengerInfo> newChallengerInfoMap = challengerIds.stream()
//            .map(getChallengerUseCase::findByIdOrNull)
//            .filter(Objects::nonNull)
//            .collect(Collectors.toMap(ChallengerInfo::challengerId, challengerInfo -> challengerInfo));
//
//        // 3. 멤버 ID 목록 추출
//        Set<Long> memberIds = newChallengerInfoMap.values().stream()
//            .map(ChallengerInfo::memberId)
//            .collect(Collectors.toSet());
//
//        // 4. 멤버 ID -> 멤버 프로필 매핑 (1 query, 일괄 조회로 N+1 해결)
//        var memberProfileMap = getMemberUseCase.getProfiles(memberIds);
//
//        // 5. 챌린저 ID -> 작성자 정보 매핑 (이름 + 프로필 이미지 + 파트)
//        Map<Long, AuthorDetails> authorDetailsMap = challengerInfoMap.entrySet().stream()
//            .collect(Collectors.toMap(
//                Map.Entry::getKey,
//                entry -> {
//                    Long memberId = entry.getValue().memberId();
//                    var memberProfile = memberProfileMap.get(memberId);
//                    String name = memberProfile != null ? memberProfile.name() : "알 수 없음";
//                    String profileImage = memberProfile != null ? memberProfile.profileImageLink() : null;
//                    return new AuthorDetails(name, profileImage, entry.getValue().part());
//                }
//            ));
//
//        return comments.stream()
//            .map(comment -> {
//                AuthorDetails authorDetails = authorDetailsMap.get(comment.getChallengerId());
//                String authorName = authorDetails != null ? authorDetails.name() : "알 수 없음";
//                String authorProfileImage = authorDetails != null ? authorDetails.profileImage() : null;
//                var authorPart = authorDetails != null ? authorDetails.part() : null;
//                // 본인 작성 댓글 여부 확인
//                boolean isAuthor = comment.getChallengerId().equals(currentChallengerId);
//                return CommentInfo.from(
//                    comment,
//                    authorName,
//                    authorProfileImage,
//                    authorPart,
//                    isAuthor
//                );
//            })
//            .toList();
    }

    @Override
    public CommentInfo getComment(Long commentId) {
        // TODO: 이거 CommentInfo에 isAuthor 필드가 있으면 안 될 것 같다는 의견이긴 합니다. 예은이는 어떻게 생각하시나요? by 경운.

        return loadCommentPort.findById(commentId)
            .map(comment -> {
                ChallengerInfo challengerInfo = getChallengerUseCase.getById(comment.getChallengerId());
                MemberInfo memberInfo = getMemberUseCase.getById(challengerInfo.memberId());

                String authorName = memberInfo != null ? memberInfo.name() : "알 수 없음";
                String authorProfileImage = memberInfo != null ? memberInfo.profileImageLink() : null;

                return CommentInfo.from(
                    comment,
                    authorName,
                    authorProfileImage,
                    challengerInfo.part(),
                    false // 단일 댓글 조회에서는 작성자 여부 판단이 어려움 (currentChallengerId가 없기 때문)
                );
            })
            .orElseThrow(() -> new CommunityDomainException(CommunityErrorCode.COMMENT_NOT_FOUND));
    }
}
