package com.umc.product.community.adapter.in.graphql;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import com.umc.product.challenger.adapter.in.graphql.dto.ChallengerGraphQlResponse;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.community.adapter.in.graphql.dto.CommunityPostGraphQlResponse;
import com.umc.product.community.adapter.in.graphql.dto.CommunityPostPageGraphQlResponse;
import com.umc.product.community.application.port.in.query.GetCommentListUseCase;
import com.umc.product.community.application.port.in.query.GetCommentedPostsUseCase;
import com.umc.product.community.application.port.in.query.GetMyPostsUseCase;
import com.umc.product.community.application.port.in.query.GetPostDetailUseCase;
import com.umc.product.community.application.port.in.query.GetPostListUseCase;
import com.umc.product.community.application.port.in.query.GetScrappedPostsUseCase;
import com.umc.product.community.application.port.in.query.dto.CommentInfo;
import com.umc.product.community.application.port.in.query.dto.PostInfo;
import com.umc.product.community.application.port.in.query.dto.PostSearchQuery;
import com.umc.product.community.domain.enums.Category;
import com.umc.product.global.graphql.dto.PageGraphQlRequest;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberPublicInfo;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class CommunityGraphQlController {

    private final GetPostDetailUseCase getPostDetailUseCase;
    private final GetPostListUseCase getPostListUseCase;
    private final GetMyPostsUseCase getMyPostsUseCase;
    private final GetCommentedPostsUseCase getCommentedPostsUseCase;
    private final GetScrappedPostsUseCase getScrappedPostsUseCase;
    private final GetCommentListUseCase getCommentListUseCase;
    private final GetChallengerUseCase getChallengerUseCase;
    private final GetMemberUseCase getMemberUseCase;

    @QueryMapping
    public CommunityPostGraphQlResponse communityPost(
        @CurrentMember MemberPrincipal principal,
        @Argument Long id
    ) {
        return CommunityPostGraphQlResponse.from(
            getPostDetailUseCase.getPostDetail(id, currentChallengerId(principal))
        );
    }

    @QueryMapping
    public CommunityPostPageGraphQlResponse communityPosts(
        @CurrentMember MemberPrincipal principal,
        @Argument Category category,
        @Argument PageGraphQlRequest page
    ) {
        return CommunityPostPageGraphQlResponse.from(
            getPostListUseCase.getPostList(
                new PostSearchQuery(category),
                principal.getMemberId(),
                PageGraphQlRequest.defaultIfNull(page).toPageableWithMaxOffset(10_000L)
            )
        );
    }

    @QueryMapping
    public CommunityPostPageGraphQlResponse myCommunityPosts(
        @CurrentMember MemberPrincipal principal,
        @Argument MyPostKind kind,
        @Argument PageGraphQlRequest page
    ) {
        Page<PostInfo> posts = switch (kind) {
            case AUTHORED -> getMyPostsUseCase.getMyPosts(principal.getMemberId(), pageable(page));
            case COMMENTED -> getCommentedPostsUseCase.getCommentedPosts(principal.getMemberId(), pageable(page));
            case SCRAPPED -> getScrappedPostsUseCase.getScrappedPosts(principal.getMemberId(), pageable(page));
        };
        return CommunityPostPageGraphQlResponse.from(posts);
    }

    @SchemaMapping(typeName = "CommunityPost", field = "comments")
    public List<CommentInfo> comments(
        CommunityPostGraphQlResponse post,
        @CurrentMember MemberPrincipal principal
    ) {
        return getCommentListUseCase.getComments(post.postId(), currentChallengerId(principal));
    }

    @BatchMapping(typeName = "CommunityPost", field = "author")
    public Map<CommunityPostGraphQlResponse, MemberPublicInfo> authors(
        List<CommunityPostGraphQlResponse> posts
    ) {
        Set<Long> memberIds = posts.stream()
            .map(CommunityPostGraphQlResponse::authorMemberId)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, MemberPublicInfo> membersById = getMemberUseCase.findAllByIds(memberIds).entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> MemberPublicInfo.from(entry.getValue())
            ));
        Map<CommunityPostGraphQlResponse, MemberPublicInfo> result = new LinkedHashMap<>();
        for (CommunityPostGraphQlResponse post : posts) {
            result.put(post, membersById.get(post.authorMemberId()));
        }
        return result;
    }

    @BatchMapping(typeName = "CommunityPost", field = "authorChallenger")
    public Map<CommunityPostGraphQlResponse, ChallengerGraphQlResponse> authorChallengers(
        List<CommunityPostGraphQlResponse> posts
    ) {
        return challengerMap(posts, CommunityPostGraphQlResponse::authorChallengerId);
    }

    @BatchMapping(typeName = "CommunityComment", field = "challenger")
    public Map<CommentInfo, ChallengerGraphQlResponse> commentChallengers(List<CommentInfo> comments) {
        return challengerMap(comments, CommentInfo::challengerId);
    }

    private <T> Map<T, ChallengerGraphQlResponse> challengerMap(
        List<T> sources,
        Function<T, Long> challengerIdExtractor
    ) {
        Set<Long> challengerIds = sources.stream()
            .map(challengerIdExtractor)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, ChallengerInfo> challengersById = getChallengerUseCase.getAllByIds(challengerIds).stream()
            .collect(Collectors.toMap(ChallengerInfo::challengerId, Function.identity()));
        Map<T, ChallengerGraphQlResponse> result = new LinkedHashMap<>();
        for (T source : sources) {
            ChallengerInfo challenger = challengersById.get(challengerIdExtractor.apply(source));
            result.put(source, challenger == null ? null : ChallengerGraphQlResponse.from(challenger));
        }
        return result;
    }

    private Long currentChallengerId(MemberPrincipal principal) {
        return getChallengerUseCase.getLatestActiveChallengerByMemberId(principal.getMemberId()).challengerId();
    }

    private org.springframework.data.domain.Pageable pageable(PageGraphQlRequest page) {
        return PageGraphQlRequest.defaultIfNull(page).toPageableWithMaxOffset(10_000L);
    }

    public enum MyPostKind {
        AUTHORED,
        COMMENTED,
        SCRAPPED
    }
}
