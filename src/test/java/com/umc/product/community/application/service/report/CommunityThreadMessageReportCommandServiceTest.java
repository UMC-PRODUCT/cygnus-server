package com.umc.product.community.application.service.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.chat.application.port.in.query.GetChatMessageRoomUseCase;
import com.umc.product.chat.application.port.in.query.GetChatMessageUseCase;
import com.umc.product.chat.application.port.in.query.dto.ChatMessageInfo;
import com.umc.product.chat.application.port.in.query.dto.GetChatMessageQuery;
import com.umc.product.chat.application.port.in.query.dto.GetChatMessageRoomQuery;
import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.community.application.port.in.command.thread.report.dto.ReportCommunityThreadMessageCommand;
import com.umc.product.community.application.port.in.query.thread.report.dto.CommunityThreadMessageReportReceiptInfo;
import com.umc.product.community.application.port.out.report.LoadReportPort;
import com.umc.product.community.application.port.out.report.SaveReportPort;
import com.umc.product.community.application.port.out.thread.LoadCommunityThreadMemberPort;
import com.umc.product.community.application.port.out.thread.LoadCommunityThreadPort;
import com.umc.product.community.domain.CommunityThread;
import com.umc.product.community.domain.CommunityThreadMember;
import com.umc.product.community.domain.Report;
import com.umc.product.community.domain.enums.ReportReason;
import com.umc.product.community.domain.exception.CommunityDomainException;
import com.umc.product.community.domain.exception.CommunityErrorCode;
import com.umc.product.global.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
@DisplayName("스레드 메시지 신고 명령 서비스")
class CommunityThreadMessageReportCommandServiceTest {

    private static final Long THREAD_ID = 11L;
    private static final Long CHAT_ROOM_ID = 22L;
    private static final Long MESSAGE_ID = 33L;
    private static final Long REQUESTER_MEMBER_ID = 44L;
    private static final Instant CREATED_AT = Instant.parse("2026-07-18T00:00:00Z");

    @Mock
    LoadCommunityThreadPort loadCommunityThreadPort;

    @Mock
    LoadCommunityThreadMemberPort loadCommunityThreadMemberPort;

    @Mock
    GetChatMessageRoomUseCase getChatMessageRoomUseCase;

    @Mock
    GetChatMessageUseCase getChatMessageUseCase;

    @Mock
    LoadReportPort loadReportPort;

    @Mock
    SaveReportPort saveReportPort;

    @InjectMocks
    CommunityThreadMessageReportCommandService sut;

    @Test
    @DisplayName("Challenger 기록이 없어도 ACTIVE 회원은 자신의 member ID로 메시지를 신고한다")
    void report_Challenger_기록이_없는_활성_회원도_member_ID로_신고한다() {
        // given
        CommunityThread thread = thread();
        CommunityThreadMember member = activeMember();
        Report persisted = mock(Report.class);
        given(persisted.getId()).willReturn(901L);
        given(persisted.getCreatedAt()).willReturn(CREATED_AT);
        given(getChatMessageRoomUseCase.getRoomId(new GetChatMessageRoomQuery(MESSAGE_ID)))
            .willReturn(CHAT_ROOM_ID);
        given(loadCommunityThreadPort.findByChatRoomId(CHAT_ROOM_ID)).willReturn(Optional.of(thread));
        given(loadCommunityThreadPort.findByIdForUpdate(THREAD_ID)).willReturn(Optional.of(thread));
        given(loadCommunityThreadMemberPort.findByThreadIdAndMemberId(THREAD_ID, REQUESTER_MEMBER_ID))
            .willReturn(Optional.of(member));
        given(getChatMessageUseCase.getMessage(
            new GetChatMessageQuery(CHAT_ROOM_ID, REQUESTER_MEMBER_ID, MESSAGE_ID)
        )).willReturn(chatMessage());
        given(loadReportPort.existsThreadMessageReport(REQUESTER_MEMBER_ID, MESSAGE_ID)).willReturn(false);
        given(saveReportPort.save(any(Report.class))).willReturn(persisted);

        // when
        CommunityThreadMessageReportReceiptInfo result = sut.report(command());

        // then
        assertThat(result.reportId()).isEqualTo(901L);
        assertThat(result.messageId()).isEqualTo(MESSAGE_ID);
        assertThat(result.reason()).isEqualTo(ReportReason.ABUSE);
        assertThat(result.createdAt()).isEqualTo(CREATED_AT);
        assertThat(Arrays.stream(CommunityThreadMessageReportReceiptInfo.class.getRecordComponents())
            .map(RecordComponent::getName)
            .toList())
            .doesNotContain("reporterId", "reporterMemberId");

        ArgumentCaptor<Report> reportCaptor = ArgumentCaptor.forClass(Report.class);
        then(saveReportPort).should().save(reportCaptor.capture());
        Report report = reportCaptor.getValue();
        assertThat(report.getReporterId()).isEqualTo(REQUESTER_MEMBER_ID);
        assertThat(report.getThreadId()).isEqualTo(THREAD_ID);
        assertThat(report.getTargetId()).isEqualTo(MESSAGE_ID);
        assertThat(report.getReasonCode()).isEqualTo(ReportReason.ABUSE);

        InOrder order = inOrder(
            getChatMessageRoomUseCase,
            loadCommunityThreadPort,
            loadCommunityThreadMemberPort,
            getChatMessageUseCase,
            loadReportPort,
            saveReportPort
        );
        order.verify(getChatMessageRoomUseCase)
            .getRoomId(new GetChatMessageRoomQuery(MESSAGE_ID));
        order.verify(loadCommunityThreadPort).findByChatRoomId(CHAT_ROOM_ID);
        order.verify(loadCommunityThreadPort).findByIdForUpdate(THREAD_ID);
        order.verify(loadCommunityThreadMemberPort)
            .findByThreadIdAndMemberId(THREAD_ID, REQUESTER_MEMBER_ID);
        order.verify(getChatMessageUseCase)
            .getMessage(new GetChatMessageQuery(CHAT_ROOM_ID, REQUESTER_MEMBER_ID, MESSAGE_ID));
        order.verify(loadReportPort).existsThreadMessageReport(REQUESTER_MEMBER_ID, MESSAGE_ID);
        order.verify(saveReportPort).save(any(Report.class));
    }

