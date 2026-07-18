package com.umc.product.community.application.service.message;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.chat.application.port.in.command.CreateChatMessageUseCase;
import com.umc.product.chat.application.port.in.command.EditChatMessageUseCase;
import com.umc.product.chat.application.port.in.command.ManageChatMessageReactionUseCase;
import com.umc.product.chat.application.port.in.command.TombstoneChatMessageUseCase;
import com.umc.product.chat.application.port.in.command.UpdateChatReadUseCase;
import com.umc.product.chat.application.port.in.command.dto.ChangeChatMessageReactionCommand;
import com.umc.product.chat.application.port.in.command.dto.ChatMessageMutationResult;
import com.umc.product.chat.application.port.in.command.dto.ChatReactionMutationResult;
import com.umc.product.chat.application.port.in.command.dto.ChatReadMutationResult;
import com.umc.product.chat.application.port.in.command.dto.CreateChatMessageCommand;
import com.umc.product.chat.application.port.in.command.dto.EditChatMessageCommand;
import com.umc.product.chat.application.port.in.command.dto.TombstoneChatMessageCommand;
import com.umc.product.chat.application.port.in.command.dto.UpdateChatReadCommand;
import com.umc.product.chat.application.port.in.query.ListChatRoomSummariesUseCase;
import com.umc.product.chat.application.port.in.query.dto.ChatMessageInfo;
import com.umc.product.chat.application.port.in.query.dto.ChatReactionInfo;
import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.community.application.event.CommunityThreadMentionedEvent;
import com.umc.product.community.application.event.CommunityThreadMessageCreatedEvent;
import com.umc.product.community.application.port.in.command.thread.message.CreateCommunityThreadMessageUseCase;
import com.umc.product.community.application.port.in.command.thread.message.EditCommunityThreadMessageUseCase;
import com.umc.product.community.application.port.in.command.thread.message.ManageCommunityThreadMessageReactionUseCase;
import com.umc.product.community.application.port.in.command.thread.message.TombstoneCommunityThreadMessageUseCase;
import com.umc.product.community.application.port.in.command.thread.message.UpdateCommunityThreadReadUseCase;
import com.umc.product.community.application.port.in.command.thread.message.dto.ChangeCommunityThreadMessageReactionCommand;
import com.umc.product.community.application.port.in.command.thread.message.dto.CreateCommunityThreadMessageCommand;
import com.umc.product.community.application.port.in.command.thread.message.dto.EditCommunityThreadMessageCommand;
import com.umc.product.community.application.port.in.command.thread.message.dto.TombstoneCommunityThreadMessageCommand;
import com.umc.product.community.application.port.in.command.thread.message.dto.UpdateCommunityThreadReadCommand;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageInfo;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageMutationInfo;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageType;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadReactionInfo;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadReactionMutationInfo;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadReadMutationInfo;
import com.umc.product.community.application.port.out.thread.LoadCommunityThreadMemberPort;
import com.umc.product.community.application.port.out.thread.LoadCommunityThreadPort;
import com.umc.product.community.application.port.out.thread.SaveCommunityThreadMemberPort;
import com.umc.product.community.application.port.out.thread.SaveCommunityThreadPort;
import com.umc.product.community.domain.CommunityThread;
import com.umc.product.community.domain.CommunityThreadMember;
import com.umc.product.community.domain.enums.CommunityThreadMemberRole;
import com.umc.product.community.domain.exception.CommunityDomainException;
import com.umc.product.community.domain.exception.CommunityErrorCode;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.global.event.domain.DomainEvent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CommunityThreadMessageCommandService implements
    CreateCommunityThreadMessageUseCase,
    EditCommunityThreadMessageUseCase,
    TombstoneCommunityThreadMessageUseCase,
    ManageCommunityThreadMessageReactionUseCase,
    UpdateCommunityThreadReadUseCase {

    private final LoadCommunityThreadPort loadThreadPort;
    private final LoadCommunityThreadMemberPort loadThreadMemberPort;
    private final SaveCommunityThreadPort saveThreadPort;
    private final SaveCommunityThreadMemberPort saveThreadMemberPort;
    private final CreateChatMessageUseCase createChatMessageUseCase;
    private final EditChatMessageUseCase editChatMessageUseCase;
    private final TombstoneChatMessageUseCase tombstoneChatMessageUseCase;
    private final ManageChatMessageReactionUseCase reactionUseCase;
    private final UpdateChatReadUseCase updateChatReadUseCase;
    private final ListChatRoomSummariesUseCase listChatRoomSummariesUseCase;
    private final CommunityThreadMessageInfoAssembler infoAssembler;
    private final DomainEventPublisher domainEventPublisher;

    @Override
    public CommunityThreadMessageMutationInfo create(CreateCommunityThreadMessageCommand command) {
        CommunityThread thread = loadLockedThread(command.threadId());
        loadActiveMember(thread.getId(), command.senderMemberId());
        ChatMessageMutationResult result = createChatMessageUseCase.create(new CreateChatMessageCommand(
            thread.getChatRoomId(),
            command.senderMemberId(),
            command.clientMessageId(),
            toChatType(command.type()),
            command.content(),
            command.fileMetadataIds(),
            command.mentionedMemberIds(),
            command.replyToMessageId()
        ));
        CommunityThreadMessageInfo message = infoAssembler.assemble(thread.getId(), result.message());
        if (result.deduplicated()) {
            return new CommunityThreadMessageMutationInfo(message, true);
        }

        ChatMessageInfo created = result.message();
        thread.updateLastMessage(
            created.messageId(),
            created.content(),
            created.senderMemberId(),
            created.createdAt()
        );
        List<CommunityThreadMember> activeMembers = loadThreadMemberPort.listByThreadId(thread.getId()).stream()
            .filter(CommunityThreadMember::isActive)
            .toList();
        activeMembers.forEach(member -> {
            if (member.getMemberId().equals(command.senderMemberId())) {
                member.resetUnreadCount();
            } else {
                member.incrementUnreadCount();
            }
        });
        saveThreadPort.save(thread);
        saveThreadMemberPort.saveAll(activeMembers);

        List<Long> recipientMemberIds = activeMembers.stream()
            .map(CommunityThreadMember::getMemberId)
            .filter(memberId -> !memberId.equals(command.senderMemberId()))
            .toList();
        Set<Long> recipients = new HashSet<>(recipientMemberIds);
        List<Long> mentionedMemberIds = command.mentionedMemberIds().stream()
            .filter(recipients::contains)
            .toList();
        DomainEvent createdFact = CommunityThreadMessageCreatedEvent.of(
            thread.getId(),
            created.messageId(),
            command.senderMemberId(),
            recipientMemberIds
        );
        List<? extends DomainEvent> facts = mentionedMemberIds.isEmpty()
            ? List.of(createdFact)
            : List.of(
                createdFact,
                CommunityThreadMentionedEvent.of(
                    thread.getId(),
                    created.messageId(),
                    command.senderMemberId(),
                    mentionedMemberIds
                )
            );
        domainEventPublisher.publishAll(facts);
        return new CommunityThreadMessageMutationInfo(message, false);
    }

    @Override
    public CommunityThreadMessageMutationInfo edit(EditCommunityThreadMessageCommand command) {
        CommunityThread thread = loadLockedThread(command.threadId());
        loadActiveMember(thread.getId(), command.requesterMemberId());
        ChatMessageMutationResult result = editChatMessageUseCase.edit(new EditChatMessageCommand(
            thread.getChatRoomId(),
            command.messageId(),
            command.requesterMemberId(),
            command.content()
        ));
        CommunityThreadMessageInfo message = infoAssembler.assemble(thread.getId(), result.message());
        refreshLastMessageProjection(thread, result);
        return new CommunityThreadMessageMutationInfo(message, result.deduplicated());
    }

    @Override
    public CommunityThreadMessageMutationInfo tombstone(
        TombstoneCommunityThreadMessageCommand command
    ) {
        CommunityThread thread = loadLockedThread(command.threadId());
        CommunityThreadMember requester = loadActiveMember(
            thread.getId(),
            command.requesterMemberId()
        );
        boolean moderator = requester.getRole() == CommunityThreadMemberRole.OWNER
            || requester.getRole() == CommunityThreadMemberRole.ADMIN;
        ChatMessageMutationResult result = tombstoneChatMessageUseCase.tombstone(
            new TombstoneChatMessageCommand(
                thread.getChatRoomId(),
                command.messageId(),
                command.requesterMemberId(),
                moderator
            )
        );
        CommunityThreadMessageInfo message = infoAssembler.assemble(thread.getId(), result.message());
        refreshLastMessageProjection(thread, result);
        return new CommunityThreadMessageMutationInfo(message, result.deduplicated());
    }

    @Override
    public CommunityThreadReactionMutationInfo add(
        ChangeCommunityThreadMessageReactionCommand command
    ) {
        CommunityThread thread = loadLockedThread(command.threadId());
        loadActiveMember(thread.getId(), command.memberId());
        return toCommunityResult(reactionUseCase.add(toChatCommand(thread, command)));
    }

    @Override
    public CommunityThreadReactionMutationInfo remove(
        ChangeCommunityThreadMessageReactionCommand command
    ) {
        CommunityThread thread = loadLockedThread(command.threadId());
        loadActiveMember(thread.getId(), command.memberId());
        return toCommunityResult(reactionUseCase.remove(toChatCommand(thread, command)));
    }

    @Override
    public CommunityThreadReadMutationInfo update(UpdateCommunityThreadReadCommand command) {
        CommunityThread thread = loadLockedThread(command.threadId());
        CommunityThreadMember member = loadActiveMember(thread.getId(), command.memberId());
        ChatReadMutationResult result = updateChatReadUseCase.update(new UpdateChatReadCommand(
            thread.getChatRoomId(),
            command.memberId(),
            command.lastReadMessageId()
        ));
        long unreadCount = listChatRoomSummariesUseCase.listRoomSummaries(
            command.memberId(),
            List.of(thread.getChatRoomId())
        ).stream()
            .filter(summary -> thread.getChatRoomId().equals(summary.roomId()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Chat room summary is missing"))
            .unreadCount();
        if (member.getUnreadCount() != unreadCount) {
            member.updateUnreadCount(unreadCount);
            saveThreadMemberPort.save(member);
        }
        return new CommunityThreadReadMutationInfo(
            thread.getId(),
            result.memberId(),
            result.lastReadMessageId(),
            result.deduplicated()
        );
    }

    private CommunityThread loadLockedThread(Long threadId) {
        CommunityThread thread = loadThreadPort.findByIdForUpdate(threadId)
            .orElseThrow(() -> new CommunityDomainException(CommunityErrorCode.THREAD_NOT_FOUND));
        if (thread.isDeleted()) {
            throw new CommunityDomainException(CommunityErrorCode.THREAD_NOT_FOUND);
        }
        return thread;
    }

    private CommunityThreadMember loadActiveMember(Long threadId, Long memberId) {
        CommunityThreadMember member = loadThreadMemberPort
            .findByThreadIdAndMemberId(threadId, memberId)
            .orElseThrow(() -> new CommunityDomainException(
                CommunityErrorCode.THREAD_MEMBER_NOT_FOUND
            ));
        if (!member.isActive()) {
            throw new CommunityDomainException(CommunityErrorCode.THREAD_ACCESS_DENIED);
        }
        return member;
    }

    private void refreshLastMessageProjection(
        CommunityThread thread,
        ChatMessageMutationResult result
    ) {
        ChatMessageInfo message = result.message();
        if (result.deduplicated() || !Objects.equals(thread.getLastMessageId(), message.messageId())) {
            return;
        }
        thread.updateLastMessage(
            message.messageId(),
            message.content(),
            message.senderMemberId(),
            message.createdAt()
        );
        saveThreadPort.save(thread);
    }

    private ChangeChatMessageReactionCommand toChatCommand(
        CommunityThread thread,
        ChangeCommunityThreadMessageReactionCommand command
    ) {
        return new ChangeChatMessageReactionCommand(
            thread.getChatRoomId(),
            command.messageId(),
            command.memberId(),
            command.emoji()
        );
    }

    private CommunityThreadReactionMutationInfo toCommunityResult(
        ChatReactionMutationResult result
    ) {
        List<CommunityThreadReactionInfo> reactions = result.reactions().stream()
            .map(this::toCommunityReaction)
            .toList();
        return new CommunityThreadReactionMutationInfo(
            result.messageId(),
            reactions,
            result.deduplicated()
        );
    }

    private CommunityThreadReactionInfo toCommunityReaction(ChatReactionInfo reaction) {
        return new CommunityThreadReactionInfo(
            reaction.emoji(),
            reaction.count(),
            reaction.reactedByMe()
        );
    }

    private MessageContentType toChatType(CommunityThreadMessageType type) {
        return switch (type) {
            case TEXT -> MessageContentType.TEXT;
            case IMAGE -> MessageContentType.IMAGE;
            case SYSTEM -> MessageContentType.SYSTEM;
        };
    }
}
