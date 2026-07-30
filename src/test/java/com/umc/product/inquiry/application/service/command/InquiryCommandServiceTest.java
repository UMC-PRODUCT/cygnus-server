package com.umc.product.inquiry.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.chat.application.port.in.command.CreateChatRoomUseCase;
import com.umc.product.chat.application.port.in.command.JoinChatRoomUseCase;
import com.umc.product.chat.application.port.in.command.MarkChatRoomReadUseCase;
import com.umc.product.chat.application.port.in.query.CheckChatRoomAccessUseCase;
import com.umc.product.chat.application.port.in.query.GetChatMessagesForAuthorizedCallerUseCase;
import com.umc.product.chat.application.port.in.query.dto.ChatMessageCursorResult;
import com.umc.product.chat.application.port.in.query.dto.ChatMessageInfo;
import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.inquiry.application.port.in.command.dto.MarkInquiryReadCommand;
import com.umc.product.inquiry.application.port.out.LoadInquiryPort;
import com.umc.product.inquiry.application.port.out.LoadOperatorStatusPort;
import com.umc.product.inquiry.application.port.out.SaveInquiryPort;
import com.umc.product.inquiry.application.port.out.dto.LoadOperatorStatusContext;
import com.umc.product.inquiry.domain.Inquiry;
import com.umc.product.inquiry.domain.enums.InquiryCategory;
import com.umc.product.inquiry.domain.enums.InquiryTarget;
import com.umc.product.inquiry.domain.exception.InquiryDomainException;
import com.umc.product.inquiry.domain.exception.InquiryErrorCode;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;

@ExtendWith(MockitoExtension.class)
@DisplayName("InquiryCommandService")
class InquiryCommandServiceTest {

    @Mock
    SaveInquiryPort saveInquiryPort;
    @Mock
    GetGisuUseCase getGisuUseCase;
    @Mock
    CreateChatRoomUseCase createChatRoomUseCase;
    @Mock
    JoinChatRoomUseCase joinChatRoomUseCase;
    @Mock
    CheckChatRoomAccessUseCase checkChatRoomAccessUseCase;
    @Mock
    MarkChatRoomReadUseCase markChatRoomReadUseCase;
    @Mock
    GetChatMessagesForAuthorizedCallerUseCase getChatMessagesForAuthorizedCallerUseCase;
    @Mock
    LoadInquiryPort loadInquiryPort;
    @Mock
    LoadOperatorStatusPort loadOperatorStatusPort;

    @InjectMocks
    InquiryCommandService sut;

    private static final Long INQUIRY_ID = 1L;
    private static final Long CHAT_ROOM_ID = 100L;
    private static final Long AUTHOR_ID = 10L;
    private static final Long OPERATOR_ID = 20L;
    private static final Long STRANGER_ID = 99L;
    private static final Long LATEST_MESSAGE_ID = 500L;

    // -----------------------------------------------------------------------
    // markRead
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("markRead")
    class MarkRead {

        @Nested
        @DisplayName("운영진이 읽음 처리하면")
        class 운영진_읽음_처리 {

            @Test
            @DisplayName("isRead가 true로 갱신된다")
            void 운영진_markRead_시_isRead_true() {
                // given
                Inquiry inquiry = unreadInquiry();
                given(loadInquiryPort.getById(INQUIRY_ID)).willReturn(inquiry);
                given(loadOperatorStatusPort.isOperator(any(LoadOperatorStatusContext.class))).willReturn(true);
                given(getChatMessagesForAuthorizedCallerUseCase.getMessages(any())).willReturn(latestMessageResult());

                // when
                sut.markRead(MarkInquiryReadCommand.of(INQUIRY_ID, OPERATOR_ID));

                // then
                assertThat(inquiry.isRead()).isTrue();
            }

            @Test
            @DisplayName("markChatRoomReadUseCase를 호출한다")
            void 운영진_markRead_시_채팅방_읽음_처리() {
                // given
                Inquiry inquiry = unreadInquiry();
                given(loadInquiryPort.getById(INQUIRY_ID)).willReturn(inquiry);
                given(loadOperatorStatusPort.isOperator(any(LoadOperatorStatusContext.class))).willReturn(true);
                given(getChatMessagesForAuthorizedCallerUseCase.getMessages(any())).willReturn(latestMessageResult());

                // when
                sut.markRead(MarkInquiryReadCommand.of(INQUIRY_ID, OPERATOR_ID));

                // then
                then(markChatRoomReadUseCase).should().markRead(any());
            }

