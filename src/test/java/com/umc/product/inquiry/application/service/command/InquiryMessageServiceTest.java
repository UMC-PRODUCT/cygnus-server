package com.umc.product.inquiry.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.chat.application.port.in.command.JoinChatRoomUseCase;
import com.umc.product.chat.application.port.in.command.SendChatMessageUseCase;
import com.umc.product.chat.application.port.in.query.CheckChatRoomAccessUseCase;
import com.umc.product.chat.application.port.in.query.dto.ChatMessageInfo;
import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.inquiry.application.port.in.command.dto.SendInquiryMessageCommand;
import com.umc.product.inquiry.application.port.out.LoadInquiryPort;
import com.umc.product.inquiry.application.port.out.LoadOperatorStatusPort;
import com.umc.product.inquiry.application.port.out.SaveInquiryPort;
import com.umc.product.inquiry.application.port.out.dto.LoadOperatorStatusContext;
import com.umc.product.inquiry.domain.Inquiry;
import com.umc.product.inquiry.domain.enums.InquiryCategory;
import com.umc.product.inquiry.domain.enums.InquiryStatus;
import com.umc.product.inquiry.domain.enums.InquiryTarget;

@ExtendWith(MockitoExtension.class)
@DisplayName("InquiryMessageService")
class InquiryMessageServiceTest {

    @Mock
    SendChatMessageUseCase sendChatMessageUseCase;
    @Mock
    JoinChatRoomUseCase joinChatRoomUseCase;
    @Mock
    CheckChatRoomAccessUseCase checkChatRoomAccessUseCase;
    @Mock
    LoadInquiryPort loadInquiryPort;
    @Mock
    SaveInquiryPort saveInquiryPort;
    @Mock
    LoadOperatorStatusPort loadOperatorStatusPort;

    @InjectMocks
    InquiryMessageService sut;

    private static final Long INQUIRY_ID = 1L;
    private static final Long CHAT_ROOM_ID = 100L;
    private static final Long AUTHOR_ID = 10L;
    private static final Long OPERATOR_ID = 20L;

    // -----------------------------------------------------------------------
    // 문의자 메시지 전송
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("문의자가 메시지를 보내면")
    class 문의자_메시지 {

        @Test
        @DisplayName("RECEIVED 상태에서 문의자가 메시지를 보내면 isRead가 false로 유지된다")
        void RECEIVED_상태에서_문의자_메시지_전송_시_미읽음_유지() {
            // given
            Inquiry inquiry = receivedInquiry();
            inquiry.markAsRead(); // 운영진이 이미 읽은 상태
            given(loadInquiryPort.getById(INQUIRY_ID)).willReturn(inquiry);
            given(loadOperatorStatusPort.isOperator(any(LoadOperatorStatusContext.class))).willReturn(false);
            given(sendChatMessageUseCase.send(any())).willReturn(stubMessageInfo());

            // when
            sut.send(command(AUTHOR_ID));

            // then
            assertThat(inquiry.isRead()).isFalse();
        }

        @Test
        @DisplayName("IN_PROGRESS 상태에서 문의자가 메시지를 보내면 isRead가 false가 된다")
        void IN_PROGRESS_상태에서_문의자_메시지_전송_시_미읽음_전환() {
            // given
            Inquiry inquiry = receivedInquiry();
            inquiry.startProgress();
            inquiry.markAsRead();
            given(loadInquiryPort.getById(INQUIRY_ID)).willReturn(inquiry);
            given(loadOperatorStatusPort.isOperator(any(LoadOperatorStatusContext.class))).willReturn(false);
            given(sendChatMessageUseCase.send(any())).willReturn(stubMessageInfo());

            // when
            sut.send(command(AUTHOR_ID));

            // then
            assertThat(inquiry.isRead()).isFalse();
        }

