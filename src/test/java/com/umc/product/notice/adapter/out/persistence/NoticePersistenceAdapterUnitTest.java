package com.umc.product.notice.adapter.out.persistence;

import static com.umc.product.support.fixture.NoticeUnitFixture.notice;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.umc.product.notice.application.port.in.query.dto.NoticeViewerInfo;
import com.umc.product.notice.domain.Notice;
import com.umc.product.notice.domain.NoticeClassification;
import com.umc.product.notice.domain.NoticeRead;
import com.umc.product.notice.domain.enums.NoticeTab;

@DisplayName("NoticePersistenceAdapter 단위 테스트")
class NoticePersistenceAdapterUnitTest {

    @Test
    @DisplayName("공지·읽음의 모든 persistence port 계약을 repository에 그대로 위임한다")
    void delegates_notice_and_read_contracts() {
        NoticeJpaRepository noticeRepository = mock(NoticeJpaRepository.class);
        NoticeReadJpaRepository readRepository = mock(NoticeReadJpaRepository.class);
        NoticeQueryRepository queryRepository = mock(NoticeQueryRepository.class);
        NoticePersistenceAdapter sut = new NoticePersistenceAdapter(noticeRepository, readRepository, queryRepository);
        Notice target = notice();
        NoticeRead read = NoticeRead.builder().notice(target).challengerId(2L).build();
        NoticeClassification classification = new NoticeClassification(1L, null, null, null, NoticeTab.CHALLENGER);
        NoticeViewerInfo viewer = new NoticeViewerInfo(null, null, null, null);
        PageRequest pageable = PageRequest.of(0, 10);
        PageImpl<Notice> page = new PageImpl<>(List.of(target), pageable, 1);

        given(noticeRepository.findById(1L)).willReturn(Optional.of(target));
        given(queryRepository.findByClassification(classification, viewer, pageable)).willReturn(page);
        given(queryRepository.findByKeyword("검색", classification, viewer, pageable)).willReturn(page);
        given(noticeRepository.findAll(pageable)).willReturn(page);
        given(noticeRepository.save(target)).willReturn(target);
        given(readRepository.findAllByNoticeId(1L)).willReturn(List.of(read));
        given(queryRepository.findUnreadChallengerIdByNoticeId(1L)).willReturn(List.of(2L));
        given(readRepository.existsByNoticeIdAndChallengerId(1L, 2L)).willReturn(true);
        given(readRepository.countByNoticeId(1L)).willReturn(3);
        given(queryRepository.countReadsByNoticeIds(List.of(1L))).willReturn(Map.of(1L, 3L));
        given(readRepository.countByNoticeIdAndChallengerIdIn(1L, List.of(2L))).willReturn(1L);
        given(readRepository.save(read)).willReturn(read);

        assertThat(sut.findNoticeById(1L)).contains(target);
        assertThat(sut.findNoticesByClassification(classification, viewer, pageable)).isSameAs(page);
        assertThat(sut.findNoticesByKeyword("검색", classification, viewer, pageable)).isSameAs(page);
        assertThat(sut.findAllNotices(pageable)).isSameAs(page);
        assertThat(sut.save(target)).isSameAs(target);
        sut.delete(target);
        sut.incrementViewCount(1L);
        assertThat(sut.findNoticeReadByNoticeId(1L)).containsExactly(read);
        assertThat(sut.findUnreadChallengerIdByNoticeId(1L)).containsExactly(2L);
        assertThat(sut.existsRead(1L, 2L)).isTrue();
        assertThat(sut.countReadsByNoticeId(1L)).isEqualTo(3L);
        assertThat(sut.countReadsByNoticeIds(List.of(1L))).containsEntry(1L, 3L);
        assertThat(sut.countReadsByChallengerIdIn(1L, List.of(2L))).isOne();
        assertThat(sut.saveRead(read)).isSameAs(read);
        sut.deleteRead(read);
        sut.deleteAllByNoticeId(1L);

        verify(noticeRepository).delete(target);
        verify(noticeRepository).incrementViewCount(1L);
        verify(readRepository).delete(read);
        verify(readRepository).deleteAllByNoticeId(1L);
    }
}
