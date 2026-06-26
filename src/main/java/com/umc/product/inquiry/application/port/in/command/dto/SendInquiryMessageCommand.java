package com.umc.product.inquiry.application.port.in.command.dto;

import com.umc.product.chat.domain.MessageContentType;
import java.util.List;

/**
 * 문의 채팅방으로의 메시지 전송 명령.
 * <p>
 * chat의 {@code SendChatMessageCommand}와 필드를 정합시킨다. senderMemberId가 곧 발신자이며,
 * 운영진/문의자 판정은 서비스에서 OperatorChecker를 통해 별도로 수행한다.
 */
public record SendInquiryMessageCommand(
    Long chatRoomId,
    Long senderMemberId,
    MessageContentType contentType,
    String content,
    List<String> fileMetadataIds
) {
}