        @Test
        @DisplayName("CLOSED 상태에서 문의자가 메시지를 보내면 reopen되고 isRead는 false다")
        void CLOSED_상태에서_문의자_메시지_전송_시_reopen_및_미읽음() {
            // given
            Inquiry inquiry = closedInquiry();
            inquiry.markAsRead();
            given(loadInquiryPort.getById(INQUIRY_ID)).willReturn(inquiry);
            given(loadOperatorStatusPort.isOperator(any(LoadOperatorStatusContext.class))).willReturn(false);
            given(sendChatMessageUseCase.send(any())).willReturn(stubMessageInfo());

            // when
            sut.send(command(AUTHOR_ID));

            // then
            assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.IN_PROGRESS);
            assertThat(inquiry.isRead()).isFalse();
        }
    }

    // -----------------------------------------------------------------------
    // 운영진 메시지 전송
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("운영진이 메시지를 보내면")
    class 운영진_메시지 {

        @Test
        @DisplayName("RECEIVED 상태에서 운영진이 메시지를 보내면 IN_PROGRESS로 전환되고 isRead가 true가 된다")
        void RECEIVED_상태에서_운영진_메시지_전송_시_상태전환_및_읽음() {
            // given
            Inquiry inquiry = receivedInquiry();
            given(loadInquiryPort.getById(INQUIRY_ID)).willReturn(inquiry);
            given(loadOperatorStatusPort.isOperator(any(LoadOperatorStatusContext.class))).willReturn(true);
            given(checkChatRoomAccessUseCase.hasChatRoomAccess(OPERATOR_ID, CHAT_ROOM_ID)).willReturn(true);
            given(sendChatMessageUseCase.send(any())).willReturn(stubMessageInfo());

            // when
            sut.send(command(OPERATOR_ID));

            // then
            assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.IN_PROGRESS);
            assertThat(inquiry.isRead()).isTrue();
        }

        @Test
        @DisplayName("CLOSED 상태에서 운영진이 메시지를 보내면 reopen 후 isRead는 true다")
        void CLOSED_상태에서_운영진_메시지_전송_시_reopen_후_읽음() {
            // given
            Inquiry inquiry = closedInquiry();
            given(loadInquiryPort.getById(INQUIRY_ID)).willReturn(inquiry);
            given(loadOperatorStatusPort.isOperator(any(LoadOperatorStatusContext.class))).willReturn(true);
            given(checkChatRoomAccessUseCase.hasChatRoomAccess(OPERATOR_ID, CHAT_ROOM_ID)).willReturn(true);
            given(sendChatMessageUseCase.send(any())).willReturn(stubMessageInfo());

            // when
            sut.send(command(OPERATOR_ID));

            // then — reopen()이 markAsUnread()를 호출하지만, 이후 markAsRead()가 덮어쓴다
            assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.IN_PROGRESS);
            assertThat(inquiry.isRead()).isTrue();
        }

        @Test
        @DisplayName("운영진이 채팅방 멤버가 아니면 joinChatRoom을 호출한다")
        void 운영진이_채팅방_멤버가_아니면_joinChatRoom_호출() {
            // given
            Inquiry inquiry = receivedInquiry();
            given(loadInquiryPort.getById(INQUIRY_ID)).willReturn(inquiry);
            given(loadOperatorStatusPort.isOperator(any(LoadOperatorStatusContext.class))).willReturn(true);
            given(checkChatRoomAccessUseCase.hasChatRoomAccess(OPERATOR_ID, CHAT_ROOM_ID)).willReturn(false);
            given(sendChatMessageUseCase.send(any())).willReturn(stubMessageInfo());

            // when
            sut.send(command(OPERATOR_ID));

            // then
            then(joinChatRoomUseCase).should().joinChatRoom(any());
        }

        @Test
        @DisplayName("운영진이 이미 채팅방 멤버이면 joinChatRoom을 호출하지 않는다")
        void 운영진이_이미_채팅방_멤버이면_joinChatRoom_미호출() {
            // given
            Inquiry inquiry = receivedInquiry();
            given(loadInquiryPort.getById(INQUIRY_ID)).willReturn(inquiry);
            given(loadOperatorStatusPort.isOperator(any(LoadOperatorStatusContext.class))).willReturn(true);
            given(checkChatRoomAccessUseCase.hasChatRoomAccess(OPERATOR_ID, CHAT_ROOM_ID)).willReturn(true);
            given(sendChatMessageUseCase.send(any())).willReturn(stubMessageInfo());

            // when
            sut.send(command(OPERATOR_ID));

            // then
            then(joinChatRoomUseCase).shouldHaveNoInteractions();
        }
    }

    // -----------------------------------------------------------------------
    // 공통 — 영속화
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("메시지 전송 후 반드시 inquiry를 저장한다")
    void 메시지_전송_후_inquiry_저장() {
        // given
        Inquiry inquiry = receivedInquiry();
        given(loadInquiryPort.getById(INQUIRY_ID)).willReturn(inquiry);
        given(loadOperatorStatusPort.isOperator(any(LoadOperatorStatusContext.class))).willReturn(false);
        given(sendChatMessageUseCase.send(any())).willReturn(stubMessageInfo());

        // when
        sut.send(command(AUTHOR_ID));

        // then
        then(saveInquiryPort).should().save(inquiry);
    }

    // -----------------------------------------------------------------------
    // 헬퍼
    // -----------------------------------------------------------------------

    private Inquiry receivedInquiry() {
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

    private Inquiry closedInquiry() {
        Inquiry inquiry = receivedInquiry();
        inquiry.startProgress();
        inquiry.close();
        return inquiry;
    }

    private SendInquiryMessageCommand command(Long senderMemberId) {
        return new SendInquiryMessageCommand(
            INQUIRY_ID,
            senderMemberId,
            MessageContentType.TEXT,
            "테스트 메시지",
            null
        );
    }

    private ChatMessageInfo stubMessageInfo() {
        return new ChatMessageInfo(1L, CHAT_ROOM_ID, AUTHOR_ID, MessageContentType.TEXT, "테스트 메시지", null,
            java.time.Instant.now());
    }
}
