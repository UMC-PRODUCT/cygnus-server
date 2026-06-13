package com.umc.product.chat.application.service.command;

import com.umc.product.chat.application.port.in.command.MarkChatRoomReadUseCase;
import com.umc.product.chat.application.port.in.command.SendChatMessageUseCase;
import com.umc.product.chat.application.port.in.command.dto.MarkChatRoomReadCommand;
import com.umc.product.chat.application.port.in.command.dto.SendChatMessageCommand;
import com.umc.product.chat.application.port.in.query.dto.ChatMessageInfo;
import com.umc.product.chat.application.policy.ChatRoomAccessPolicy;
import com.umc.product.chat.application.port.out.LoadChatMemberPort;
import com.umc.product.chat.application.port.out.LoadChatMessagePort;
import com.umc.product.chat.application.port.out.SaveChatMemberPort;
import com.umc.product.chat.application.port.out.SaveChatMessagePort;
import com.umc.product.chat.domain.ChatMember;
import com.umc.product.chat.domain.ChatMessage;
import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.chat.domain.event.ChatMessageCreatedEvent;
import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatMessageCommandService implements SendChatMessageUseCase, MarkChatRoomReadUseCase {

    private final SaveChatMessagePort saveChatMessagePort;
    private final LoadChatMessagePort loadChatMessagePort;
    private final LoadChatMemberPort loadChatMemberPort;
    private final SaveChatMemberPort saveChatMemberPort;
    private final ChatRoomAccessPolicy chatRoomAccessPolicy;
    private final DomainEventPublisher domainEventPublisher;

    /**
     * 메시지를 저장하고 생성 이벤트를 발행한다.
     * <p>
     * broadcast 및 문의 상태 전환은 이 이벤트를 수신하는 다른 컴포넌트가 처리한다. chat은 알지 못한다.
     * 이벤트 발행은 {@link DomainEventPublisher} 한 곳에만 위임하며, outbox 적재 / 인메모리 발행 분기는
     * 어댑터 구성(app.event-outbox.enabled)이 결정한다.
     */
    @Override
    public ChatMessageInfo send(SendChatMessageCommand command) {
        validate(command);

        // 방 멤버만 전송 가능
        chatRoomAccessPolicy.verifyMember(command.roomId(), command.senderMemberId());

        ChatMessage saved = saveChatMessagePort.save(ChatMessage.create(
            command.roomId(),
            command.senderMemberId(),
            command.contentType(),
            command.content(),
            command.fileMetadataIds()
        ));

        domainEventPublisher.publish(ChatMessageCreatedEvent.from(saved));

        return ChatMessageInfo.from(saved);
    }

    /**
     * 방을 현재 최신 메시지까지 읽음 처리한다.
     * <p>
     * 읽음 위치는 클라이언트 값이 아니라 서버가 조회한 방 최신 메시지 id를 기준으로 한다(조작 불가).
     * 메시지가 아직 없는 방이면 갱신 없이 종료한다.
     */
    @Override
    public void markRead(MarkChatRoomReadCommand command) {
        ChatMember member = loadChatMemberPort.getByRoomIdAndMemberId(command.roomId(), command.memberId());
        loadChatMessagePort.findLatestMessageId(command.roomId())
            .ifPresent(member::markRead);
        saveChatMemberPort.save(member);
    }

    private void validate(SendChatMessageCommand command) {
        // SYSTEM 메시지는 서버 내부에서만 생성한다(클라이언트 전송 불가).
        if (command.contentType() == MessageContentType.SYSTEM) {
            throw new ChatDomainException(ChatErrorCode.CHAT_MESSAGE_INVALID_CONTENT_TYPE);
        }
        boolean noContent = command.content() == null || command.content().isBlank();
        boolean noFiles = command.fileMetadataIds() == null || command.fileMetadataIds().isEmpty();
        if (noContent && noFiles) {
            throw new ChatDomainException(ChatErrorCode.CHAT_MESSAGE_EMPTY);
        }
    }
}
