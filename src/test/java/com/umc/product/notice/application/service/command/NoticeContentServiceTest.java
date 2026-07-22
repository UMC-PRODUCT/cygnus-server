package com.umc.product.notice.application.service.command;

import static com.umc.product.support.fixture.NoticeUnitFixture.notice;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.form.application.port.in.command.ManageVoteUseCase;
import com.umc.product.notice.application.port.in.command.dto.AddNoticeImagesCommand;
import com.umc.product.notice.application.port.in.command.dto.AddNoticeLinksCommand;
import com.umc.product.notice.application.port.in.command.dto.AddNoticeVoteCommand;
import com.umc.product.notice.application.port.in.command.dto.ReplaceNoticeImagesCommand;
import com.umc.product.notice.application.port.in.command.dto.ReplaceNoticeLinksCommand;
import com.umc.product.notice.application.port.out.LoadNoticeImagePort;
import com.umc.product.notice.application.port.out.LoadNoticeLinkPort;
import com.umc.product.notice.application.port.out.LoadNoticePort;
import com.umc.product.notice.application.port.out.LoadNoticeVotePort;
import com.umc.product.notice.application.port.out.SaveNoticeImagePort;
import com.umc.product.notice.application.port.out.SaveNoticeLinkPort;
import com.umc.product.notice.application.port.out.SaveNoticeVotePort;
import com.umc.product.notice.domain.Notice;
import com.umc.product.notice.domain.NoticeImage;
import com.umc.product.notice.domain.NoticeLink;
import com.umc.product.notice.domain.NoticeVote;
import com.umc.product.notice.domain.exception.NoticeDomainException;

@ExtendWith(MockitoExtension.class)
@DisplayName("Notice 콘텐츠 command service 테스트")
class NoticeContentServiceTest {

    @Mock
    LoadNoticeVotePort loadNoticeVotePort;

    @Mock
    LoadNoticeLinkPort loadNoticeLinkPort;

    @Mock
    LoadNoticeImagePort loadNoticeImagePort;

    @Mock
    SaveNoticeVotePort saveNoticeVotePort;

    @Mock
    SaveNoticeImagePort saveNoticeImagePort;

    @Mock
    SaveNoticeLinkPort saveNoticeLinkPort;

    @Mock
    LoadNoticePort loadNoticePort;

    @Mock
    ManageVoteUseCase manageVoteUseCase;

    NoticeContentService service;

    @BeforeEach
    void setUp() {
        service = new NoticeContentService(
            loadNoticeVotePort,
            loadNoticeLinkPort,
            loadNoticeImagePort,
            saveNoticeVotePort,
            saveNoticeImagePort,
            saveNoticeLinkPort,
            loadNoticePort,
            manageVoteUseCase
        );
    }

    @Test
    @DisplayName("투표 추가는 작성자와 중복을 검증하고 form vote와 notice vote ID를 반환한다")
    void 투표_추가는_작성자와_중복을_검증한다() {
        Notice target = notice();
        given(loadNoticePort.findNoticeById(1L)).willReturn(Optional.of(target));
        AddNoticeVoteCommand command = voteCommand(10L);

        given(loadNoticeVotePort.existsVoteByNoticeId(1L)).willReturn(true);
        assertThatThrownBy(() -> service.addVote(command, 1L)).isInstanceOf(NoticeDomainException.class);

        given(loadNoticeVotePort.existsVoteByNoticeId(1L)).willReturn(false);
        given(manageVoteUseCase.createVote(any())).willReturn(20L);
        given(saveNoticeVotePort.saveVote(any())).willAnswer(invocation -> {
            NoticeVote saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 30L);
            return saved;
        });

