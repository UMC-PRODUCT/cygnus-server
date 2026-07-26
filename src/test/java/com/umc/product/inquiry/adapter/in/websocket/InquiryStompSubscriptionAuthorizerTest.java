package com.umc.product.inquiry.adapter.in.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.inquiry.application.port.out.LoadInquiryPort;
import com.umc.product.inquiry.application.port.out.LoadOperatorStatusPort;
import com.umc.product.inquiry.application.port.out.dto.LoadOperatorStatusContext;
import com.umc.product.inquiry.domain.Inquiry;
import com.umc.product.inquiry.domain.enums.InquiryCategory;
import com.umc.product.inquiry.domain.enums.InquiryTarget;
import com.umc.product.inquiry.domain.exception.InquiryDomainException;
import com.umc.product.inquiry.domain.exception.InquiryErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("InquiryStompSubscriptionAuthorizer")
class InquiryStompSubscriptionAuthorizerTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long INQUIRY_ID = 42L;
    private static final Long CHAT_ROOM_ID = 100L;

    @Mock
    LoadInquiryPort loadInquiryPort;

    @Mock
    LoadOperatorStatusPort loadOperatorStatusPort;

    @InjectMocks
    InquiryStompSubscriptionAuthorizer sut;

    // ───────────────────────────────────────────────────────────────────────
    // supports
    // ───────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("supports")
    class Supports {

        @Test
        @DisplayName("/topic/inquiry/{숫자} 형식이면 true를 반환한다")
        void 올바른_destination이면_true를_반환한다() {
            assertThat(sut.supports("/topic/inquiry/42")).isTrue();
        }

        @Test
        @DisplayName("prefix가 다른 destination이면 false를 반환한다")
        void 다른_prefix이면_false를_반환한다() {
            assertThat(sut.supports("/topic/chat/rooms/1/messages")).isFalse();
        }

        @Test
        @DisplayName("숫자 이외 세그먼트가 붙은 경우 false를 반환한다")
        void 세그먼트가_추가된_경우_false를_반환한다() {
            assertThat(sut.supports("/topic/inquiry/42/extra")).isFalse();
        }

        @Test
        @DisplayName("prefix만 있고 숫자가 없으면 false를 반환한다")
        void 숫자_없으면_false를_반환한다() {
            assertThat(sut.supports("/topic/inquiry/")).isFalse();
        }

        @Test
        @DisplayName("null destination이면 false를 반환한다")
        void null이면_false를_반환한다() {
            assertThat(sut.supports(null)).isFalse();
        }
    }

    // ───────────────────────────────────────────────────────────────────────
    // isAuthorized — 권한 있음
    // ───────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isAuthorized — 권한 있음")
    class IsAuthorizedGranted {

        @Test
        @DisplayName("문의 작성자이면 true를 반환한다")
        void 작성자이면_true를_반환한다() {
            // given
            Inquiry inquiry = createInquiry(MEMBER_ID);    // authorMemberId = MEMBER_ID
            given(loadInquiryPort.getById(INQUIRY_ID)).willReturn(inquiry);
            given(loadOperatorStatusPort.isOperator(LoadOperatorStatusContext.of(MEMBER_ID, inquiry)))
                .willReturn(false);

            // when
            boolean result = sut.isAuthorized(MEMBER_ID, "/topic/inquiry/" + INQUIRY_ID);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("운영진이면 true를 반환한다")
        void 운영진이면_true를_반환한다() {
            // given
            Long operatorId = 99L;
            Inquiry inquiry = createInquiry(MEMBER_ID);    // 작성자는 MEMBER_ID
            given(loadInquiryPort.getById(INQUIRY_ID)).willReturn(inquiry);
            given(loadOperatorStatusPort.isOperator(LoadOperatorStatusContext.of(operatorId, inquiry)))
                .willReturn(true);

            // when
            boolean result = sut.isAuthorized(operatorId, "/topic/inquiry/" + INQUIRY_ID);

            // then
            assertThat(result).isTrue();
        }
    }

    // ───────────────────────────────────────────────────────────────────────
    // isAuthorized — 거부
    // ───────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isAuthorized — 거부")
    class IsAuthorizedDenied {

        @Test
        @DisplayName("작성자도 운영진도 아니면 false를 반환한다")
        void 권한없는_멤버이면_false를_반환한다() {
            // given
            Long strangerId = 77L;
            Inquiry inquiry = createInquiry(MEMBER_ID);    // 작성자는 MEMBER_ID
            given(loadInquiryPort.getById(INQUIRY_ID)).willReturn(inquiry);
            given(loadOperatorStatusPort.isOperator(LoadOperatorStatusContext.of(strangerId, inquiry)))
                .willReturn(false);

            // when
            boolean result = sut.isAuthorized(strangerId, "/topic/inquiry/" + INQUIRY_ID);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("존재하지 않는 inquiryId이면 false를 반환한다")
        void 존재하지_않는_inquiryId이면_false를_반환한다() {
            // given
            given(loadInquiryPort.getById(INQUIRY_ID))
                .willThrow(new InquiryDomainException(InquiryErrorCode.INQUIRY_NOT_FOUND));

            // when
            boolean result = sut.isAuthorized(MEMBER_ID, "/topic/inquiry/" + INQUIRY_ID);

            // then
            assertThat(result).isFalse();
            then(loadOperatorStatusPort).should(never()).isOperator(org.mockito.ArgumentMatchers.any());
        }

        @Test
        @DisplayName("destination에서 inquiryId 파싱에 실패하면 false를 반환한다")
        void 잘못된_destination이면_false를_반환한다() {
            // when
            boolean result = sut.isAuthorized(MEMBER_ID, "/topic/inquiry/not-a-number");

            // then
            assertThat(result).isFalse();
            then(loadInquiryPort).should(never()).getById(org.mockito.ArgumentMatchers.any());
        }
    }

    // ───────────────────────────────────────────────────────────────────────
    // helpers
    // ───────────────────────────────────────────────────────────────────────

    private Inquiry createInquiry(Long authorMemberId) {
        return Inquiry.create(
            "테스트 제목",
            "테스트 내용",
            InquiryCategory.BUG_REPORT,
            InquiryTarget.CENTRAL,
            CHAT_ROOM_ID,
            authorMemberId,
            null,
            null,
            10L
        );
    }
}
