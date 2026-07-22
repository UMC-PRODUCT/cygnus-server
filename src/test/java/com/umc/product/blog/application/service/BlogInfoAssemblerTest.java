package com.umc.product.blog.application.service;

import static com.umc.product.support.fixture.BlogUnitFixture.comment;
import static com.umc.product.support.fixture.BlogUnitFixture.content;
import static com.umc.product.support.fixture.BlogUnitFixture.series;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.blog.application.port.in.query.dto.BlogAuthorInfo;
import com.umc.product.blog.application.port.out.LoadBlogHashtagPort;
import com.umc.product.blog.application.port.out.LoadBlogSeriesPort;
import com.umc.product.blog.domain.BlogComment;
import com.umc.product.blog.domain.BlogContent;
import com.umc.product.blog.domain.BlogHashtag;
import com.umc.product.blog.domain.BlogSeries;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;

@ExtendWith(MockitoExtension.class)
@DisplayName("Blog info assembler 테스트")
class BlogInfoAssemblerTest {

    @Mock
    GetMemberUseCase getMemberUseCase;

    @Mock
    LoadBlogSeriesPort loadBlogSeriesPort;

    @Mock
    LoadBlogHashtagPort loadBlogHashtagPort;

    @Test
    @DisplayName("작성자 assembler는 빈 batch를 단축하고 조회된 회원만 공개 정보로 변환한다")
    void 작성자_assembler는_빈_batch와_부분_조회에_대응한다() {
        BlogAuthorAssembler assembler = new BlogAuthorAssembler(getMemberUseCase);
        assertThat(assembler.assemble(null)).isEmpty();
        assertThat(assembler.assemble(Set.of())).isEmpty();
        verify(getMemberUseCase, never()).findAllByIds(Set.of());

        MemberInfo member = member(1L);
        given(getMemberUseCase.findAllByIds(Set.of(1L, 2L))).willReturn(Map.of(1L, member));
        Map<Long, BlogAuthorInfo> result = assembler.assemble(Set.of(1L, 2L));

        assertThat(result).containsOnlyKeys(1L);
        assertThat(result.get(1L).name()).isEqualTo("회원1");
        assertThat(result.get(1L).profileImageUrl()).isEqualTo("profile-1");
    }

    @Test
    @DisplayName("콘텐츠 assembler는 빈 목록에서 외부 조회 없이 빈 결과를 반환한다")
    void 콘텐츠_assembler는_빈_목록을_단축한다() {
        BlogContentInfoAssembler assembler = new BlogContentInfoAssembler(
            new BlogAuthorAssembler(getMemberUseCase), loadBlogSeriesPort, loadBlogHashtagPort
        );

        assertThat(assembler.assemble(List.of(), null, false)).isEmpty();
        assertThat(assembler.assembleSummaries(List.of(), null, false)).isEmpty();
        verify(loadBlogSeriesPort, never()).listSeriesByContentIdsGrouped(List.of());
    }

    @Test
    @DisplayName("콘텐츠 assembler는 시리즈·hashtag·작성자 관계와 권한을 한 번에 조립한다")
    void 콘텐츠_assembler는_연관_정보와_권한을_조립한다() {
        BlogContent target = identified(content(), 10L);
        BlogSeries relatedSeries = identified(series("related-series", 2L), 20L);
        BlogHashtag hashtag = BlogHashtag.create("Spring");
        ReflectionTestUtils.setField(hashtag, "id", 30L);
        given(loadBlogSeriesPort.listSeriesByContentIdsGrouped(List.of(10L)))
            .willReturn(Map.of(10L, List.of(relatedSeries)));
        given(loadBlogHashtagPort.listHashtagsByContentIdsGrouped(List.of(10L)))
            .willReturn(Map.of(10L, List.of(hashtag)));
        given(loadBlogSeriesPort.countPublishedContentsBySeriesIds(List.of(20L))).willReturn(Map.of(20L, 7));
        given(loadBlogHashtagPort.countPublishedContentsByHashtagIds(List.of(30L))).willReturn(Map.of(30L, 8));
        given(getMemberUseCase.findAllByIds(Set.of(1L, 2L)))
            .willReturn(Map.of(1L, member(1L), 2L, member(2L)));
        BlogContentInfoAssembler assembler = new BlogContentInfoAssembler(
            new BlogAuthorAssembler(getMemberUseCase), loadBlogSeriesPort, loadBlogHashtagPort
        );

        var info = assembler.assemble(target, 1L, false);
        var summary = assembler.assembleSummaries(List.of(target), 3L, true).getFirst();

        assertThat(info.author().id()).isEqualTo(1L);
        assertThat(info.series()).singleElement().satisfies(seriesInfo -> {
            assertThat(seriesInfo.id()).isEqualTo(20L);
            assertThat(seriesInfo.contentCount()).isEqualTo(7);
            assertThat(seriesInfo.author().id()).isEqualTo(2L);
            assertThat(seriesInfo.canEdit()).isFalse();
            assertThat(seriesInfo.canDelete()).isFalse();
        });
        assertThat(info.hashtags()).singleElement().satisfies(tag -> assertThat(tag.contentCount()).isEqualTo(8));
        assertThat(info.canEdit()).isTrue();
        assertThat(info.canDelete()).isTrue();
        assertThat(summary.canEdit()).isFalse();
        assertThat(summary.canDelete()).isTrue();
    }

