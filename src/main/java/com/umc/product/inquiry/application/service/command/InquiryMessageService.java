package com.umc.product.inquiry.application.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.chat.application.port.in.command.JoinChatRoomUseCase;
import com.umc.product.chat.application.port.in.command.SendChatMessageUseCase;
import com.umc.product.chat.application.port.in.command.dto.JoinChatRoomCommand;
import com.umc.product.chat.application.port.in.command.dto.SendChatMessageCommand;
import com.umc.product.chat.application.port.in.query.CheckChatRoomAccessUseCase;
import com.umc.product.chat.application.port.in.query.dto.ChatMessageInfo;
import com.umc.product.inquiry.application.port.in.command.SendInquiryMessageUseCase;
import com.umc.product.inquiry.application.port.in.command.dto.SendInquiryMessageCommand;
import com.umc.product.inquiry.application.port.out.LoadInquiryPort;
import com.umc.product.inquiry.application.port.out.LoadOperatorStatusPort;
import com.umc.product.inquiry.application.port.out.SaveInquiryPort;
import com.umc.product.inquiry.application.port.out.dto.LoadOperatorStatusContext;
import com.umc.product.inquiry.domain.Inquiry;
import com.umc.product.inquiry.domain.enums.InquiryStatus;

import lombok.RequiredArgsConstructor;

/**
 * 문의 메시지 전송 흐름을 단일 트랜잭션으로 엮는다.
 * <p>
 * "메시지 전송 → (운영진 첫 메시지면) 상태 전환 → 저장"을 하나의 @Transactional로 처리한다. broadcast/FCM 등 외부 호출은 직접 수행하지 않는다. chat의 send()가 발행하는
 * 이벤트를 기존 리스너가 AFTER_COMMIT에서 처리하므로 여기서는 send()만 호출한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class InquiryMessageService implements SendInquiryMessageUseCase {

    private final SendChatMessageUseCase sendChatMessageUseCase;
    private final JoinChatRoomUseCase joinChatRoomUseCase;
    private final CheckChatRoomAccessUseCase checkChatRoomAccessUseCase;
    private final LoadInquiryPort loadInquiryPort;
    private final SaveInquiryPort saveInquiryPort;
    private final LoadOperatorStatusPort loadOperatorStatusPort;

    @Override
    public ChatMessageInfo send(SendInquiryMessageCommand command) {
        // 1) inquiryId로 문의 로드 (컨트롤러에서 chatRoomId 변환 없이 직접 전달)
        Inquiry inquiry = loadInquiryPort.getById(command.inquiryId());
        Long chatRoomId = inquiry.getChatRoomId();

        // 2) 상태 전환
        //    - CLOSED: 발신자 구분 없이 reopen() (카카오톡 채널 방식)
        //    - RECEIVED + 운영진: startProgress()
        boolean isOperator = loadOperatorStatusPort.isOperator(
            LoadOperatorStatusContext.of(command.senderMemberId(), inquiry));
        if (inquiry.getStatus() == InquiryStatus.CLOSED) {
            inquiry.reopen();
        } else if (isOperator && inquiry.getStatus() == InquiryStatus.RECEIVED) {
            inquiry.startProgress();
        }

        // 3) 읽음 상태 갱신
        //    - 운영진 메시지: 채팅방 멤버 등록 후 열람 처리(isRead = true)
        //    - 문의자 메시지: 운영진이 아직 읽지 않은 상태로 전환(isRead = false)
        //      CLOSED → reopen() 이 이미 markAsUnread() 를 호출하지만, 중복 호출은 무해하며
        //      RECEIVED/IN_PROGRESS 케이스도 동일하게 처리하기 위해 else 브랜치에서 통합한다.
        if (isOperator) {
            if (!checkChatRoomAccessUseCase.hasChatRoomAccess(command.senderMemberId(), chatRoomId)) {
                joinChatRoomUseCase.joinChatRoom(new JoinChatRoomCommand(chatRoomId, command.senderMemberId()));
            }
            inquiry.markAsRead();
        } else {
            inquiry.markAsUnread();
        }

        // 4) 메시지 전송 (chat send 재사용 — DB 저장 + 이벤트 발행, broadcast는 AFTER_COMMIT)
        ChatMessageInfo result = sendChatMessageUseCase.send(
            new SendChatMessageCommand(
                chatRoomId,
                command.senderMemberId(),
                command.contentType(),
                command.content(),
                command.fileMetadataIds()));

        // 5) 상태 변경 영속 (변경이 없었어도 save 호출은 무해)
        saveInquiryPort.save(inquiry);

        return result;
    }
}
