package com.umc.product.chat.application.service.command;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.chat.application.policy.ChatReactionPolicy;
import com.umc.product.chat.application.policy.ChatRoomAccessPolicy;
import com.umc.product.chat.application.port.in.command.ManageChatMessageReactionUseCase;
import com.umc.product.chat.application.port.in.command.dto.ChangeChatMessageReactionCommand;
import com.umc.product.chat.application.port.in.command.dto.ChatReactionMutationResult;
import com.umc.product.chat.application.port.in.query.dto.ChatReactionInfo;
import com.umc.product.chat.application.port.out.LoadChatMessagePort;
import com.umc.product.chat.application.port.out.LoadChatMessageReactionPort;
import com.umc.product.chat.application.port.out.LoadChatRoomPort;
import com.umc.product.chat.application.port.out.SaveChatMessageReactionPort;
import com.umc.product.chat.domain.ChatMessage;
import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.chat.domain.event.ChatMessageReactionChangedEvent;
import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatReactionCommandService implements ManageChatMessageReactionUseCase {

    private final LoadChatRoomPort loadChatRoomPort;
    private final LoadChatMessagePort loadChatMessagePort;
    private final LoadChatMessageReactionPort loadChatMessageReactionPort;
    private final SaveChatMessageReactionPort saveChatMessageReactionPort;
    private final ChatRoomAccessPolicy chatRoomAccessPolicy;
    private final ChatReactionPolicy chatReactionPolicy;
    private final DomainEventPublisher domainEventPublisher;

    @Override
    public ChatReactionMutationResult add(ChangeChatMessageReactionCommand command) {
        return change(command, true);
    }

    @Override
    public ChatReactionMutationResult remove(ChangeChatMessageReactionCommand command) {
        return change(command, false);
    }

    private ChatReactionMutationResult change(ChangeChatMessageReactionCommand command, boolean add) {
        chatReactionPolicy.validate(command.emoji());
        loadChatRoomPort.getByIdForUpdate(command.roomId());
        chatRoomAccessPolicy.verifyMember(command.roomId(), command.memberId());
        ChatMessage message = loadChatMessagePort.getByIdAndRoomId(command.messageId(), command.roomId());
        if (message.isDeleted() || message.getContentType() == MessageContentType.SYSTEM) {
            throw new ChatDomainException(ChatErrorCode.CHAT_MESSAGE_REACTION_NOT_ALLOWED);
        }

        boolean changed = add
            ? saveChatMessageReactionPort.addIfAbsent(message.getId(), command.memberId(), command.emoji())
            : saveChatMessageReactionPort.remove(message.getId(), command.memberId(), command.emoji());
        if (changed) {
            domainEventPublisher.publish(ChatMessageReactionChangedEvent.of(
                command.roomId(),
                command.messageId(),
                command.memberId(),
                command.emoji(),
                add
            ));
        }
        return new ChatReactionMutationResult(
            command.messageId(),
            loadReactions(command.messageId(), command.memberId()),
            !changed
        );
    }

    private List<ChatReactionInfo> loadReactions(Long messageId, Long viewerMemberId) {
        return loadChatMessageReactionPort.summarizeByMessageIds(List.of(messageId), viewerMemberId).stream()
            .map(summary -> new ChatReactionInfo(
                summary.emoji(),
                summary.count(),
                summary.reactedByViewer()
            ))
            .toList();
    }
}
