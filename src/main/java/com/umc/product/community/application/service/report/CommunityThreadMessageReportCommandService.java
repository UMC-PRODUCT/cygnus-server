package com.umc.product.community.application.service.report;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.chat.application.port.in.query.GetChatMessageRoomUseCase;
import com.umc.product.chat.application.port.in.query.GetChatMessageUseCase;
import com.umc.product.chat.application.port.in.query.dto.GetChatMessageQuery;
import com.umc.product.chat.application.port.in.query.dto.GetChatMessageRoomQuery;
import com.umc.product.community.application.port.in.command.thread.report.ReportCommunityThreadMessageUseCase;
import com.umc.product.community.application.port.in.command.thread.report.dto.ReportCommunityThreadMessageCommand;
import com.umc.product.community.application.port.in.query.thread.report.dto.CommunityThreadMessageReportReceiptInfo;
import com.umc.product.community.application.port.out.report.LoadReportPort;
import com.umc.product.community.application.port.out.report.SaveReportPort;
import com.umc.product.community.application.port.out.thread.LoadCommunityThreadMemberPort;
import com.umc.product.community.application.port.out.thread.LoadCommunityThreadPort;
import com.umc.product.community.domain.CommunityThread;
import com.umc.product.community.domain.CommunityThreadMember;
import com.umc.product.community.domain.Report;
import com.umc.product.community.domain.exception.CommunityDomainException;
import com.umc.product.community.domain.exception.CommunityErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CommunityThreadMessageReportCommandService implements ReportCommunityThreadMessageUseCase {

    private final LoadCommunityThreadPort loadThreadPort;
    private final LoadCommunityThreadMemberPort loadThreadMemberPort;
    private final GetChatMessageRoomUseCase getChatMessageRoomUseCase;
    private final GetChatMessageUseCase getChatMessageUseCase;
    private final LoadReportPort loadReportPort;
    private final SaveReportPort saveReportPort;

    @Override
    public CommunityThreadMessageReportReceiptInfo report(ReportCommunityThreadMessageCommand command) {
        Long chatRoomId = getChatMessageRoomUseCase.getRoomId(new GetChatMessageRoomQuery(command.messageId()));
        CommunityThread mappedThread = loadThreadPort.findByChatRoomId(chatRoomId)
            .orElseThrow(() -> new CommunityDomainException(CommunityErrorCode.THREAD_NOT_FOUND));
        CommunityThread thread = loadThreadPort.findByIdForUpdate(mappedThread.getId())
            .filter(foundThread -> Objects.equals(foundThread.getChatRoomId(), chatRoomId))
            .filter(foundThread -> !foundThread.isDeleted())
            .orElseThrow(() -> new CommunityDomainException(CommunityErrorCode.THREAD_NOT_FOUND));

        CommunityThreadMember member = loadThreadMemberPort
            .findByThreadIdAndMemberId(thread.getId(), command.requesterMemberId())
            .orElseThrow(() -> new CommunityDomainException(CommunityErrorCode.THREAD_MEMBER_NOT_FOUND));
        if (!member.isActive()) {
            throw new CommunityDomainException(CommunityErrorCode.THREAD_ACCESS_DENIED);
        }

        getChatMessageUseCase.getMessage(new GetChatMessageQuery(
            chatRoomId,
            command.requesterMemberId(),
            command.messageId()
        ));

        if (loadReportPort.existsThreadMessageReport(command.requesterMemberId(), command.messageId())) {
            throw new CommunityDomainException(CommunityErrorCode.REPORT_ALREADY_EXISTS);
        }

        Report savedReport = saveReportPort.save(Report.createThreadMessage(
            command.requesterMemberId(),
            thread.getId(),
            command.messageId(),
            command.reason()
        ));
        return CommunityThreadMessageReportReceiptInfo.from(
            savedReport,
            command.messageId(),
            command.reason()
        );
    }
}