            @Test
            @DisplayName("inquiry를 저장한다")
            void 운영진_markRead_후_inquiry_저장() {
                // given
                Inquiry inquiry = unreadInquiry();
                given(loadInquiryPort.getById(INQUIRY_ID)).willReturn(inquiry);
                given(loadOperatorStatusPort.isOperator(any(LoadOperatorStatusContext.class))).willReturn(true);
                given(getChatMessagesForAuthorizedCallerUseCase.getMessages(any())).willReturn(latestMessageResult());

                // when
                sut.markRead(MarkInquiryReadCommand.of(INQUIRY_ID, OPERATOR_ID));

                // then
                then(saveInquiryPort).should().save(inquiry);
            }
        }

        @Nested
        @DisplayName("작성자가 읽음 처리하면")
        class 작성자_읽음_처리 {

            @Test
            @DisplayName("isRead는 변경되지 않는다 — 운영진 열람 플래그를 보존한다")
            void 작성자_markRead_시_isRead_불변() {
                // given: 운영진이 이미 읽은 상태
                Inquiry inquiry = unreadInquiry();
                inquiry.markAsRead();
                given(loadInquiryPort.getById(INQUIRY_ID)).willReturn(inquiry);
                given(loadOperatorStatusPort.isOperator(any(LoadOperatorStatusContext.class))).willReturn(false);
                given(getChatMessagesForAuthorizedCallerUseCase.getMessages(any())).willReturn(latestMessageResult());

                // when
                sut.markRead(MarkInquiryReadCommand.of(INQUIRY_ID, AUTHOR_ID));

                // then — 작성자 호출로 isRead(true)가 false로 바뀌면 안 된다
                assertThat(inquiry.isRead()).isTrue();
            }

            @Test
            @DisplayName("미읽음 상태에서도 isRead는 false로 유지된다")
            void 작성자_markRead_시_isRead_false_유지() {
                // given
                Inquiry inquiry = unreadInquiry();
                given(loadInquiryPort.getById(INQUIRY_ID)).willReturn(inquiry);
                given(loadOperatorStatusPort.isOperator(any(LoadOperatorStatusContext.class))).willReturn(false);
                given(getChatMessagesForAuthorizedCallerUseCase.getMessages(any())).willReturn(latestMessageResult());

                // when
                sut.markRead(MarkInquiryReadCommand.of(INQUIRY_ID, AUTHOR_ID));

                // then
                assertThat(inquiry.isRead()).isFalse();
            }

            @Test
            @DisplayName("markChatRoomReadUseCase를 호출한다")
            void 작성자_markRead_시_채팅방_읽음_처리() {
                // given
                Inquiry inquiry = unreadInquiry();
                given(loadInquiryPort.getById(INQUIRY_ID)).willReturn(inquiry);
                given(loadOperatorStatusPort.isOperator(any(LoadOperatorStatusContext.class))).willReturn(false);
                given(getChatMessagesForAuthorizedCallerUseCase.getMessages(any())).willReturn(latestMessageResult());

                // when
                sut.markRead(MarkInquiryReadCommand.of(INQUIRY_ID, AUTHOR_ID));

                // then
                then(markChatRoomReadUseCase).should().markRead(any());
            }

            @Test
            @DisplayName("inquiry를 저장한다")
            void 작성자_markRead_후_inquiry_저장() {
                // given
                Inquiry inquiry = unreadInquiry();
                given(loadInquiryPort.getById(INQUIRY_ID)).willReturn(inquiry);
                given(loadOperatorStatusPort.isOperator(any(LoadOperatorStatusContext.class))).willReturn(false);
                given(getChatMessagesForAuthorizedCallerUseCase.getMessages(any())).willReturn(latestMessageResult());

                // when
                sut.markRead(MarkInquiryReadCommand.of(INQUIRY_ID, AUTHOR_ID));

                // then
                then(saveInquiryPort).should().save(inquiry);
            }
        }

        @Nested
        @DisplayName("제3자가 읽음 처리하면")
        class 제3자_읽음_처리 {

