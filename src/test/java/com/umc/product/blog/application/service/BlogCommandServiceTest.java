package com.umc.product.blog.application.service;

import static com.umc.product.support.fixture.BlogUnitFixture.content;
import static com.umc.product.support.fixture.BlogUnitFixture.series;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.blog.application.port.in.command.dto.CreateBlogContentCommand;
import com.umc.product.blog.application.port.in.command.dto.CreateBlogSeriesCommand;
import com.umc.product.blog.application.port.in.command.dto.DeleteBlogContentCommand;
import com.umc.product.blog.application.port.in.command.dto.DeleteBlogSeriesCommand;
import com.umc.product.blog.application.port.in.command.dto.ReplaceBlogSeriesContentsCommand;
import com.umc.product.blog.application.port.in.command.dto.UpdateBlogContentCommand;
import com.umc.product.blog.application.port.in.command.dto.UpdateBlogSeriesCommand;
import com.umc.product.blog.application.port.in.query.dto.BlogContentInfo;
import com.umc.product.blog.application.port.in.query.dto.BlogSeriesInfo;
import com.umc.product.blog.application.port.out.LoadBlogContentPort;
import com.umc.product.blog.application.port.out.LoadBlogHashtagPort;
import com.umc.product.blog.application.port.out.LoadBlogSeriesPort;
import com.umc.product.blog.application.port.out.SaveBlogContentPort;
import com.umc.product.blog.application.port.out.SaveBlogHashtagPort;
import com.umc.product.blog.application.port.out.SaveBlogSeriesContentPort;
import com.umc.product.blog.application.port.out.SaveBlogSeriesPort;
import com.umc.product.blog.domain.BlogContent;
import com.umc.product.blog.domain.BlogContentHashtag;
import com.umc.product.blog.domain.BlogContentStatus;
import com.umc.product.blog.domain.BlogDomainException;
import com.umc.product.blog.domain.BlogHashtag;
import com.umc.product.blog.domain.BlogSeries;
import com.umc.product.blog.domain.BlogSeriesContent;

@ExtendWith(MockitoExtension.class)
@DisplayName("Blog command service 테스트")
class BlogCommandServiceTest {

    @Nested
    @DisplayName("콘텐츠 command")
    class ContentCommand {

        @Mock
        LoadBlogContentPort loadBlogContentPort;

        @Mock
        SaveBlogContentPort saveBlogContentPort;

        @Mock
        LoadBlogHashtagPort loadBlogHashtagPort;

        @Mock
        SaveBlogHashtagPort saveBlogHashtagPort;

        @Mock
        BlogContentInfoAssembler contentInfoAssembler;

        BlogContentCommandService service;

        @BeforeEach
        void setUp() {
            service = new BlogContentCommandService(
                loadBlogContentPort,
                saveBlogContentPort,
                loadBlogHashtagPort,
                saveBlogHashtagPort,
                contentInfoAssembler
            );
        }

        @Test
        @DisplayName("생성은 기본 DRAFT를 적용하고 중복 hashtag를 입력 순서대로 한 번만 저장한다")
        void 생성은_hashtag를_중복_제거해_순서대로_저장한다() {
            CreateBlogContentCommand command = createContentCommand(null, List.of("Spring", "spring", "Java"));
            BlogContentInfo expected = org.mockito.Mockito.mock(BlogContentInfo.class);
            BlogHashtag existing = BlogHashtag.create("Spring");
            ReflectionTestUtils.setField(existing, "id", 11L);
            given(loadBlogContentPort.existsContentByTypeAndSlug(any(), any(), any())).willReturn(false);
            given(saveBlogContentPort.save(any())).willAnswer(invocation -> {
                BlogContent saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", 10L);
                return saved;
            });
            given(loadBlogHashtagPort.findByNormalizedName("spring")).willReturn(Optional.of(existing));
            given(loadBlogHashtagPort.findByNormalizedName("java")).willReturn(Optional.empty());
            given(saveBlogHashtagPort.save(any())).willAnswer(invocation -> {
                BlogHashtag saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", 12L);
                return saved;
            });
            given(contentInfoAssembler.assemble(any(BlogContent.class), any(), any(Boolean.class)))
                .willReturn(expected);

            BlogContentInfo actual = service.create(command);

            assertThat(actual).isSameAs(expected);
            ArgumentCaptor<BlogContent> contentCaptor = ArgumentCaptor.forClass(BlogContent.class);
            verify(saveBlogContentPort).save(contentCaptor.capture());
            assertThat(contentCaptor.getValue().getStatus()).isEqualTo(BlogContentStatus.DRAFT);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<BlogContentHashtag>> relationCaptor = ArgumentCaptor.forClass(List.class);
            verify(saveBlogHashtagPort).saveContentHashtags(relationCaptor.capture());
            assertThat(relationCaptor.getValue()).extracting(BlogContentHashtag::getHashtagId).containsExactly(11L, 12L);
            assertThat(relationCaptor.getValue()).extracting(BlogContentHashtag::getDisplayOrder).containsExactly(0, 1);
        }