        var result = service.addVote(command, 1L);
        assertThat(result.noticeVoteId()).isEqualTo(30L);
        assertThat(result.voteId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("투표 추가는 미존재 공지와 다른 작성자를 거부한다")
    void 투표_추가는_공지와_작성자를_검증한다() {
        given(loadNoticePort.findNoticeById(1L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.addVote(voteCommand(10L), 1L)).isInstanceOf(NoticeDomainException.class);

        given(loadNoticePort.findNoticeById(1L)).willReturn(Optional.of(notice()));
        assertThatThrownBy(() -> service.addVote(voteCommand(11L), 1L)).isInstanceOf(NoticeDomainException.class);
    }

    @Test
    @DisplayName("이미지 추가는 빈 입력과 누적 10장 초과를 거부한다")
    void 이미지_추가는_입력과_상한을_검증한다() {
        given(loadNoticePort.findNoticeById(1L)).willReturn(Optional.of(notice()));
        assertThatThrownBy(() -> service.addImages(new AddNoticeImagesCommand(List.of()), 1L, 10L))
            .isInstanceOf(NoticeDomainException.class);

        given(loadNoticeImagePort.countImageByNoticeId(1L)).willReturn(9);
        assertThatThrownBy(() -> service.addImages(
            new AddNoticeImagesCommand(List.of("a", "b")), 1L, 10L
        )).isInstanceOf(NoticeDomainException.class);
    }

    @Test
    @DisplayName("이미지 추가는 다음 표시 순서부터 저장하고 생성 ID를 반환한다")
    void 이미지_추가는_순서대로_저장한다() {
        given(loadNoticePort.findNoticeById(1L)).willReturn(Optional.of(notice()));
        given(loadNoticeImagePort.countImageByNoticeId(1L)).willReturn(1);
        given(loadNoticeImagePort.findNextImageDisplayOrder(1L)).willReturn(3);
        given(saveNoticeImagePort.saveAllImages(any())).willAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<NoticeImage> images = invocation.getArgument(0);
            ReflectionTestUtils.setField(images.get(0), "id", 100L);
            ReflectionTestUtils.setField(images.get(1), "id", 101L);
            return images;
        });

        assertThat(service.addImages(new AddNoticeImagesCommand(List.of("a", "b")), 1L, 10L))
            .containsExactly(100L, 101L);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<NoticeImage>> captor = ArgumentCaptor.forClass(List.class);
        verify(saveNoticeImagePort).saveAllImages(captor.capture());
        assertThat(captor.getValue()).extracting(NoticeImage::getDisplayOrder).containsExactly(3, 4);
    }