            @Test
            @DisplayName("NO_INQUIRY_PERMISSION 예외가 발생한다")
            void 제3자_markRead_시_예외() {
                // given
                Inquiry inquiry = unreadInquiry();
                given(loadInquiryPort.getById(INQUIRY_ID)).willReturn(inquiry);
                given(loadOperatorStatusPort.isOperator(any(LoadOperatorStatusContext.class))).willReturn(false);

                // when & then
                assertThatThrownBy(() ->
                    sut.markRead(MarkInquiryReadCommand.of(INQUIRY_ID, STRANGER_ID)))
                    .isInstanceOf(InquiryDomainException.class)
                    .extracting(e -> ((InquiryDomainException) e).getBaseCode())
                    .isEqualTo(InquiryErrorCode.NO_INQUIRY_PERMISSION);
            }

            @Test
            @DisplayName("채팅방 읽음 처리와 저장이 호출되지 않는다")
            void 제3자_markRead_시_사이드이펙트_없음() {
                // given
                Inquiry inquiry = unreadInquiry();
                given(loadInquiryPort.getById(INQUIRY_ID)).willReturn(inquiry);
                given(loadOperatorStatusPort.isOperator(any(LoadOperatorStatusContext.class))).willReturn(false);

                // when
                try {
                    sut.markRead(MarkInquiryReadCommand.of(INQUIRY_ID, STRANGER_ID));
                } catch (InquiryDomainException ignored) {
                }

                // then
                then(markChatRoomReadUseCase).shouldHaveNoInteractions();
                then(saveInquiryPort).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("빈 채팅방(메시지 없음)에서 읽음 처리하면")
        class 빈_채팅방_읽음_처리 {

            @Test
            @DisplayName("운영진이면 isRead는 true로 갱신되고 채팅방 읽음은 생략된다")
            void 빈_방_운영진_isRead_갱신() {
                // given
                Inquiry inquiry = unreadInquiry();
                given(loadInquiryPort.getById(INQUIRY_ID)).willReturn(inquiry);
                given(loadOperatorStatusPort.isOperator(any(LoadOperatorStatusContext.class))).willReturn(true);
                given(getChatMessagesForAuthorizedCallerUseCase.getMessages(any())).willReturn(emptyMessageResult());

                // when
                sut.markRead(MarkInquiryReadCommand.of(INQUIRY_ID, OPERATOR_ID));

                // then
                assertThat(inquiry.isRead()).isTrue();
                then(markChatRoomReadUseCase).shouldHaveNoInteractions();
                then(saveInquiryPort).should().save(inquiry);
            }

            @Test
            @DisplayName("작성자이면 채팅방 읽음 생략, isRead 불변, 저장은 진행된다")
            void 빈_방_작성자_저장_진행() {
                // given
                Inquiry inquiry = unreadInquiry();
                given(loadInquiryPort.getById(INQUIRY_ID)).willReturn(inquiry);
                given(loadOperatorStatusPort.isOperator(any(LoadOperatorStatusContext.class))).willReturn(false);
                given(getChatMessagesForAuthorizedCallerUseCase.getMessages(any())).willReturn(emptyMessageResult());

                // when
                sut.markRead(MarkInquiryReadCommand.of(INQUIRY_ID, AUTHOR_ID));

                // then
                assertThat(inquiry.isRead()).isFalse();
                then(markChatRoomReadUseCase).shouldHaveNoInteractions();
                then(saveInquiryPort).should().save(inquiry);
            }
        }
    }

    // -----------------------------------------------------------------------
    // 헬퍼
    // -----------------------------------------------------------------------

    private Inquiry unreadInquiry() {
        return Inquiry.create(
            "테스트 문의",
            "문의 내용",
            InquiryCategory.ETC,
            InquiryTarget.CENTRAL,
            CHAT_ROOM_ID,
            AUTHOR_ID,
            null,
            null,
            10L
        );
    }

    private ChatMessageCursorResult latestMessageResult() {
        ChatMessageInfo message = new ChatMessageInfo(
            LATEST_MESSAGE_ID,
            CHAT_ROOM_ID,
            OPERATOR_ID,
            MessageContentType.TEXT,
            "최신 메시지",
            List.of(),
            Instant.now()
        );
        return new ChatMessageCursorResult(List.of(message), null, false);
    }

    private ChatMessageCursorResult emptyMessageResult() {
        return new ChatMessageCursorResult(List.of(), null, false);
    }
}