        @Test
        @DisplayName("생성은 동일 type과 slug가 있으면 저장하지 않는다")
        void 생성은_중복_slug를_거부한다() {
            CreateBlogContentCommand command = createContentCommand(BlogContentStatus.DRAFT, List.of());
            given(loadBlogContentPort.existsContentByTypeAndSlug(any(), any(), any())).willReturn(true);

            assertThatThrownBy(() -> service.create(command)).isInstanceOf(BlogDomainException.class);
            verify(saveBlogContentPort, never()).save(any());
        }

        @Test
        @DisplayName("생성과 수정은 빈 hashtag 입력에서 관계를 비우고 조회를 생략한다")
        void 빈_hashtag는_관계만_비운다() {
            BlogContent target = content();
            ReflectionTestUtils.setField(target, "id", 10L);
            given(loadBlogContentPort.existsContentByTypeAndSlug(any(), any(), any())).willReturn(false);
            given(saveBlogContentPort.save(any())).willReturn(target);
            given(loadBlogContentPort.findContentById(10L)).willReturn(Optional.of(target));
            given(contentInfoAssembler.assemble(any(BlogContent.class), any(), any(Boolean.class)))
                .willReturn(org.mockito.Mockito.mock(BlogContentInfo.class));

            service.create(createContentCommand(BlogContentStatus.DRAFT, null));
            service.update(updateContentCommand(List.of()));

            verify(saveBlogHashtagPort, org.mockito.Mockito.times(2)).deleteContentHashtags(10L);
            verify(loadBlogHashtagPort, never()).findByNormalizedName(any());
        }

        @Test
        @DisplayName("수정은 존재 여부와 slug 중복을 검사한 뒤 변경값을 저장한다")
        void 수정은_존재와_slug_중복을_검증한다() {
            given(loadBlogContentPort.findContentById(10L)).willReturn(Optional.empty());
            assertThatThrownBy(() -> service.update(updateContentCommand(List.of())))
                .isInstanceOf(BlogDomainException.class);

            BlogContent target = content();
            ReflectionTestUtils.setField(target, "id", 10L);
            given(loadBlogContentPort.findContentById(10L)).willReturn(Optional.of(target));
            given(loadBlogContentPort.existsContentByTypeAndSlug(any(), any(), any())).willReturn(true);
            assertThatThrownBy(() -> service.update(updateContentCommand(List.of())))
                .isInstanceOf(BlogDomainException.class);
        }

        @Test
        @DisplayName("hashtag는 최대 10개를 초과하면 기존 관계를 삭제하기 전에 거부한다")
        void hashtag는_최대_10개까지_허용한다() {
            List<String> hashtags = java.util.stream.IntStream.rangeClosed(1, 11).mapToObj(i -> "tag" + i).toList();
            given(loadBlogContentPort.existsContentByTypeAndSlug(any(), any(), any())).willReturn(false);
            given(saveBlogContentPort.save(any())).willAnswer(invocation -> {
                BlogContent saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", 10L);
                return saved;
            });

            assertThatThrownBy(() -> service.create(createContentCommand(BlogContentStatus.DRAFT, hashtags)))
                .isInstanceOf(BlogDomainException.class);
            verify(saveBlogHashtagPort, never()).deleteContentHashtags(any());
        }

        @Test
        @DisplayName("삭제는 존재하는 콘텐츠를 soft delete하고 저장한다")
        void 삭제는_soft_delete한다() {
            BlogContent target = content();
            given(loadBlogContentPort.findContentById(10L)).willReturn(Optional.of(target));

            service.delete(DeleteBlogContentCommand.of(10L, 1L));

            assertThat(target.isDeleted()).isTrue();
            verify(saveBlogContentPort).save(target);
        }

