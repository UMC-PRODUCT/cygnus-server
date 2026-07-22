package com.umc.product.notice.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.notice.domain.NoticeImage;
import com.umc.product.notice.domain.NoticeLink;
import com.umc.product.notice.domain.NoticeVote;

@DisplayName("NoticeContentPersistenceAdapter 단위 테스트")
class NoticeContentPersistenceAdapterUnitTest {

    @Test
    @DisplayName("이미지·링크·투표의 모든 조회·저장·삭제 port 계약을 위임한다")
    void delegates_all_content_contracts() {
        NoticeVoteJpaRepository voteRepository = mock(NoticeVoteJpaRepository.class);
        NoticeLinkJpaRepository linkRepository = mock(NoticeLinkJpaRepository.class);
        NoticeImageJpaRepository imageRepository = mock(NoticeImageJpaRepository.class);
        NoticeContentsQueryRepository queryRepository = mock(NoticeContentsQueryRepository.class);
        NoticeContentPersistenceAdapter sut = new NoticeContentPersistenceAdapter(
            voteRepository, linkRepository, imageRepository, queryRepository
        );
        NoticeImage image = mock(NoticeImage.class);
        NoticeLink link = mock(NoticeLink.class);
        NoticeVote vote = mock(NoticeVote.class);
        given(imageRepository.findById(1L)).willReturn(Optional.of(image));
        given(imageRepository.findByNoticeId(10L)).willReturn(List.of(image));
        given(imageRepository.existsByNoticeId(10L)).willReturn(true);
        given(queryRepository.findNextImageDisplayOrder(10L)).willReturn(2);
        given(imageRepository.countByNotice_Id(10L)).willReturn(1);
        given(linkRepository.findById(2L)).willReturn(Optional.of(link));
        given(linkRepository.findByNoticeId(10L)).willReturn(List.of(link));
        given(linkRepository.existsByNoticeId(10L)).willReturn(true);
        given(queryRepository.findNextLinkDisplayOrder(10L)).willReturn(3);
        given(linkRepository.countByNotice_Id(10L)).willReturn(1);
        given(voteRepository.findById(3L)).willReturn(Optional.of(vote));
        given(voteRepository.findByNoticeId(10L)).willReturn(Optional.of(vote));
        given(voteRepository.existsByNoticeId(10L)).willReturn(true);
        given(imageRepository.save(image)).willReturn(image);
        given(imageRepository.saveAll(List.of(image))).willReturn(List.of(image));
        given(linkRepository.save(link)).willReturn(link);
        given(linkRepository.saveAll(List.of(link))).willReturn(List.of(link));
        given(voteRepository.save(vote)).willReturn(vote);

        assertThat(sut.findImageById(1L)).contains(image);
        assertThat(sut.findImagesByNoticeId(10L)).containsExactly(image);
        assertThat(sut.existsImageByNoticeId(10L)).isTrue();
        assertThat(sut.findNextImageDisplayOrder(10L)).isEqualTo(2);
        assertThat(sut.countImageByNoticeId(10L)).isOne();
        assertThat(sut.findLinkById(2L)).contains(link);
        assertThat(sut.findLinksByNoticeId(10L)).containsExactly(link);
        assertThat(sut.existsLinkByNoticeId(10L)).isTrue();
        assertThat(sut.findNextLinkDisplayOrder(10L)).isEqualTo(3);
        assertThat(sut.countLinkByNoticeId(10L)).isOne();
        assertThat(sut.findVoteById(3L)).contains(vote);
        assertThat(sut.findVoteByNoticeId(10L)).contains(vote);
        assertThat(sut.existsVoteByNoticeId(10L)).isTrue();
        assertThat(sut.saveImage(image)).isSameAs(image);
        assertThat(sut.saveAllImages(List.of(image))).containsExactly(image);
        sut.deleteImage(image);
        sut.deleteAllImagesByNoticeId(10L);
        assertThat(sut.saveLink(link)).isSameAs(link);
        assertThat(sut.saveAllLinks(List.of(link))).containsExactly(link);
        sut.deleteLink(link);
        sut.deleteAllLinksByNoticeId(10L);
        assertThat(sut.saveVote(vote)).isSameAs(vote);
        sut.deleteVote(vote);
        sut.deleteAllVotesByNoticeId(10L);

        verify(imageRepository).delete(image);
        verify(imageRepository).deleteAllByNoticeId(10L);
        verify(linkRepository).delete(link);
        verify(linkRepository).deleteAllByNoticeId(10L);
        verify(voteRepository).delete(vote);
        verify(voteRepository).deleteAllByNoticeId(10L);
    }
}