    @Test
    @DisplayName("시리즈 assembler는 단건과 목록에서 count 기본값과 관리자 삭제 권한을 적용한다")
    void 시리즈_assembler는_count와_권한을_조립한다() {
        BlogSeries target = identified(series(), 10L);
        BlogAuthorAssembler authorAssembler = new BlogAuthorAssembler(getMemberUseCase);
        BlogSeriesInfoAssembler assembler = new BlogSeriesInfoAssembler(authorAssembler, loadBlogSeriesPort);
        given(getMemberUseCase.findAllByIds(Set.of(1L))).willReturn(Map.of(1L, member(1L)));
        given(loadBlogSeriesPort.countPublishedContentsBySeriesIds(List.of(10L))).willReturn(Map.of());

        var detail = assembler.assemble(target, 1L, false);
        var summary = assembler.assembleSummaries(List.of(target), 2L, true).getFirst();

        assertThat(detail.contentCount()).isZero();
        assertThat(detail.canEdit()).isTrue();
        assertThat(detail.canDelete()).isTrue();
        assertThat(summary.author().id()).isEqualTo(1L);
        assertThat(summary.canEdit()).isFalse();
        assertThat(summary.canDelete()).isTrue();
    }

    @Test
    @DisplayName("댓글 assembler는 guest fallback과 누락 회원, 삭제 placeholder를 안전하게 처리한다")
    void 댓글_assembler는_작성자_누락과_삭제를_처리한다() {
        BlogCommentInfoAssembler assembler = new BlogCommentInfoAssembler(getMemberUseCase);
        BlogComment guest = identified(BlogComment.create(1L, null, null, true, "게스트", "댓글"), 10L);
        ReflectionTestUtils.setField(guest, "nickname", null);
        var guestInfo = assembler.assemble(guest, true, 3, List.of(), null, false);
        assertThat(guestInfo.author().nickname()).isEqualTo("익명");

        BlogComment memberComment = identified(comment(), 11L);
        given(getMemberUseCase.findAllByIds(Set.of(1L))).willReturn(Map.of());
        var missingAuthor = assembler.assemble(memberComment, false, 0, List.of(), 1L, false);
        assertThat(missingAuthor.author()).isNull();
        assertThat(missingAuthor.canEdit()).isTrue();

        memberComment.deleteByAdmin(2L);
        var deleted = assembler.assemble(memberComment, true, 99, List.of(), 1L, true);
        assertThat(deleted.author()).isNull();
        assertThat(deleted.likedByMe()).isFalse();
        assertThat(deleted.likeCount()).isZero();
        assertThat(deleted.canEdit()).isFalse();
        assertThat(deleted.canDelete()).isFalse();
    }

    private static MemberInfo member(Long id) {
        return MemberInfo.builder()
            .id(id)
            .name("회원" + id)
            .nickname("nickname-" + id)
            .profileImageLink("profile-" + id)
            .build();
    }

    private static BlogContent identified(BlogContent target, Long id) {
        ReflectionTestUtils.setField(target, "id", id);
        return target;
    }

    private static BlogSeries identified(BlogSeries target, Long id) {
        ReflectionTestUtils.setField(target, "id", id);
        return target;
    }

    private static BlogComment identified(BlogComment target, Long id) {
        ReflectionTestUtils.setField(target, "id", id);
        return target;
    }
}