    @Test
    @DisplayName("메시지 room에 매핑된 스레드가 없으면 THREAD_NOT_FOUND로 종료한다")
    void report_스레드가_없으면_신고하지_않는다() {
        // given
        given(getChatMessageRoomUseCase.getRoomId(new GetChatMessageRoomQuery(MESSAGE_ID)))
            .willReturn(CHAT_ROOM_ID);
        given(loadCommunityThreadPort.findByChatRoomId(CHAT_ROOM_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> sut.report(command()))
            .isInstanceOf(CommunityDomainException.class)
            .extracting(error -> ((BusinessException) error).getBaseCode())
            .isEqualTo(CommunityErrorCode.THREAD_NOT_FOUND);

        verifyNoInteractions(
            loadCommunityThreadMemberPort,
            getChatMessageUseCase,
            loadReportPort,
            saveReportPort
        );
    }

    @Test
    @DisplayName("room reverse mapping 뒤 잠금 스레드의 chatRoomId가 다르면 멤버 검증 전에 THREAD_NOT_FOUND로 종료한다")
    void report_잠금_스레드_room이_다르면_접근할수없다() {
        // given
        CommunityThread mappedThread = mock(CommunityThread.class);
        given(mappedThread.getId()).willReturn(THREAD_ID);
        CommunityThread lockedThread = mock(CommunityThread.class);
        given(lockedThread.getChatRoomId()).willReturn(99L);
        given(getChatMessageRoomUseCase.getRoomId(new GetChatMessageRoomQuery(MESSAGE_ID)))
            .willReturn(CHAT_ROOM_ID);
        given(loadCommunityThreadPort.findByChatRoomId(CHAT_ROOM_ID)).willReturn(Optional.of(mappedThread));
        given(loadCommunityThreadPort.findByIdForUpdate(THREAD_ID)).willReturn(Optional.of(lockedThread));

        // when & then
        assertThatThrownBy(() -> sut.report(command()))
            .isInstanceOf(CommunityDomainException.class)
            .extracting(error -> ((BusinessException) error).getBaseCode())
            .isEqualTo(CommunityErrorCode.THREAD_NOT_FOUND);

        then(loadCommunityThreadMemberPort).shouldHaveNoInteractions();
        then(getChatMessageUseCase).shouldHaveNoInteractions();
        then(loadReportPort).shouldHaveNoInteractions();
        then(saveReportPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("room reverse mapping 뒤 잠금 스레드가 삭제되면 멤버 검증 전에 THREAD_NOT_FOUND로 종료한다")
    void report_잠금_스레드가_삭제되면_접근할수없다() {
        // given
        CommunityThread mappedThread = mock(CommunityThread.class);
        given(mappedThread.getId()).willReturn(THREAD_ID);
        CommunityThread lockedThread = mock(CommunityThread.class);
        given(lockedThread.getChatRoomId()).willReturn(CHAT_ROOM_ID);
        given(lockedThread.isDeleted()).willReturn(true);
        given(getChatMessageRoomUseCase.getRoomId(new GetChatMessageRoomQuery(MESSAGE_ID)))
            .willReturn(CHAT_ROOM_ID);
        given(loadCommunityThreadPort.findByChatRoomId(CHAT_ROOM_ID)).willReturn(Optional.of(mappedThread));
        given(loadCommunityThreadPort.findByIdForUpdate(THREAD_ID)).willReturn(Optional.of(lockedThread));

        // when & then
        assertThatThrownBy(() -> sut.report(command()))
            .isInstanceOf(CommunityDomainException.class)
            .extracting(error -> ((BusinessException) error).getBaseCode())
            .isEqualTo(CommunityErrorCode.THREAD_NOT_FOUND);

        then(loadCommunityThreadMemberPort).shouldHaveNoInteractions();
        then(getChatMessageUseCase).shouldHaveNoInteractions();
        then(loadReportPort).shouldHaveNoInteractions();
        then(saveReportPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("회원이 없으면 메시지/신고 포트를 호출하지 않고 THREAD_MEMBER_NOT_FOUND로 종료한다")
    void report_스레드_회원이_없으면_신고하지_않는다() {
        // given
        CommunityThread thread = thread();
        given(getChatMessageRoomUseCase.getRoomId(new GetChatMessageRoomQuery(MESSAGE_ID)))
            .willReturn(CHAT_ROOM_ID);
        given(loadCommunityThreadPort.findByChatRoomId(CHAT_ROOM_ID)).willReturn(Optional.of(thread));
        given(loadCommunityThreadPort.findByIdForUpdate(THREAD_ID)).willReturn(Optional.of(thread));
        given(loadCommunityThreadMemberPort.findByThreadIdAndMemberId(THREAD_ID, REQUESTER_MEMBER_ID))
            .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> sut.report(command()))
            .isInstanceOf(CommunityDomainException.class)
            .extracting(error -> ((BusinessException) error).getBaseCode())
            .isEqualTo(CommunityErrorCode.THREAD_MEMBER_NOT_FOUND);

        then(getChatMessageUseCase).shouldHaveNoInteractions();
        then(loadReportPort).shouldHaveNoInteractions();
        then(saveReportPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("LEFT 또는 KICKED 회원은 Chat 단건 조회 전에 THREAD_ACCESS_DENIED로 차단한다")
    void report_비활성_회원은_접근이_거부된다() {
        // given
        CommunityThread thread = thread();
        CommunityThreadMember inactiveMember = mock(CommunityThreadMember.class);
        given(inactiveMember.isActive()).willReturn(false);
        given(getChatMessageRoomUseCase.getRoomId(new GetChatMessageRoomQuery(MESSAGE_ID)))
            .willReturn(CHAT_ROOM_ID);
        given(loadCommunityThreadPort.findByChatRoomId(CHAT_ROOM_ID)).willReturn(Optional.of(thread));
        given(loadCommunityThreadPort.findByIdForUpdate(THREAD_ID)).willReturn(Optional.of(thread));
        given(loadCommunityThreadMemberPort.findByThreadIdAndMemberId(THREAD_ID, REQUESTER_MEMBER_ID))
            .willReturn(Optional.of(inactiveMember));

        // when & then
        assertThatThrownBy(() -> sut.report(command()))
            .isInstanceOf(CommunityDomainException.class)
            .extracting(error -> ((BusinessException) error).getBaseCode())
            .isEqualTo(CommunityErrorCode.THREAD_ACCESS_DENIED);

        then(getChatMessageUseCase).shouldHaveNoInteractions();
        then(loadReportPort).shouldHaveNoInteractions();
        then(saveReportPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("같은 회원이 같은 메시지를 다시 신고하면 Chat 검증 뒤 중복 충돌로 저장하지 않는다")
    void report_같은_회원의_메시지_중복_신고를_거부한다() {
        // given
        CommunityThread thread = thread();
        CommunityThreadMember member = activeMember();
        given(getChatMessageRoomUseCase.getRoomId(new GetChatMessageRoomQuery(MESSAGE_ID)))
            .willReturn(CHAT_ROOM_ID);
        given(loadCommunityThreadPort.findByChatRoomId(CHAT_ROOM_ID)).willReturn(Optional.of(thread));
        given(loadCommunityThreadPort.findByIdForUpdate(THREAD_ID)).willReturn(Optional.of(thread));
        given(loadCommunityThreadMemberPort.findByThreadIdAndMemberId(THREAD_ID, REQUESTER_MEMBER_ID))
            .willReturn(Optional.of(member));
        given(getChatMessageUseCase.getMessage(
            new GetChatMessageQuery(CHAT_ROOM_ID, REQUESTER_MEMBER_ID, MESSAGE_ID)
        )).willReturn(chatMessage());
        given(loadReportPort.existsThreadMessageReport(REQUESTER_MEMBER_ID, MESSAGE_ID)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> sut.report(command()))
            .isInstanceOf(CommunityDomainException.class)
            .extracting(error -> ((BusinessException) error).getBaseCode())
            .isEqualTo(CommunityErrorCode.REPORT_ALREADY_EXISTS);

        then(saveReportPort).should(never()).save(any(Report.class));
        InOrder order = inOrder(
            getChatMessageRoomUseCase,
            loadCommunityThreadPort,
            loadCommunityThreadMemberPort,
            getChatMessageUseCase,
            loadReportPort
        );
        order.verify(getChatMessageRoomUseCase)
            .getRoomId(new GetChatMessageRoomQuery(MESSAGE_ID));
        order.verify(loadCommunityThreadPort).findByChatRoomId(CHAT_ROOM_ID);
        order.verify(loadCommunityThreadPort).findByIdForUpdate(THREAD_ID);
        order.verify(loadCommunityThreadMemberPort)
            .findByThreadIdAndMemberId(THREAD_ID, REQUESTER_MEMBER_ID);
        order.verify(getChatMessageUseCase)
            .getMessage(new GetChatMessageQuery(CHAT_ROOM_ID, REQUESTER_MEMBER_ID, MESSAGE_ID));
        order.verify(loadReportPort).existsThreadMessageReport(REQUESTER_MEMBER_ID, MESSAGE_ID);
    }

    @Test
    @DisplayName("중복 사전 확인과 저장 사이의 동시 충돌도 REPORT_ALREADY_EXISTS로 전달한다")
    void report_저장_시점_동시_중복_충돌을_그대로_전달한다() {
        // given
        CommunityThread thread = thread();
        CommunityThreadMember member = activeMember();
        CommunityDomainException duplicate = new CommunityDomainException(
            CommunityErrorCode.REPORT_ALREADY_EXISTS
        );
        given(getChatMessageRoomUseCase.getRoomId(new GetChatMessageRoomQuery(MESSAGE_ID)))
            .willReturn(CHAT_ROOM_ID);
        given(loadCommunityThreadPort.findByChatRoomId(CHAT_ROOM_ID)).willReturn(Optional.of(thread));
        given(loadCommunityThreadPort.findByIdForUpdate(THREAD_ID)).willReturn(Optional.of(thread));
        given(loadCommunityThreadMemberPort.findByThreadIdAndMemberId(THREAD_ID, REQUESTER_MEMBER_ID))
            .willReturn(Optional.of(member));
        given(getChatMessageUseCase.getMessage(
            new GetChatMessageQuery(CHAT_ROOM_ID, REQUESTER_MEMBER_ID, MESSAGE_ID)
        )).willReturn(chatMessage());
        given(loadReportPort.existsThreadMessageReport(REQUESTER_MEMBER_ID, MESSAGE_ID)).willReturn(false);
        given(saveReportPort.save(any(Report.class))).willThrow(duplicate);

        // when & then
        assertThatThrownBy(() -> sut.report(command()))
            .isSameAs(duplicate)
            .extracting(error -> ((BusinessException) error).getBaseCode())
            .isEqualTo(CommunityErrorCode.REPORT_ALREADY_EXISTS);
        then(saveReportPort).should().save(any(Report.class));
    }

    private ReportCommunityThreadMessageCommand command() {
        return new ReportCommunityThreadMessageCommand(
            MESSAGE_ID,
            REQUESTER_MEMBER_ID,
            ReportReason.ABUSE
        );
    }

    private CommunityThread thread() {
        CommunityThread thread = mock(CommunityThread.class);
        given(thread.getId()).willReturn(THREAD_ID);
        given(thread.getChatRoomId()).willReturn(CHAT_ROOM_ID);
        given(thread.isDeleted()).willReturn(false);
        return thread;
    }

    private CommunityThreadMember activeMember() {
        CommunityThreadMember member = mock(CommunityThreadMember.class);
        given(member.isActive()).willReturn(true);
        return member;
    }

    private ChatMessageInfo chatMessage() {
        return new ChatMessageInfo(
            MESSAGE_ID,
            CHAT_ROOM_ID,
            66L,
            MessageContentType.TEXT,
            "신고 대상 메시지",
            List.of(),
            CREATED_AT
        );
    }

}