        @Test
        @DisplayName("삭제는 콘텐츠가 없으면 not-found를 반환한다")
        void 삭제는_없는_콘텐츠를_거부한다() {
            given(loadBlogContentPort.findContentById(10L)).willReturn(Optional.empty());
            assertThatThrownBy(() -> service.delete(DeleteBlogContentCommand.of(10L, 1L)))
                .isInstanceOf(BlogDomainException.class);
        }
    }

    @Nested
    @DisplayName("시리즈 command")
    class SeriesCommand {

        @Mock
        LoadBlogSeriesPort loadBlogSeriesPort;

        @Mock
        SaveBlogSeriesPort saveBlogSeriesPort;

        @Mock
        LoadBlogContentPort loadBlogContentPort;

        @Mock
        SaveBlogSeriesContentPort saveBlogSeriesContentPort;

        @Mock
        BlogSeriesInfoAssembler seriesInfoAssembler;

        BlogSeriesCommandService service;

        @BeforeEach
        void setUp() {
            service = new BlogSeriesCommandService(
                loadBlogSeriesPort,
                saveBlogSeriesPort,
                loadBlogContentPort,
                saveBlogSeriesContentPort,
                seriesInfoAssembler
            );
        }

        @Test
        @DisplayName("생성은 중복 slug를 거부하고 정상 입력은 저장한다")
        void 생성은_slug_중복을_검증한다() {
            CreateBlogSeriesCommand command = createSeriesCommand();
            given(loadBlogSeriesPort.existsSeriesByTypeAndSlug(any(), any(), any())).willReturn(true);
            assertThatThrownBy(() -> service.create(command)).isInstanceOf(BlogDomainException.class);

            given(loadBlogSeriesPort.existsSeriesByTypeAndSlug(any(), any(), any())).willReturn(false);
            BlogSeries saved = series();
            ReflectionTestUtils.setField(saved, "id", 10L);
            BlogSeriesInfo expected = org.mockito.Mockito.mock(BlogSeriesInfo.class);
            given(saveBlogSeriesPort.save(any())).willReturn(saved);
            given(seriesInfoAssembler.assemble(saved, 1L, true)).willReturn(expected);
            assertThat(service.create(command)).isSameAs(expected);
        }

        @Test
        @DisplayName("수정과 삭제는 존재하고 삭제되지 않은 시리즈만 처리한다")
        void 수정과_삭제는_존재하는_시리즈만_처리한다() {
            given(loadBlogSeriesPort.findSeriesById(10L)).willReturn(Optional.empty());
            assertThatThrownBy(() -> service.update(updateSeriesCommand())).isInstanceOf(BlogDomainException.class);

            BlogSeries deleted = series();
            deleted.softDelete(1L);
            given(loadBlogSeriesPort.findSeriesById(10L)).willReturn(Optional.of(deleted));
            assertThatThrownBy(() -> service.delete(DeleteBlogSeriesCommand.of(10L, 1L)))
                .isInstanceOf(BlogDomainException.class);
        }

        @Test
        @DisplayName("수정은 다른 시리즈의 slug와 중복되면 저장하지 않는다")
        void 수정은_slug_중복을_거부한다() {
            BlogSeries target = series();
            ReflectionTestUtils.setField(target, "id", 10L);
            given(loadBlogSeriesPort.findSeriesById(10L)).willReturn(Optional.of(target));
            given(loadBlogSeriesPort.existsSeriesByTypeAndSlug(any(), any(), any())).willReturn(true);

            assertThatThrownBy(() -> service.update(updateSeriesCommand())).isInstanceOf(BlogDomainException.class);
            verify(saveBlogSeriesPort, never()).save(any());
        }

