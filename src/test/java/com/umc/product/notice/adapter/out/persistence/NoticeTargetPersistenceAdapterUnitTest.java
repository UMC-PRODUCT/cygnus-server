package com.umc.product.notice.adapter.out.persistence;

import static com.umc.product.support.fixture.NoticeUnitFixture.target;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.notice.domain.NoticeTarget;

@DisplayName("NoticeTargetPersistenceAdapter 단위 테스트")
class NoticeTargetPersistenceAdapterUnitTest {

    @Test
    @DisplayName("대상 조회·저장·삭제의 모든 port 계약을 repository에 위임한다")
    void delegates_all_target_contracts() {
        NoticeTargetJpaRepository repository = mock(NoticeTargetJpaRepository.class);
        NoticeTargetPersistenceAdapter sut = new NoticeTargetPersistenceAdapter(repository);
        NoticeTarget target = target();
        given(repository.findByNoticeId(1L)).willReturn(Optional.of(target));
        given(repository.findByNoticeIdIn(List.of(1L))).willReturn(List.of(target));
        given(repository.findByTargetGisuId(1L)).willReturn(List.of(target));
        given(repository.findByTargetChapterId(2L)).willReturn(List.of(target));
        given(repository.findByTargetSchoolId(3L)).willReturn(List.of(target));
        given(repository.existsByNoticeId(1L)).willReturn(true);
        given(repository.save(target)).willReturn(target);

        assertThat(sut.findByNoticeId(1L)).contains(target);
        assertThat(sut.findByNoticeIdIn(List.of(1L))).containsExactly(target);
        assertThat(sut.findByTargetGisuId(1L)).containsExactly(target);
        assertThat(sut.findByTargetChapterId(2L)).containsExactly(target);
        assertThat(sut.findByTargetSchoolId(3L)).containsExactly(target);
        assertThat(sut.existsByNoticeId(1L)).isTrue();
        assertThat(sut.save(target)).isSameAs(target);
        sut.delete(target);
        sut.deleteByNoticeId(1L);

        verify(repository).delete(target);
        verify(repository).deleteByNoticeId(1L);
    }
}
