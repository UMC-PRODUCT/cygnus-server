package com.umc.product.inquiry.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.chat.application.port.in.query.GetChatMessagesForAuthorizedCallerUseCase;
import com.umc.product.chat.application.port.in.query.ListChatRoomSummariesUseCase;
import com.umc.product.chat.application.port.in.query.dto.ChatMessageCursorResult;
import com.umc.product.chat.application.port.in.query.dto.ChatRoomSummaryInfo;
import com.umc.product.chat.application.port.in.query.dto.GetChatMessagesForAuthorizedCallerQuery;
import com.umc.product.global.response.CursorResponse;
import com.umc.product.inquiry.application.access.InquiryAccessScope;
import com.umc.product.inquiry.application.access.InquiryAccessScopeResolver;
import com.umc.product.inquiry.application.port.in.query.dto.GetInquiryListQuery;
import com.umc.product.inquiry.application.port.in.query.dto.GetInquiryMessagesQuery;
import com.umc.product.inquiry.application.port.in.query.dto.InquirySummaryInfo;
import com.umc.product.inquiry.application.port.out.LoadInquiryPort;
import com.umc.product.inquiry.application.port.out.LoadOperatorStatusPort;
import com.umc.product.inquiry.application.port.out.dto.LoadOperatorStatusContext;
import com.umc.product.inquiry.domain.Inquiry;
import com.umc.product.inquiry.domain.enums.InquiryCategory;
import com.umc.product.inquiry.domain.enums.InquiryTarget;
import com.umc.product.inquiry.domain.exception.InquiryDomainException;
import com.umc.product.inquiry.domain.exception.InquiryErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("InquiryQueryService")
class InquiryQueryServiceTest {

    @Mock
    InquiryAccessScopeResolver scopeResolver;
    @Mock
    LoadInquiryPort loadInquiryPort;
    @Mock
    LoadOperatorStatusPort loadOperatorStatusPort;
    @Mock
    GetChatMessagesForAuthorizedCallerUseCase getChatMessagesForAuthorizedCallerUseCase;
    @Mock
    ListChatRoomSummariesUseCase listChatRoomSummariesUseCase;

    @InjectMocks
    InquiryQueryService sut;

    private static final Long MEMBER_ID = 1L;

    // -----------------------------------------------------------------------
    // getList — 페이징 및 미읽음 최적화
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("getList")
    class GetList {

        @Test
        @DisplayName("size+1개 rows가 반환되면 hasNext=true이고 page는 size개다")
        void hasNext_true_when_rows_exceed_size() {
            // given
            int size = 2;
            List<Inquiry> rows = List.of(inquiry(101L), inquiry(102L), inquiry(103L)); // size+1=3
            GetInquiryListQuery query = query(size);
            given(scopeResolver.resolve(MEMBER_ID))
                .willReturn(new InquiryAccessScope.OwnedOnly(MEMBER_ID));
            given(loadInquiryPort.listByScope(any(), eq(query))).willReturn(rows);
            given(listChatRoomSummariesUseCase.listRoomSummaries(any(), any())).willReturn(List.of());

            // when
            CursorResponse<InquirySummaryInfo> result = sut.getList(query);

            // then
            assertThat(result.hasNext()).isTrue();
            assertThat(result.content()).hasSize(size);
        }

        @Test
        @DisplayName("rows가 size 이하이면 hasNext=false이고 전체가 반환된다")
        void hasNext_false_when_rows_within_size() {
            // given
            int size = 5;
            List<Inquiry> rows = List.of(inquiry(101L), inquiry(102L));
            GetInquiryListQuery query = query(size);
            given(scopeResolver.resolve(MEMBER_ID))
                .willReturn(new InquiryAccessScope.OwnedOnly(MEMBER_ID));
            given(loadInquiryPort.listByScope(any(), eq(query))).willReturn(rows);
            given(listChatRoomSummariesUseCase.listRoomSummaries(any(), any())).willReturn(List.of());

            // when
            CursorResponse<InquirySummaryInfo> result = sut.getList(query);

            // then
            assertThat(result.hasNext()).isFalse();
            assertThat(result.content()).hasSize(2);
        }

