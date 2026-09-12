package com.umc.product.inquiry.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.umc.product.inquiry.domain.enums.InquiryCategory;
import com.umc.product.inquiry.domain.enums.InquiryStatus;
import com.umc.product.inquiry.domain.enums.InquiryTarget;
import com.umc.product.inquiry.domain.exception.InquiryDomainException;
import com.umc.product.inquiry.domain.exception.InquiryErrorCode;

class InquiryTest {

    private static final Long AUTHOR_ID = 1L;
    private static final Long OPERATOR_ID = 2L;
    private static final Long STRANGER_ID = 3L;

    // -----------------------------------------------------------------------
    // markAsUnread
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("markAsUnread")
    class MarkAsUnread {

        @Test
        @DisplayName("isRead가 true이면 false로 바뀐다")
        void isRead가_true이면_false로_바뀐다() {
            // given
            Inquiry inquiry = inquiry();
            inquiry.markAsRead();
            assertThat(inquiry.isRead()).isTrue();

            // when
            inquiry.markAsUnread();

            // then
            assertThat(inquiry.isRead()).isFalse();
        }

        @Test
        @DisplayName("isRead가 이미 false이면 false 그대로다 (멱등)")
        void isRead가_이미_false이면_그대로다() {
            // given
            Inquiry inquiry = inquiry();
            assertThat(inquiry.isRead()).isFalse();

            // when
            inquiry.markAsUnread();

            // then
            assertThat(inquiry.isRead()).isFalse();
        }
    }

    // -----------------------------------------------------------------------
    // isAccessibleBy
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("isAccessibleBy")
    class IsAccessibleBy {

        @Test
        @DisplayName("작성자는 운영진 여부와 관계없이 접근할 수 있다")
        void 작성자는_운영진_여부와_관계없이_접근할_수_있다() {
            Inquiry inquiry = inquiry();

            assertThat(inquiry.isAccessibleBy(AUTHOR_ID, false)).isTrue();
            assertThat(inquiry.isAccessibleBy(AUTHOR_ID, true)).isTrue();
        }

        @Test
        @DisplayName("운영진은 작성자가 아니어도 접근할 수 있다")
        void 운영진은_작성자가_아니어도_접근할_수_있다() {
            Inquiry inquiry = inquiry();

            assertThat(inquiry.isAccessibleBy(OPERATOR_ID, true)).isTrue();
        }

        @Test
        @DisplayName("작성자도 운영진도 아닌 제3자는 접근할 수 없다")
        void 제3자는_접근할_수_없다() {
            Inquiry inquiry = inquiry();

            assertThat(inquiry.isAccessibleBy(STRANGER_ID, false)).isFalse();
        }
    }

    // -----------------------------------------------------------------------
    // reopen
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("reopen")
    class Reopen {

        @Test
        @DisplayName("CLOSED 문의를 재오픈하면 isRead가 false로 초기화된다")
        void CLOSED_문의를_재오픈하면_isRead가_false로_초기화된다() {
            // given
            Inquiry inquiry = inquiry();
            inquiry.startProgress();
            inquiry.markAsRead();
            inquiry.close();
            assertThat(inquiry.isRead()).isTrue();

            // when
            inquiry.reopen();

            // then
            assertThat(inquiry.isRead()).isFalse();
        }
    }

    // -----------------------------------------------------------------------
    // startProgress
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("startProgress")
    class StartProgress {

        @Test
        @DisplayName("RECEIVED 상태에서 호출하면 IN_PROGRESS로 전환된다")
        void RECEIVED_상태에서_호출하면_IN_PROGRESS로_전환된다() {
            // given
            Inquiry inquiry = inquiry();
            assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.RECEIVED);

            // when
            inquiry.startProgress();

