package com.umc.product.inquiry.application.service.command;

import com.umc.product.chat.application.port.in.command.SendChatMessageUseCase;
import com.umc.product.chat.application.port.in.command.dto.SendChatMessageCommand;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final LoadInquiryPort loadInquiryPort;
    private final SaveInquiryPort saveInquiryPort;
    private final LoadOperatorStatusPort loadOperatorStatusPort;

    @Override
    public ChatMessageInfo send(SendInquiryMessageCommand command) {
        // 1) 이 채팅방의 문의 로드
        Inquiry inquiry = loadInquiryPort.getByChatRoomId(command.chatRoomId());

        // 2) 운영진의 첫 메시지면 상태 전환 (운영진 AND 현재 RECEIVED일 때만 호출)
        //    startProgress()는 RECEIVED가 아니면 예외를 던지므로 가드로 보호한다(예외를 흐름 제어에 쓰지 않음).
        boolean isOperator = loadOperatorStatusPort.isOperator(
            LoadOperatorStatusContext.of(command.senderMemberId(), inquiry));
        if (isOperator && inquiry.getStatus() == InquiryStatus.RECEIVED) {
            inquiry.startProgress();
        }

        // 3) 메시지 전송 (chat send 재사용 — DB 저장 + 이벤트 발행, broadcast는 AFTER_COMMIT)
        ChatMessageInfo result = sendChatMessageUseCase.send(
            new SendChatMessageCommand(
                command.chatRoomId(),
                command.senderMemberId(),
                command.contentType(),
                command.content(),
                command.fileMetadataIds()));

        // 4) 상태 변경 영속 (변경이 없었어도 save 호출은 무해)
        saveInquiryPort.save(inquiry);

        return result;
    }
}