    @Test
    @DisplayName("링크 추가는 빈 입력을 거부하고 다음 표시 순서부터 저장한다")
    void 링크_추가는_입력과_순서를_검증한다() {
        given(loadNoticePort.findNoticeById(1L)).willReturn(Optional.of(notice()));
        assertThatThrownBy(() -> service.addLinks(new AddNoticeLinksCommand(List.of()), 1L, 10L))
            .isInstanceOf(NoticeDomainException.class);

        given(loadNoticeLinkPort.findNextLinkDisplayOrder(1L)).willReturn(2);
        given(saveNoticeLinkPort.saveAllLinks(any())).willAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<NoticeLink> links = invocation.getArgument(0);
            ReflectionTestUtils.setField(links.get(0), "id", 100L);
            return links;
        });
        assertThat(service.addLinks(new AddNoticeLinksCommand(List.of("link")), 1L, 10L))
            .containsExactly(100L);
    }

    @Test
    @DisplayName("투표 삭제는 notice vote와 form vote를 함께 삭제하고 미존재 투표를 거부한다")
    void 투표_삭제는_두_저장소를_동기화한다() {
        Notice target = notice();
        given(loadNoticePort.findNoticeById(1L)).willReturn(Optional.of(target));
        given(loadNoticeVotePort.findVoteByNoticeId(1L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.deleteVote(1L, 10L)).isInstanceOf(NoticeDomainException.class);

        NoticeVote vote = NoticeVote.create(
            20L, target, Instant.now().minusSeconds(1), Instant.now().plusSeconds(60)
        );
        given(loadNoticeVotePort.findVoteByNoticeId(1L)).willReturn(Optional.of(vote));
        service.deleteVote(1L, 10L);
        verify(saveNoticeVotePort).deleteVote(vote);
        verify(manageVoteUseCase).deleteVote(20L);
    }

    @Test
    @DisplayName("전체 콘텐츠 삭제는 이미지·링크를 지우고 투표가 있을 때만 form까지 삭제한다")
    void 전체_콘텐츠_삭제는_투표_존재를_분기한다() {
        NoticeVote vote = NoticeVote.create(
            20L, notice(), Instant.now().minusSeconds(1), Instant.now().plusSeconds(60)
        );
        given(loadNoticeVotePort.findVoteByNoticeId(1L)).willReturn(Optional.empty(), Optional.of(vote));

        service.removeContentsByNoticeId(1L, 10L);
        service.removeContentsByNoticeId(1L, 10L);

        verify(saveNoticeImagePort, org.mockito.Mockito.times(2)).deleteAllImagesByNoticeId(1L);
        verify(saveNoticeLinkPort, org.mockito.Mockito.times(2)).deleteAllLinksByNoticeId(1L);
        verify(saveNoticeVotePort).deleteAllVotesByNoticeId(1L);
        verify(manageVoteUseCase).deleteVote(20L);
    }

    @Test
    @DisplayName("이미지 교체는 null no-op, 10장 상한, 빈 목록 삭제, 재정렬을 처리한다")
    void 이미지_교체는_모든_입력_경계를_처리한다() {
        service.replaceImages(new ReplaceNoticeImagesCommand(null), 1L, 10L);
        verify(loadNoticePort, never()).findNoticeById(1L);

        assertThatThrownBy(() -> service.replaceImages(
            new ReplaceNoticeImagesCommand(java.util.stream.IntStream.range(0, 11).mapToObj(String::valueOf).toList()),
            1L,
            10L
        )).isInstanceOf(NoticeDomainException.class);

        given(loadNoticePort.findNoticeById(1L)).willReturn(Optional.of(notice()));
        service.replaceImages(new ReplaceNoticeImagesCommand(List.of()), 1L, 10L);
        service.replaceImages(new ReplaceNoticeImagesCommand(List.of("a", "b")), 1L, 10L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<NoticeImage>> captor = ArgumentCaptor.forClass(List.class);
        verify(saveNoticeImagePort).saveAllImages(captor.capture());
        assertThat(captor.getValue()).extracting(NoticeImage::getDisplayOrder).containsExactly(0, 1);
    }

    @Test
    @DisplayName("링크 교체는 null no-op, 빈 목록 삭제, 비어 있지 않으면 재정렬한다")
    void 링크_교체는_모든_입력_경계를_처리한다() {
        service.replaceLinks(new ReplaceNoticeLinksCommand(null), 1L, 10L);
        verify(loadNoticePort, never()).findNoticeById(1L);

        given(loadNoticePort.findNoticeById(1L)).willReturn(Optional.of(notice()));
        service.replaceLinks(new ReplaceNoticeLinksCommand(List.of()), 1L, 10L);
        service.replaceLinks(new ReplaceNoticeLinksCommand(List.of("a", "b")), 1L, 10L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<NoticeLink>> captor = ArgumentCaptor.forClass(List.class);
        verify(saveNoticeLinkPort).saveAllLinks(captor.capture());
        assertThat(captor.getValue()).extracting(NoticeLink::getDisplayOrder).containsExactly(0, 1);
    }

    private static AddNoticeVoteCommand voteCommand(Long memberId) {
        return AddNoticeVoteCommand.builder()
            .createdMemberId(memberId)
            .title("투표")
            .startsAt(Instant.now().minusSeconds(1))
            .endsAtExclusive(Instant.now().plusSeconds(60))
            .options(List.of("A", "B"))
            .build();
    }
}