        @Test
        @DisplayName("미읽음 조회는 page 기준 roomIds로만 호출된다 — hasNext용 마지막 1건 제외")
        void buildUnreadMap_호출_시_page_기준_roomIds만_전달된다() {
            // given
            int size = 2;
            // rows = size+1 개. 마지막(103L)은 hasNext 판별용이므로 미읽음 조회 대상에서 빠져야 한다.
            Inquiry first = inquiry(101L);
            Inquiry second = inquiry(102L);
            Inquiry extra = inquiry(103L);
            List<Inquiry> rows = List.of(first, second, extra);
            GetInquiryListQuery query = query(size);

            given(scopeResolver.resolve(MEMBER_ID))
                .willReturn(new InquiryAccessScope.OwnedOnly(MEMBER_ID));
            given(loadInquiryPort.listByScope(any(), eq(query))).willReturn(rows);
            given(listChatRoomSummariesUseCase.listRoomSummaries(any(), any())).willReturn(List.of());

            // when
            sut.getList(query);

            // then — listRoomSummaries에 넘긴 roomIds에 extra(103L)의 chatRoomId가 없어야 한다
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Long>> roomIdsCaptor = ArgumentCaptor.forClass(List.class);
            then(listChatRoomSummariesUseCase).should()
                .listRoomSummaries(eq(MEMBER_ID), roomIdsCaptor.capture());

            List<Long> capturedRoomIds = roomIdsCaptor.getValue();
            assertThat(capturedRoomIds).hasSize(size);
            assertThat(capturedRoomIds).doesNotContain(extra.getChatRoomId());
        }

        @Test
        @DisplayName("rows가 비어 있으면 미읽음 조회를 하지 않는다")
        void 빈_rows이면_미읽음_조회_없음() {
            // given
            GetInquiryListQuery query = query(5);
            given(scopeResolver.resolve(MEMBER_ID))
                .willReturn(new InquiryAccessScope.OwnedOnly(MEMBER_ID));
            given(loadInquiryPort.listByScope(any(), eq(query))).willReturn(List.of());

            // when
            CursorResponse<InquirySummaryInfo> result = sut.getList(query);

            // then
            then(listChatRoomSummariesUseCase).should(never()).listRoomSummaries(any(), any());
            assertThat(result.content()).isEmpty();
            assertThat(result.hasNext()).isFalse();
        }

