package com.umc.product.community.adapter.out.persistence;

import static com.umc.product.community.domain.QComment.comment;
import static com.umc.product.community.domain.QPost.post;
import static com.umc.product.community.domain.QScrap.scrap;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.product.community.application.port.in.query.dto.PostSearchQuery;
import com.umc.product.community.application.port.in.query.dto.PostSearchResult.MatchType;
import com.umc.product.community.application.port.out.dto.PostSearchData;
import com.umc.product.community.domain.Post;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PostQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<Post> findAllByQuery(PostSearchQuery query, Pageable pageable) {
        BooleanExpression condition = buildCondition(query);

        List<Post> results = queryFactory
            .selectFrom(post)
            .where(condition)
            .orderBy(post.createdAt.desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        Long totalCount = queryFactory
            .select(post.count())
            .from(post)
            .where(condition)
            .fetchOne();

        return new PageImpl<>(results, pageable, totalCount != null ? totalCount : 0);
    }

    private BooleanExpression buildCondition(PostSearchQuery query) {
        if (query.category() != null) {
            return post.category.eq(query.category());
        }
        return null;
    }

    public Page<Post> searchByKeyword(String keyword, Pageable pageable) {
        if (!StringUtils.hasText(keyword)) {
            return Page.empty(pageable);
        }

        String searchKeyword = keyword.trim().toLowerCase();

        BooleanExpression searchCondition = titleContains(searchKeyword)
            .or(contentContains(searchKeyword));

        NumberExpression<Integer> relevanceScore = createRelevanceScore(searchKeyword);

        List<Post> results = queryFactory
            .selectFrom(post)
            .where(searchCondition)
            .orderBy(
                relevanceScore.desc(),
                post.createdAt.desc()
            )
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        Long totalCount = queryFactory
            .select(post.count())
            .from(post)
            .where(searchCondition)
            .fetchOne();

        return new PageImpl<>(results, pageable, totalCount != null ? totalCount : 0);

        // TODO: 여기서 PostSearchData 반환하는 중
//        List<PostSearchData> searchDataList = results.stream()
//            .map(entity -> toSearchData(entity, searchKeyword))
//            .toList();
//
//        return new PageImpl<>(searchDataList, pageable, totalCount != null ? totalCount : 0);
    }

    private BooleanExpression titleContains(String keyword) {
        return post.title.lower().contains(keyword);
    }

    private BooleanExpression contentContains(String keyword) {
        return post.content.lower().contains(keyword);
    }

    private NumberExpression<Integer> createRelevanceScore(String keyword) {
        return new CaseBuilder()
            .when(post.title.lower().startsWith(keyword))
            .then(100)
            .when(post.title.lower().contains(keyword))
            .then(50)
            .when(post.content.lower().contains(keyword))
            .then(10)
            .otherwise(0);
    }

    private PostSearchData toSearchData(Post entity, String keyword) {
        MatchType matchType = determineMatchType(entity, keyword);
        int score = calculateScore(entity, keyword);

        return PostSearchData.from(entity, matchType, score);
    }

    private MatchType determineMatchType(Post entity, String keyword) {
        String titleLower = entity.getTitle().toLowerCase();

        if (titleLower.startsWith(keyword)) {
            return MatchType.TITLE_START;
        }
        if (titleLower.contains(keyword)) {
            return MatchType.TITLE_CONTAIN;
        }
        return MatchType.CONTENT;
    }

    private int calculateScore(Post entity, String keyword) {
        String titleLower = entity.getTitle().toLowerCase();

        if (titleLower.startsWith(keyword)) {
            return 100;
        }
        if (titleLower.contains(keyword)) {
            return 50;
        }
        return 10;
    }

    /**
     * 챌린저가 작성한 게시글 목록 조회
     */
    public Page<Post> findByAuthorChallengerId(Long challengerId, Pageable pageable) {
        List<Post> results = queryFactory
            .selectFrom(post)
            .where(post.authorChallengerId.eq(challengerId))
            .orderBy(post.createdAt.desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        Long totalCount = queryFactory
            .select(post.count())
            .from(post)
            .where(post.authorChallengerId.eq(challengerId))
            .fetchOne();

        return new PageImpl<>(results, pageable, totalCount != null ? totalCount : 0);
    }

    /**
     * 챌린저가 댓글을 단 게시글 목록 조회 (중복 제거, 최신 댓글 순)
     */
    public Page<Post> findCommentedPostsByChallengerId(Long challengerId, Pageable pageable) {
        List<Post> results = queryFactory
            .selectFrom(post)
            .innerJoin(comment)
            .on(post.id.eq(comment.post.id))
            .where(comment.challengerId.eq(challengerId))
            .groupBy(post.id)
            .orderBy(comment.createdAt.max().desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        if (results.isEmpty()) {
            return Page.empty(pageable);
        }

        Long totalCount = queryFactory
            .select(comment.post.id.countDistinct())
            .from(comment)
            .where(comment.challengerId.eq(challengerId))
            .fetchOne();

        return new PageImpl<>(results, pageable, totalCount != null ? totalCount : 0);
    }

    /**
     * 챌린저가 스크랩한 게시글 목록 조회 (최신 스크랩 순)
     */
    public Page<Post> findScrappedPostsByChallengerId(Long challengerId, Pageable pageable) {
        List<Post> results = queryFactory
            .selectFrom(post)
            .innerJoin(scrap)
            .on(post.id.eq(scrap.post.id))
            .where(scrap.challengerId.eq(challengerId))
            .orderBy(scrap.createdAt.desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        if (results.isEmpty()) {
            return Page.empty(pageable);
        }

        Long totalCount = queryFactory
            .select(scrap.count())
            .from(scrap)
            .where(scrap.challengerId.eq(challengerId))
            .fetchOne();

        return new PageImpl<>(results, pageable, totalCount != null ? totalCount : 0);
    }
}