            // then
            assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("IN_PROGRESS 상태에서 호출하면 예외가 발생한다")
        void IN_PROGRESS_상태에서_호출하면_예외가_발생한다() {
            // given
            Inquiry inquiry = inquiry();
            inquiry.startProgress();

            // when & then
            assertThatThrownBy(inquiry::startProgress)
                .isInstanceOf(InquiryDomainException.class)
                .extracting("baseCode")
                .isEqualTo(InquiryErrorCode.INQUIRY_INVALID_STATUS_FOR_PROGRESS);
        }

        @Test
        @DisplayName("CLOSED 상태에서 호출하면 예외가 발생한다")
        void CLOSED_상태에서_호출하면_예외가_발생한다() {
            // given
            Inquiry inquiry = inquiry();
            inquiry.startProgress();
            inquiry.close();

            // when & then
            assertThatThrownBy(inquiry::startProgress)
                .isInstanceOf(InquiryDomainException.class)
                .extracting("baseCode")
                .isEqualTo(InquiryErrorCode.INQUIRY_INVALID_STATUS_FOR_PROGRESS);
        }
    }

    // -----------------------------------------------------------------------
    // close
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("close")
    class Close {

        @Test
        @DisplayName("RECEIVED 상태에서 호출하면 CLOSED로 전환된다")
        void RECEIVED_상태에서_호출하면_CLOSED로_전환된다() {
            // given
            Inquiry inquiry = inquiry();

            // when
            inquiry.close();

            // then
            assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.CLOSED);
        }

        @Test
        @DisplayName("IN_PROGRESS 상태에서 호출하면 CLOSED로 전환된다")
        void IN_PROGRESS_상태에서_호출하면_CLOSED로_전환된다() {
            // given
            Inquiry inquiry = inquiry();
            inquiry.startProgress();

            // when
            inquiry.close();

            // then
            assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.CLOSED);
        }

        @Test
        @DisplayName("이미 CLOSED 상태에서 호출하면 예외가 발생한다")
        void 이미_CLOSED_상태에서_호출하면_예외가_발생한다() {
            // given
            Inquiry inquiry = inquiry();
            inquiry.close();

            // when & then
            assertThatThrownBy(inquiry::close)
                .isInstanceOf(InquiryDomainException.class)
                .extracting("baseCode")
                .isEqualTo(InquiryErrorCode.INQUIRY_ALREADY_CLOSED);
        }
    }

    // -----------------------------------------------------------------------
    // reopen 예외 케이스
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("reopen 예외")
    class ReopenException {

        @Test
        @DisplayName("RECEIVED 상태에서 reopen을 호출하면 예외가 발생한다")
        void RECEIVED_상태에서_reopen_호출_시_예외가_발생한다() {
            // given
            Inquiry inquiry = inquiry();

            // when & then
            assertThatThrownBy(inquiry::reopen)
                .isInstanceOf(InquiryDomainException.class)
                .extracting("baseCode")
                .isEqualTo(InquiryErrorCode.INQUIRY_INVALID_STATUS_FOR_REOPEN);
        }

        @Test
        @DisplayName("IN_PROGRESS 상태에서 reopen을 호출하면 예외가 발생한다")
        void IN_PROGRESS_상태에서_reopen_호출_시_예외가_발생한다() {
            // given
            Inquiry inquiry = inquiry();
            inquiry.startProgress();

            // when & then
            assertThatThrownBy(inquiry::reopen)
                .isInstanceOf(InquiryDomainException.class)
                .extracting("baseCode")
                .isEqualTo(InquiryErrorCode.INQUIRY_INVALID_STATUS_FOR_REOPEN);
        }
    }

    // -----------------------------------------------------------------------
    // 헬퍼
    // -----------------------------------------------------------------------

    private static Inquiry inquiry() {
        return Inquiry.create(
            "테스트 문의 제목",
            "테스트 문의 내용",
            InquiryCategory.ETC,
            InquiryTarget.CENTRAL,
            100L,       // chatRoomId
            AUTHOR_ID,
            null,       // targetSchoolId
            null,       // targetChapterId
            10L         // targetGisuId
        );
    }
}