        @Test
        @DisplayName("미읽음 수는 chatRoomId 기준으로 content에 매핑된다")
        void 미읽음_수_content에_매핑() {
            // given
            Inquiry inquiry = inquiry(101L);
            GetInquiryListQuery query = query(5);
            given(scopeResolver.resolve(MEMBER_ID))
                .willReturn(new InquiryAccessScope.OwnedOnly(MEMBER_ID));
            given(loadInquiryPort.listByScope(any(), eq(query))).willReturn(List.of(inquiry));
            given(listChatRoomSummariesUseCase.listRoomSummaries(eq(MEMBER_ID), any()))
                .willReturn(List.of(new ChatRoomSummaryInfo(inquiry.getChatRoomId(), null, 3L)));

            // when
            CursorResponse<InquirySummaryInfo> result = sut.getList(query);

            // then
            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).unreadCount()).isEqualTo(3L);
        }
    }

    // -----------------------------------------------------------------------
    // verifyAccess
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("접근 권한 검증")
    class VerifyAccess {

        @Test
        @DisplayName("작성자는 별도 운영진 판정 없이 문의를 조회할 수 있다")
        void 작성자는_운영진_판정_없이_접근_가능() {
            // given
            Inquiry inquiry = inquiry(101L);  // authorMemberId = MEMBER_ID
            given(loadInquiryPort.getById(INQUIRY_ID)).willReturn(inquiry);
            given(listChatRoomSummariesUseCase.listRoomSummaries(any(), any())).willReturn(List.of());

            // when — 예외 없이 통과
            sut.getById(new com.umc.product.inquiry.application.port.in.query.dto.GetInquiryQuery(INQUIRY_ID, MEMBER_ID));

            // then
            then(loadOperatorStatusPort).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("운영진은 본인이 작성하지 않은 문의도 조회할 수 있다")
        void 운영진은_타인_문의_접근_가능() {
            // given
            Long otherMemberId = 99L;
            Inquiry inquiry = inquiry(101L); // authorMemberId = MEMBER_ID
            given(loadInquiryPort.getById(INQUIRY_ID)).willReturn(inquiry);
            given(loadOperatorStatusPort.isOperator(any(LoadOperatorStatusContext.class))).willReturn(true);
            given(listChatRoomSummariesUseCase.listRoomSummaries(any(), any())).willReturn(List.of());

            // when — 예외 없이 통과
            sut.getById(new com.umc.product.inquiry.application.port.in.query.dto.GetInquiryQuery(INQUIRY_ID, otherMemberId));

            // then
            then(loadOperatorStatusPort).should().isOperator(any());
        }

        @Test
        @DisplayName("작성자도 운영진도 아닌 제3자는 조회 시 권한 예외가 발생한다")
        void 제3자는_조회_시_권한_예외() {
            // given
            Long strangerId = 99L;
            Inquiry inquiry = inquiry(101L);
            given(loadInquiryPort.getById(INQUIRY_ID)).willReturn(inquiry);
            given(loadOperatorStatusPort.isOperator(any(LoadOperatorStatusContext.class))).willReturn(false);

            // when / then
            assertThatThrownBy(() ->
                sut.getById(new com.umc.product.inquiry.application.port.in.query.dto.GetInquiryQuery(INQUIRY_ID, strangerId)))
                .isInstanceOf(InquiryDomainException.class)
                .extracting(e -> ((InquiryDomainException) e).getBaseCode())
                .isEqualTo(InquiryErrorCode.NO_INQUIRY_PERMISSION);
        }
    }

    // -----------------------------------------------------------------------
    // getMessages — 담당자 지정·첫 메시지 이전에도 권한 있는 운영진/작성자는 조회 가능,
    // chat에는 membership 검사 없이 위임(ChatMember 미등록)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("getMessages")
    class GetMessages {

        @Test
        @DisplayName("권한 있는 운영진(담당자 아님)은 메시지를 조회할 수 있고, memberId 없이 권한위임형 조회만 호출된다")
        void 권한있는_운영진은_담당자_아니어도_메시지_조회_가능() {
            // given
            Long operatorMemberId = 99L;
            Inquiry inquiry = inquiry(101L); // authorMemberId = MEMBER_ID (operatorMemberId와 다름)
            given(loadInquiryPort.getById(INQUIRY_ID)).willReturn(inquiry);
            given(loadOperatorStatusPort.isOperator(any(LoadOperatorStatusContext.class))).willReturn(true);
            given(getChatMessagesForAuthorizedCallerUseCase.getMessages(any()))
                .willReturn(new ChatMessageCursorResult(List.of(), null, false));

            // when
            sut.getMessages(new GetInquiryMessagesQuery(INQUIRY_ID, operatorMemberId, null, 20));

            // then — chat에는 membership 검사가 없는 권한위임형 유스케이스만, roomId 기준으로 호출된다
            ArgumentCaptor<GetChatMessagesForAuthorizedCallerQuery> captor =
                ArgumentCaptor.forClass(GetChatMessagesForAuthorizedCallerQuery.class);
            then(getChatMessagesForAuthorizedCallerUseCase).should().getMessages(captor.capture());
            assertThat(captor.getValue().roomId()).isEqualTo(inquiry.getChatRoomId());
        }

        @Test
        @DisplayName("작성자는 메시지를 조회할 수 있다")
        void 작성자는_메시지_조회_가능() {
            // given
            Inquiry inquiry = inquiry(101L); // authorMemberId = MEMBER_ID
            given(loadInquiryPort.getById(INQUIRY_ID)).willReturn(inquiry);
            given(getChatMessagesForAuthorizedCallerUseCase.getMessages(any()))
                .willReturn(new ChatMessageCursorResult(List.of(), null, false));

            // when
            ChatMessageCursorResult result =
                sut.getMessages(new GetInquiryMessagesQuery(INQUIRY_ID, MEMBER_ID, null, 20));

            // then
            assertThat(result.hasNext()).isFalse();
            then(loadOperatorStatusPort).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("작성자도 운영진도 아닌 제3자는 verifyAccess에서 거부되고 chat까지 호출되지 않는다")
        void 제3자는_verifyAccess에서_거부되고_chat_미호출() {
            // given
            Long strangerId = 99L;
            Inquiry inquiry = inquiry(101L);
            given(loadInquiryPort.getById(INQUIRY_ID)).willReturn(inquiry);
            given(loadOperatorStatusPort.isOperator(any(LoadOperatorStatusContext.class))).willReturn(false);

            // when / then
            assertThatThrownBy(() ->
                sut.getMessages(new GetInquiryMessagesQuery(INQUIRY_ID, strangerId, null, 20)))
                .isInstanceOf(InquiryDomainException.class)
                .extracting(e -> ((InquiryDomainException) e).getBaseCode())
                .isEqualTo(InquiryErrorCode.NO_INQUIRY_PERMISSION);

            then(getChatMessagesForAuthorizedCallerUseCase).shouldHaveNoInteractions();
        }
    }

    // -----------------------------------------------------------------------
    // 헬퍼
    // -----------------------------------------------------------------------

    private static final Long INQUIRY_ID = 1L;
    private static final long BASE_CHAT_ROOM_ID = 200L;

    /** chatRoomId = BASE_CHAT_ROOM_ID + seq 로 고유 방 ID 부여 */
    private static Inquiry inquiry(long seq) {
        return Inquiry.create(
            "제목",
            "내용",
            InquiryCategory.ETC,
            InquiryTarget.CENTRAL,
            BASE_CHAT_ROOM_ID + seq,
            MEMBER_ID,
            null,
            null,
            10L
        );
    }

    private static GetInquiryListQuery query(int size) {
        return new GetInquiryListQuery(MEMBER_ID, null, size, null, null, null);
    }
}
