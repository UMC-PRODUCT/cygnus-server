package com.umc.product.notice.application.service.query;

import static com.umc.product.support.fixture.NoticeUnitFixture.notice;
import static com.umc.product.support.fixture.NoticeUnitFixture.target;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.form.application.port.in.query.GetVoteUseCase;
import com.umc.product.form.application.port.in.query.dto.VoteInfo;
import com.umc.product.form.application.port.in.query.dto.VoteInfo.VoteOptionInfo;
import com.umc.product.notice.application.port.out.LoadNoticeImagePort;
import com.umc.product.notice.application.port.out.LoadNoticeLinkPort;
import com.umc.product.notice.application.port.out.LoadNoticeTargetPort;
import com.umc.product.notice.application.port.out.LoadNoticeVotePort;
import com.umc.product.notice.domain.NoticeImage;
import com.umc.product.notice.domain.NoticeLink;
import com.umc.product.notice.domain.NoticeVote;
import com.umc.product.notice.domain.exception.NoticeDomainException;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;

@ExtendWith(MockitoExtension.class)
@DisplayName("Notice content 및 target query service 테스트")
class NoticeContentAndTargetQueryServiceTest {

    @Nested
    @DisplayName("콘텐츠 조회")
    class ContentQuery {

        @Mock
        LoadNoticeVotePort loadNoticeVotePort;

        @Mock
        LoadNoticeImagePort loadNoticeImagePort;

        @Mock
        LoadNoticeLinkPort loadNoticeLinkPort;

        @Mock
        GetFileUseCase getFileUseCase;

        @Mock
        GetVoteUseCase getVoteUseCase;

        @InjectMocks
        NoticeContentQueryService service;

        @Test
        @DisplayName("링크는 displayOrder 오름차순으로 info를 반환한다")
        void 링크를_표시_순서대로_반환한다() {
            NoticeLink second = link(2L, "second", 2);
            NoticeLink first = link(1L, "first", 1);
            given(loadNoticeLinkPort.findLinksByNoticeId(1L)).willReturn(List.of(second, first));

            assertThat(service.findLinkByNoticeId(1L))
                .extracting(info -> info.id(), info -> info.url(), info -> info.displayOrder())
                .containsExactly(
                    org.assertj.core.groups.Tuple.tuple(1L, "first", 1),
                    org.assertj.core.groups.Tuple.tuple(2L, "second", 2)
                );
        }

        @Test
        @DisplayName("투표가 없거나 form 정보가 누락되면 null을 반환한다")
        void 투표_부분_데이터_누락을_처리한다() {
            given(loadNoticeVotePort.findVoteByNoticeId(1L)).willReturn(Optional.empty());
            assertThat(service.findVoteByNoticeId(1L, 10L)).isNull();

            NoticeVote vote = openVote();
            given(loadNoticeVotePort.findVoteByNoticeId(1L)).willReturn(Optional.of(vote));
            given(getVoteUseCase.getVoteInfo(20L, 10L)).willReturn(null);
            assertThat(service.findVoteByNoticeId(1L, 10L)).isNull();
        }

        @Test
        @DisplayName("투표는 form 선택지와 참여 정보 및 notice 기간 상태를 조립한다")
        void 투표_정보를_조립한다() {
            NoticeVote vote = openVote();
            VoteOptionInfo option = new VoteOptionInfo(30L, "A", 2L, BigDecimal.TEN, List.of(10L));
            VoteInfo formInfo = new VoteInfo(20L, "투표", true, true, 2L, List.of(30L), List.of(option));
            given(loadNoticeVotePort.findVoteByNoticeId(1L)).willReturn(Optional.of(vote));
            given(getVoteUseCase.getVoteInfo(20L, 10L)).willReturn(formInfo);

            var result = service.findVoteByNoticeId(1L, 10L);

            assertThat(result.voteId()).isEqualTo(20L);
            assertThat(result.title()).isEqualTo("투표");
            assertThat(result.isAnonymous()).isTrue();
            assertThat(result.allowMultipleChoice()).isTrue();
            assertThat(result.totalParticipants()).isEqualTo(2L);
            assertThat(result.mySelectedOptionIds()).containsExactly(30L);
            assertThat(result.options()).singleElement().satisfies(mapped -> {
                assertThat(mapped.optionId()).isEqualTo(30L);
                assertThat(mapped.selectedMemberIds()).containsExactly(10L);
            });
        }

        @Test
        @DisplayName("이미지는 file link 누락을 허용하며 displayOrder 오름차순으로 반환한다")
        void 이미지는_file_link를_batch로_조립한다() {
            NoticeImage second = image(2L, "second", 2);
            NoticeImage first = image(1L, "first", 1);
            given(loadNoticeImagePort.findImagesByNoticeId(1L)).willReturn(List.of(second, first));
            given(getFileUseCase.getFileLinks(List.of("second", "first"))).willReturn(Map.of("first", "url"));

            assertThat(service.findImageByNoticeId(1L))
                .extracting(info -> info.id(), info -> info.url(), info -> info.displayOrder())
                .containsExactly(
                    org.assertj.core.groups.Tuple.tuple(1L, "url", 1),
                    org.assertj.core.groups.Tuple.tuple(2L, null, 2)
                );
        }

        private NoticeVote openVote() {
            return NoticeVote.create(
                20L, notice(), Instant.now().minusSeconds(60), Instant.now().plusSeconds(60)
            );
        }

        private NoticeImage image(Long id, String imageId, int order) {
            NoticeImage image = NoticeImage.create(imageId, notice(), order);
            ReflectionTestUtils.setField(image, "id", id);
            return image;
        }

        private NoticeLink link(Long id, String url, int order) {
            NoticeLink link = NoticeLink.create(url, notice(), order);
            ReflectionTestUtils.setField(link, "id", id);
            return link;
        }
    }

    @Nested
    @DisplayName("대상 조회")
    class TargetQuery {

        @Mock
        LoadNoticeTargetPort loadNoticeTargetPort;

        @InjectMocks
        NoticeTargetQueryService service;

        @Test
        @DisplayName("대상을 info로 변환하고 미존재 대상은 구체적인 예외를 반환한다")
        void 대상을_조회하고_미존재를_거부한다() {
            given(loadNoticeTargetPort.findByNoticeId(1L)).willReturn(Optional.of(target()));
            assertThat(service.findByNoticeId(1L).targetGisuId()).isEqualTo(1L);

            given(loadNoticeTargetPort.findByNoticeId(2L)).willReturn(Optional.empty());
            assertThatThrownBy(() -> service.findByNoticeId(2L)).isInstanceOf(NoticeDomainException.class);
        }
    }
}
