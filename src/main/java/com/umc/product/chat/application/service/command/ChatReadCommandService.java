package com.umc.product.chat.application.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.chat.application.policy.ChatRoomAccessPolicy;
import com.umc.product.chat.application.port.in.command.UpdateChatReadUseCase;
import com.umc.product.chat.application.port.in.command.dto.ChatReadMutationResult;
import com.umc.product.chat.application.port.in.command.dto.UpdateChatReadCommand;
import com.umc.product.chat.application.port.out.LoadChatMemberPort;
import com.umc.product.chat.application.port.out.LoadChatMessagePort;
import com.umc.product.chat.application.port.out.LoadChatRoomPort;
import com.umc.product.chat.application.port.out.SaveChatMemberPort;
import com.umc.product.chat.domain.ChatMember;
import com.umc.product.chat.domain.event.ChatReadUpdatedEvent;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatReadCommandService implements UpdateChatReadUseCase {

    private final LoadChatRoomPort loadChatRoomPort;
    private final LoadChatMessagePort loadChatMessagePort;
    private final LoadChatMemberPort loadChatMemberPort;
    private final SaveChatMemberPort saveChatMemberPort;
    private final ChatRoomAccessPolicy chatRoomAccessPolicy;
    private final DomainEventPublisher domainEventPublisher;

    @Override
    public ChatReadMutationResult update(UpdateChatReadCommand command) {
        loadChatRoomPort.getByIdForUpdate(command.roomId());
        chatRoomAccessPolicy.verifyMember(command.roomId(), command.memberId());
        loadChatMessagePort.getByIdAndRoomId(command.lastReadMessageId(), command.roomId());
        ChatMember member = loadChatMemberPort.getByRoomIdAndMemberId(command.roomId(), command.memberId());

        Long current = member.getLastReadMessageId();
        if (current != null && current >= command.lastReadMessageId()) {
            return new ChatReadMutationResult(command.roomId(), command.memberId(), current, true);
        }

        saveChatMemberPort.bumpLastReadMessageId(
            command.roomId(),
            command.memberId(),
            command.lastReadMessageId()
        );
        domainEventPublisher.publish(ChatReadUpdatedEvent.of(
            command.roomId(),
            command.memberId(),
            command.lastReadMessageId()
        ));
        return new ChatReadMutationResult(
            command.roomId(),
            command.memberId(),
            command.lastReadMessageId(),
            false
        );
    }
}
