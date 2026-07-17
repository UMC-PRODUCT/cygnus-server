package com.umc.product.notice.application.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.form.domain.FormOwnerReference;

@DisplayName("NoticeVoteOwnerReferenceFactory")
class NoticeVoteOwnerReferenceFactoryTest {

    @Test
    @DisplayName("Notice ID를 trusted notice.vote/{noticeId}/default 좌표로 변환한다")
    void createsTrustedNoticeVoteCoordinate() {
        FormOwnerReference reference = NoticeVoteOwnerReferenceFactory.forNotice(42L).create(900L);

        assertThat(NoticeVoteOwnerReferenceFactory.coordinate(42L))
            .isEqualTo("notice.vote/42/default");
        assertThat(reference).isEqualTo(FormOwnerReference.of(
            900L,
            NoticeVoteOwnerReferenceFactory.NAMESPACE,
            "42",
            NoticeVoteOwnerReferenceFactory.SLOT
        ));
    }

    @Test
    @DisplayName("Notice ID 또는 저장된 Form ID가 없으면 trusted 좌표를 만들지 않는다")
    void rejectsMissingIds() {
        assertThatThrownBy(() -> NoticeVoteOwnerReferenceFactory.forNotice(null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> NoticeVoteOwnerReferenceFactory.forNotice(0L))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> NoticeVoteOwnerReferenceFactory.forNotice(42L).create(null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
