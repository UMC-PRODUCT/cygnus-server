package com.umc.product.chat.application.service.command;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.chat.application.authorization.ChatPolicyAction;
import com.umc.product.chat.application.policy.ChatMessagePayloadFingerprint;
import com.umc.product.chat.application.policy.ChatRoomAccessPolicy;
import com.umc.product.chat.application.policy.CommunityChatMessagePolicy;
import com.umc.product.chat.application.port.in.command.CreateChatMessageUseCase;
import com.umc.product.chat.application.port.in.command.EditChatMessageUseCase;
import com.umc.product.chat.application.port.in.command.TombstoneChatMessageUseCase;
import com.umc.product.chat.application.port.in.command.dto.ChatMessageMutationResult;
import com.umc.product.chat.application.port.in.command.dto.CreateChatMessageCommand;
import com.umc.product.chat.application.port.in.command.dto.EditChatMessageCommand;
import com.umc.product.chat.application.port.in.command.dto.TombstoneChatMessageCommand;
import com.umc.product.chat.application.port.out.LoadChatMemberPort;
import com.umc.product.chat.application.port.out.LoadChatMessageMentionPort;
import com.umc.product.chat.application.port.out.LoadChatMessagePort;
import com.umc.product.chat.application.port.out.LoadChatRoomPort;
import com.umc.product.chat.application.port.out.SaveChatMemberPort;
import com.umc.product.chat.application.port.out.SaveChatMessageMentionPort;
import com.umc.product.chat.application.port.out.SaveChatMessagePort;
import com.umc.product.chat.application.port.out.SaveChatMessageReactionPort;
import com.umc.product.chat.application.service.query.ChatMessageInfoAssembler;
import com.umc.product.chat.domain.ChatMember;
import com.umc.product.chat.domain.ChatMessage;
import com.umc.product.chat.domain.event.ChatMessageCreatedEvent;
import com.umc.product.chat.domain.event.ChatMessageDeletedEvent;
import com.umc.product.chat.domain.event.ChatMessageUpdatedEvent;
import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatMessageLifecycleCommandService implements
    CreateChatMessageUseCase,
    EditChatMessageUseCase,
    TombstoneChatMessageUseCase {

    private final SaveChatMessagePort saveChatMessagePort;
    private final LoadChatMessagePort loadChatMessagePort;
    private final LoadChatRoomPort loadChatRoomPort;
    private final LoadChatMemberPort loadChatMemberPort;
    private final SaveChatMemberPort saveChatMemberPort;
    private final LoadChatMessageMentionPort loadChatMessageMentionPort;
    private final SaveChatMessageMentionPort saveChatMessageMentionPort;
    private final SaveChatMessageReactionPort saveChatMessageReactionPort;
    private final CommunityChatMessagePolicy communityChatMessagePolicy;
    private final ChatRoomAccessPolicy chatRoomAccessPolicy;
    private final ChatMessageInfoAssembler chatMessageInfoAssembler;
    private final DomainEventPublisher domainEventPublisher;

    @Override
    public ChatMessageMutationResult create(CreateChatMessageCommand command) {
        communityChatMessagePolicy.validateCreate(command);
        String payloadFingerprint = ChatMessagePayloadFingerprint.from(command);
        loadChatRoomPort.getByIdForUpdate(command.roomId());
        chatRoomAccessPolicy.verifyMember(
            ChatPolicyAction.MESSAGE_CREATE,
            command.roomId(),
            command.senderMemberId());
        validateReply(command.roomId(), command.replyToMessageId());
        validateMentions(command.roomId(), command.mentionedMemberIds());

        Optional<ChatMessage> existing = loadChatMessagePort
            .findByRoomIdAndSenderMemberIdAndClientMessageId(
                command.roomId(),
                command.senderMemberId(),
                command.clientMessageId()
            );
        if (existing.isPresent()) {
            return replay(existing.get(), command, payloadFingerprint);
        }

        communityChatMessagePolicy.validateAttachments(command);
        ChatMessage saved = saveChatMessagePort.save(ChatMessage.create(
            command.roomId(),
            command.senderMemberId(),
            command.contentType(),
            command.content(),
            command.fileMetadataIds(),
            command.replyToMessageId(),
            command.clientMessageId(),
            payloadFingerprint
        ));
        saveChatMessageMentionPort.saveAll(saved.getId(), command.mentionedMemberIds());
        saveChatMemberPort.bumpLastReadMessageId(
            command.roomId(),
            command.senderMemberId(),
            saved.getId()
        );
        domainEventPublisher.publish(ChatMessageCreatedEvent.from(saved, command.mentionedMemberIds()));

        return new ChatMessageMutationResult(
            chatMessageInfoAssembler.assemble(saved, command.senderMemberId()),
            false
        );
    }

    @Override
    public ChatMessageMutationResult edit(EditChatMessageCommand command) {
        communityChatMessagePolicy.validateEdit(command.content());
        loadChatRoomPort.getByIdForUpdate(command.roomId());
        ChatMessage message = loadChatMessagePort.getByIdAndRoomId(command.messageId(), command.roomId());
        chatRoomAccessPolicy.verifyMessageMutation(
            ChatPolicyAction.MESSAGE_UPDATE,
            command.roomId(),
            command.editorMemberId(),
            message.isAuthoredBy(command.editorMemberId()),
            false);
        if (!message.editContent(command.content())) {
            return new ChatMessageMutationResult(
                chatMessageInfoAssembler.assemble(message, command.editorMemberId()),
                true
            );
        }

        ChatMessage saved = saveChatMessagePort.save(message);
        List<Long> mentions = loadChatMessageMentionPort.listMemberIdsByMessageId(saved.getId());
        domainEventPublisher.publish(ChatMessageUpdatedEvent.from(saved, mentions));
        return new ChatMessageMutationResult(
            chatMessageInfoAssembler.assemble(saved, command.editorMemberId()),
            false
        );
    }

    @Override
    public ChatMessageMutationResult tombstone(TombstoneChatMessageCommand command) {
        loadChatRoomPort.getByIdForUpdate(command.roomId());
        ChatMessage message = loadChatMessagePort.getByIdAndRoomId(command.messageId(), command.roomId());
        chatRoomAccessPolicy.verifyMessageMutation(
            ChatPolicyAction.MESSAGE_DELETE,
            command.roomId(),
            command.requesterMemberId(),
            message.isAuthoredBy(command.requesterMemberId()),
            command.moderator());
        if (!message.tombstone()) {
            return new ChatMessageMutationResult(
                chatMessageInfoAssembler.assemble(message, command.requesterMemberId()),
                true
            );
        }

        ChatMessage saved = saveChatMessagePort.save(message);
        saveChatMessageMentionPort.deleteByMessageId(saved.getId());
        saveChatMessageReactionPort.deleteByMessageId(saved.getId());
        domainEventPublisher.publish(ChatMessageDeletedEvent.from(saved));
        return new ChatMessageMutationResult(
            chatMessageInfoAssembler.assemble(saved, command.requesterMemberId()),
            false
        );
    }

    private ChatMessageMutationResult replay(
        ChatMessage existing,
        CreateChatMessageCommand command,
        String payloadFingerprint
    ) {
        List<Long> storedMentions = loadChatMessageMentionPort.listMemberIdsByMessageId(existing.getId()).stream()
            .distinct()
            .sorted()
            .toList();
        boolean legacyPayloadMatches = existing.getClientPayloadFingerprint() == null
            && existing.hasCanonicalPayload(
                command.contentType(),
                command.content(),
                command.fileMetadataIds(),
                command.replyToMessageId()
            )
            && storedMentions.equals(command.mentionedMemberIds());
        if (!existing.hasClientPayloadFingerprint(payloadFingerprint) && !legacyPayloadMatches) {
            throw new ChatDomainException(ChatErrorCode.CHAT_MESSAGE_IDEMPOTENCY_CONFLICT);
        }
        return new ChatMessageMutationResult(
            chatMessageInfoAssembler.assemble(existing, command.senderMemberId()),
            true
        );
    }

    private void validateReply(Long roomId, Long replyToMessageId) {
        if (replyToMessageId != null
            && !loadChatMessagePort.existsByIdAndRoomId(replyToMessageId, roomId)) {
            throw new ChatDomainException(ChatErrorCode.CHAT_MESSAGE_INVALID_REPLY_TARGET);
        }
    }

    private void validateMentions(Long roomId, List<Long> mentionedMemberIds) {
        if (mentionedMemberIds.isEmpty()) {
            return;
        }
        Set<Long> activeMembers = new HashSet<>(loadChatMemberPort.listByRoomId(roomId).stream()
            .map(ChatMember::getMemberId)
            .toList());
        if (!activeMembers.containsAll(mentionedMemberIds)) {
            throw new ChatDomainException(ChatErrorCode.CHAT_MESSAGE_INVALID_MENTION);
        }
    }
}