        @Test
        @DisplayName("수정과 삭제는 도메인 변경을 저장하고 assembler 결과를 반환한다")
        void 수정과_삭제는_변경을_저장한다() {
            BlogSeries target = series();
            ReflectionTestUtils.setField(target, "id", 10L);
            BlogSeriesInfo expected = org.mockito.Mockito.mock(BlogSeriesInfo.class);
            given(loadBlogSeriesPort.findSeriesById(10L)).willReturn(Optional.of(target));
            given(loadBlogSeriesPort.existsSeriesByTypeAndSlug(any(), any(), any())).willReturn(false);
            given(saveBlogSeriesPort.save(target)).willReturn(target);
            given(seriesInfoAssembler.assemble(target, 1L, false)).willReturn(expected);

            assertThat(service.update(updateSeriesCommand())).isSameAs(expected);
            assertThat(target.getSlug()).isEqualTo("updated");
            service.delete(DeleteBlogSeriesCommand.of(10L, 1L));
            assertThat(target.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("콘텐츠 교체는 중복 ID를 제거하고 입력 순서대로 관계를 저장한다")
        void 콘텐츠_교체는_중복을_제거하고_입력_순서를_보존한다() {
            BlogSeries target = series();
            ReflectionTestUtils.setField(target, "id", 10L);
            BlogContent first = content("first", BlogContentStatus.PUBLISHED, 1L);
            BlogContent second = content("second", BlogContentStatus.DRAFT, 1L);
            ReflectionTestUtils.setField(first, "id", 1L);
            ReflectionTestUtils.setField(second, "id", 2L);
            given(loadBlogSeriesPort.findSeriesById(10L)).willReturn(Optional.of(target));
            given(loadBlogContentPort.listByIds(List.of(2L, 1L))).willReturn(List.of(second, first));
            BlogSeriesInfo expected = org.mockito.Mockito.mock(BlogSeriesInfo.class);
            given(seriesInfoAssembler.assemble(target, 1L, false)).willReturn(expected);

            assertThat(service.replaceContents(ReplaceBlogSeriesContentsCommand.of(10L, List.of(2L, 1L, 2L))))
                .isSameAs(expected);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<BlogSeriesContent>> captor = ArgumentCaptor.forClass(List.class);
            verify(saveBlogSeriesContentPort).saveAll(captor.capture());
            assertThat(captor.getValue()).extracting(BlogSeriesContent::getContentId).containsExactly(2L, 1L);
            assertThat(captor.getValue()).extracting(BlogSeriesContent::getDisplayOrder).containsExactly(0, 1);
        }

        @Test
        @DisplayName("콘텐츠 교체는 누락, 삭제, 다른 type 콘텐츠를 거부한다")
        void 콘텐츠_교체는_잘못된_콘텐츠를_거부한다() {
            BlogSeries target = series();
            ReflectionTestUtils.setField(target, "id", 10L);
            given(loadBlogSeriesPort.findSeriesById(10L)).willReturn(Optional.of(target));

            given(loadBlogContentPort.listByIds(List.of(1L))).willReturn(List.of());
            assertThatThrownBy(() -> service.replaceContents(ReplaceBlogSeriesContentsCommand.of(10L, List.of(1L))))
                .isInstanceOf(BlogDomainException.class);

            BlogContent deleted = content();
            deleted.softDelete(1L);
            given(loadBlogContentPort.listByIds(List.of(1L))).willReturn(List.of(deleted));
            assertThatThrownBy(() -> service.replaceContents(ReplaceBlogSeriesContentsCommand.of(10L, List.of(1L))))
                .isInstanceOf(BlogDomainException.class);

            BlogContent mismatched = BlogContent.create(
                com.umc.product.blog.domain.BlogContentType.DESIGN,
                "design",
                "제목",
                null,
                null,
                "본문",
                BlogContentStatus.DRAFT,
                1L,
                null,
                null,
                null
            );
            given(loadBlogContentPort.listByIds(List.of(1L))).willReturn(List.of(mismatched));
            assertThatThrownBy(() -> service.replaceContents(ReplaceBlogSeriesContentsCommand.of(10L, List.of(1L))))
                .isInstanceOf(BlogDomainException.class);
        }
    }

    private static CreateBlogContentCommand createContentCommand(BlogContentStatus status, List<String> hashtags) {
        return CreateBlogContentCommand.of(
            "engineering", "content", "제목", "요약", null, "본문", status, 1L, null, null, null, hashtags
        );
    }

    private static UpdateBlogContentCommand updateContentCommand(List<String> hashtags) {
        return UpdateBlogContentCommand.of(
            10L,
            "updated",
            "수정 제목",
            "수정 요약",
            null,
            "수정 본문",
            BlogContentStatus.PUBLISHED,
            null,
            null,
            null,
            hashtags
        );
    }

    private static CreateBlogSeriesCommand createSeriesCommand() {
        return CreateBlogSeriesCommand.of(
            "engineering", "series", "시리즈", "설명", null, 1L, null, null, null
        );
    }

    private static UpdateBlogSeriesCommand updateSeriesCommand() {
        return UpdateBlogSeriesCommand.of(10L, "updated", "수정", "설명", null, null, null, null);
    }
}
