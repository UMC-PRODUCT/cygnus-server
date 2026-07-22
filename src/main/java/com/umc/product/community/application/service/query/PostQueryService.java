package com.umc.product.community.application.service.query;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.community.adapter.in.web.dto.response.PostResponse;
import com.umc.product.community.application.port.in.query.GetCommentedPostsUseCase;
import com.umc.product.community.application.port.in.query.GetMyPostsUseCase;
import com.umc.product.community.application.port.in.query.GetPostDetailUseCase;
import com.umc.product.community.application.port.in.query.GetPostListUseCase;
import com.umc.product.community.application.port.in.query.GetScrappedPostsUseCase;
import com.umc.product.community.application.port.in.query.SearchPostUseCase;
import com.umc.product.community.application.port.in.query.dto.PostDetailInfo;
import com.umc.product.community.application.port.in.query.dto.PostInfo;
import com.umc.product.community.application.port.in.query.dto.PostSearchQuery;
import com.umc.product.community.application.port.out.comment.LoadCommentPort;
import com.umc.product.community.application.port.out.dto.PostWithAuthor;
import com.umc.product.community.application.port.out.post.LoadPostPort;
import com.umc.product.community.application.port.out.scrap.LoadScrapPort;
import com.umc.product.community.domain.Post;
import com.umc.product.community.domain.exception.CommunityDomainException;
import com.umc.product.community.domain.exception.CommunityErrorCode;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostQueryService implements GetPostDetailUseCase, GetPostListUseCase, SearchPostUseCase,
    GetMyPostsUseCase, GetCommentedPostsUseCase, GetScrappedPostsUseCase {

    private final LoadPostPort loadPostPort;
    private final LoadCommentPort loadCommentPort;
    private final LoadScrapPort loadScrapPort;
    private final GetChallengerUseCase getChallengerUseCase;
    private final GetMemberUseCase getMemberUseCase;

    @Override
    public PostInfo getPostDetail(Long postId) {
        PostWithAuthor postWithAuthor = loadPostPort.findByIdWithAuthor(postId)
            .orElseThrow(() -> new CommunityDomainException(CommunityErrorCode.POST_NOT_FOUND));

        Long authorChallengerId = postWithAuthor.authorChallengerId();

        // 챌린저 정보 조회
        ChallengerInfo challengerInfo = getChallengerUseCase.getById(authorChallengerId);

        // 멤버 프로필 조회 (이름과 프로필 이미지)
        MemberInfo memberProfile = getMemberUseCase.getById(challengerInfo.memberId());
        String authorName = memberProfile.name();
        String authorProfileImage = memberProfile.profileImageLink();

        return PostInfo.from(postWithAuthor.post(), authorChallengerId, authorName, authorProfileImage,
            challengerInfo.part(), 0);
    }

    @Override
    public PostDetailInfo getPostDetail(Long postId, Long challengerId) {
        PostWithAuthor postWithAuthor = loadPostPort.findByIdWithAuthor(postId, challengerId)
            .orElseThrow(() -> new CommunityDomainException(CommunityErrorCode.POST_NOT_FOUND));

        Long authorChallengerId = postWithAuthor.authorChallengerId();

        // 작성자 챌린저 정보 조회
        ChallengerInfo authorChallengerInfo = getChallengerUseCase.getById(authorChallengerId);

        // 작성자 멤버 프로필 조회 (이름과 프로필 이미지)
        MemberInfo memberProfile = getMemberUseCase.getById(authorChallengerInfo.memberId());

        int commentCount = loadCommentPort.countByPostId(postId);

        PostInfo postInfo = PostInfo.from(
            postWithAuthor.post(),
            memberProfile,
            authorChallengerInfo,
            postWithAuthor.liked()
        );

        // 스크랩 정보 조회
        boolean isScrapped = loadScrapPort.existsByPostIdAndChallengerId(postId, challengerId);
        int scrapCount = loadScrapPort.countByPostId(postId);

        return PostDetailInfo.of(postInfo, commentCount, authorChallengerInfo.part(), isScrapped, scrapCount);
    }

    @Override
    public Page<PostInfo> getPostList(PostSearchQuery query, Long memberId, Pageable pageable) {
        Page<Post> posts = loadPostPort.findAllByQuery(query, pageable);
        return convertToPostInfoPage(posts, pageable);
    }

    @Override
    public Page<PostResponse> search(String keyword, Pageable pageable) {
        Page<Post> searchDataPage = loadPostPort.searchByKeyword(keyword, pageable);

        return searchDataPage.map(
            data -> {
                ChallengerInfo challengerInfo = getChallengerUseCase.getById(
                    data.getAuthorChallengerId());
                MemberInfo memberInfo = getMemberUseCase.getById(challengerInfo.memberId());

                return PostResponse.from(
                    PostInfo.from(data, memberInfo, challengerInfo), memberInfo, challengerInfo
                );
            }
        );
    }

    @Override
    public Page<PostInfo> getMyPosts(Long memberId, Pageable pageable) {
        Long challengerId = getChallengerUseCase.getLatestActiveChallengerByMemberId(memberId).challengerId();
        Page<Post> posts = loadPostPort.findByAuthorChallengerId(challengerId, pageable);
        return convertToPostInfoPage(posts, pageable);
    }

    @Override
    public Page<PostInfo> getCommentedPosts(Long memberId, Pageable pageable) {
        Long challengerId = getChallengerUseCase.getLatestActiveChallengerByMemberId(memberId).challengerId();
        Page<Post> posts = loadPostPort.findCommentedPostsByChallengerId(challengerId, pageable);
        return convertToPostInfoPage(posts, pageable);
    }

    @Override
    public Page<PostInfo> getScrappedPosts(Long memberId, Pageable pageable) {
        Long challengerId = getChallengerUseCase.getLatestActiveChallengerByMemberId(memberId).challengerId();
        Page<Post> posts = loadPostPort.findScrappedPostsByChallengerId(challengerId, pageable);
        return convertToPostInfoPage(posts, pageable);
    }

    /**
     * Post 페이지를 PostInfo 페이지로 변환 (작성자 정보 포함)
     */
    private Page<PostInfo> convertToPostInfoPage(Page<Post> posts, Pageable pageable) {
        // 게시글이 없으면 빈 페이지 반환
        if (posts.isEmpty()) {
            return Page.empty(pageable);
        }

        // 1. 게시글 ID 목록 추출
        List<Long> postIds = posts.stream()
            .map(Post::getId)
            .toList();

        // 2. 게시글 ID -> 작성자 챌린저 ID 매핑 (1 query)
        Map<Long, Long> postIdToAuthorId = loadPostPort.findAuthorIdsByPostIds(postIds);

        // 3. 고유한 챌린저 ID 목록 추출
        Set<Long> challengerIds = Set.copyOf(postIdToAuthorId.values());

        // 4. 챌린저 ID -> 챌린저 정보 매핑 (1 query)
        Map<Long, ChallengerInfo> challengerInfoMap = getChallengerUseCase.getAllByIdsAsMap(challengerIds);

        // 5. 멤버 ID 목록 추출
        Set<Long> memberIds = challengerInfoMap.values().stream()
            .map(ChallengerInfo::memberId)
            .collect(Collectors.toSet());

        // 6. 멤버 ID -> 멤버 프로필 매핑 (1 query, 일괄 조회로 N+1 해결)
        Map<Long, MemberInfo> memberProfileMap = getMemberUseCase.findAllByIds(memberIds);

        // 7. 챌린저 ID -> 작성자 정보 매핑 (이름 + 프로필 이미지 + 파트)
        Map<Long, AuthorDetails> authorDetailsMap = challengerInfoMap.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    Long memberId = entry.getValue().memberId();
                    MemberInfo memberProfile = memberProfileMap.get(memberId);
                    String name = memberProfile != null ? memberProfile.name() : "알 수 없음";
                    String profileImage = memberProfile != null ? memberProfile.profileImageLink() : null;
                    return new AuthorDetails(name, profileImage, entry.getValue().part());
                }
            ));

        // 8. 댓글 수 일괄 조회 (1 query)
        Map<Long, Integer> commentCountMap = loadCommentPort.countByPostIds(postIds);

        // 9. PostInfo로 변환
        return posts.map(post -> {
            Long postId = post.getId();
            Long authorChallengerId = postIdToAuthorId.get(postId);
            AuthorDetails authorDetails = authorChallengerId != null ? authorDetailsMap.get(authorChallengerId) : null;
            String authorName = authorDetails != null ? authorDetails.name() : "알 수 없음";
            String authorProfileImage = authorDetails != null ? authorDetails.profileImage() : null;
            ChallengerPart authorPart = authorDetails != null ? authorDetails.part() : null;
            int commentCount = commentCountMap.getOrDefault(postId, 0);
            return PostInfo.from(post, authorChallengerId, authorName, authorProfileImage, authorPart, commentCount);
        });
    }

    private record AuthorDetails(
        String name,
        String profileImage,
        ChallengerPart part
    ) {
    }
}
