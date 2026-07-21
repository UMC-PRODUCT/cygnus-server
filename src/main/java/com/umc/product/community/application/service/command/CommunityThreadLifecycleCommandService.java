package com.umc.product.community.application.service.command;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.chat.application.port.in.command.CreateChatRoomUseCase;
import com.umc.product.chat.application.port.in.command.dto.CreateChatRoomCommand;
import com.umc.product.chat.application.port.in.query.dto.ChatRoomInfo;
import com.umc.product.community.application.port.in.command.thread.CreateCommunityThreadUseCase;
import com.umc.product.community.application.port.in.command.thread.DeleteCommunityThreadUseCase;
import com.umc.product.community.application.port.in.command.thread.UpdateCommunityThreadUseCase;
import com.umc.product.community.application.port.in.command.thread.dto.CommunityThreadLifecycleInfo;
import com.umc.product.community.application.port.in.command.thread.dto.CreateCommunityThreadCommand;
import com.umc.product.community.application.port.in.command.thread.dto.ThreadActorCommand;
import com.umc.product.community.application.port.in.command.thread.dto.UpdateCommunityThreadCommand;
import com.umc.product.community.application.port.out.thread.LoadCommunityThreadMemberPort;
import com.umc.product.community.application.port.out.thread.LoadCommunityThreadPort;
import com.umc.product.community.application.port.out.thread.SaveCommunityThreadMemberPort;
import com.umc.product.community.application.port.out.thread.SaveCommunityThreadPort;
import com.umc.product.community.domain.CommunityThread;
import com.umc.product.community.domain.CommunityThreadMember;
import com.umc.product.community.domain.CommunityThreadProperties;
import com.umc.product.community.domain.enums.CommunityThreadMemberRole;
import com.umc.product.community.domain.event.CommunityThreadDeletedEvent;
import com.umc.product.community.domain.event.CommunityThreadInvitedEvent;
import com.umc.product.community.domain.event.CommunityThreadUpdatedEvent;
import com.umc.product.community.domain.exception.CommunityDomainException;
import com.umc.product.community.domain.exception.CommunityErrorCode;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CommunityThreadLifecycleCommandService implements
    CreateCommunityThreadUseCase,
    UpdateCommunityThreadUseCase,
    DeleteCommunityThreadUseCase {

    private final LoadCommunityThreadPort loadThreadPort;
    private final SaveCommunityThreadPort saveThreadPort;
    private final LoadCommunityThreadMemberPort loadMemberPort;
    private final SaveCommunityThreadMemberPort saveMemberPort;
    private final CreateChatRoomUseCase createChatRoomUseCase;
    private final CommunityThreadInviteManager inviteManager;
    private final DomainEventPublisher eventPublisher;
    private final CommunityThreadProperties properties;
    private final Clock clock;

    @Override
    public CommunityThreadLifecycleInfo create(CreateCommunityThreadCommand command) {
        if (!properties.allowsCreateInviteCount(command.inviteeMemberIds().size())) {
            throw new CommunityDomainException(CommunityErrorCode.THREAD_CAPACITY_EXCEEDED);
        }

        ChatRoomInfo chatRoom = createChatRoomUseCase.create(
            CreateChatRoomCommand.from(command.actorMemberId())
        );
        Instant occurredAt = clock.instant();
        CommunityThread thread = saveThreadPort.save(CommunityThread.create(
            chatRoom.roomId(),
            command.title(),
            command.description(),
            command.category(),
            command.icon(),
            command.actorMemberId(),
            chatRoom.createdAt()
        ));
        CommunityThreadMember owner = saveMemberPort.save(
            CommunityThreadMember.createOwner(thread.getId(), command.actorMemberId(), occurredAt)
        );
        List<CommunityThreadMember> invitedMembers = command.inviteeMemberIds().isEmpty()
            ? List.of()
            : inviteManager.invite(thread, command.inviteeMemberIds(), occurredAt);

        if (!invitedMembers.isEmpty()) {
            eventPublisher.publish(CommunityThreadInvitedEvent.of(
                thread.getId(),
                command.actorMemberId(),
                command.inviteeMemberIds(),
                occurredAt
            ));
        }
        return CommunityThreadLifecycleInfo.from(
            thread,
            owner,
            invitedMembers.size() + 1L,
            properties.maxMembers()
        );
    }

    @Override
    public CommunityThreadLifecycleInfo update(UpdateCommunityThreadCommand command) {
        CommunityThread thread = loadLockedThread(command.threadId());
        CommunityThreadMember actor = loadActiveActor(thread.getId(), command.actorMemberId());
        requireManager(actor);
        List<Long> activeMemberIds = activeMemberIds(thread.getId());
        thread.updateMetadata(
            command.title() == null ? thread.getTitle() : command.title(),
            command.descriptionProvided() ? command.description() : thread.getDescription(),
            command.category() == null ? thread.getCategory() : command.category(),
            command.icon() == null ? thread.getIcon() : command.icon()
        );
        saveThreadPort.save(thread);
        eventPublisher.publish(CommunityThreadUpdatedEvent.of(
            thread.getId(),
            actor.getMemberId(),
            clock.instant()
        ));
        return CommunityThreadLifecycleInfo.from(
            thread,
            actor,
            activeMemberIds.size(),
            properties.maxMembers()
        );
    }

    @Override
    public CommunityThreadLifecycleInfo delete(ThreadActorCommand command) {
        CommunityThread thread = loadLockedThread(command.threadId());
        CommunityThreadMember actor = loadActiveActor(thread.getId(), command.actorMemberId());
        requireManager(actor);
        List<Long> activeMemberIds = activeMemberIds(thread.getId());
        Instant deletedAt = clock.instant();
        thread.delete(deletedAt);
        saveThreadPort.save(thread);
        eventPublisher.publish(CommunityThreadDeletedEvent.of(
            thread.getId(),
            actor.getMemberId(),
            activeMemberIds,
            deletedAt
        ));
        return CommunityThreadLifecycleInfo.from(
            thread,
            actor,
            activeMemberIds.size(),
            properties.maxMembers()
        );
    }

    private CommunityThread loadLockedThread(Long threadId) {
        CommunityThread thread = loadThreadPort.findByIdForUpdate(threadId)
            .orElseThrow(() -> new CommunityDomainException(CommunityErrorCode.THREAD_NOT_FOUND));
        if (thread.isDeleted()) {
            throw new CommunityDomainException(CommunityErrorCode.THREAD_DELETED);
        }
        return thread;
    }

    private CommunityThreadMember loadActiveActor(Long threadId, Long actorMemberId) {
        return loadMemberPort.findByThreadIdAndMemberId(threadId, actorMemberId)
            .filter(CommunityThreadMember::isActive)
            .orElseThrow(() -> new CommunityDomainException(CommunityErrorCode.THREAD_ACCESS_DENIED));
    }

    private void requireManager(CommunityThreadMember actor) {
        if (actor.getRole() != CommunityThreadMemberRole.OWNER
            && actor.getRole() != CommunityThreadMemberRole.ADMIN) {
            throw new CommunityDomainException(CommunityErrorCode.THREAD_ACCESS_DENIED);
        }
    }

    private List<Long> activeMemberIds(Long threadId) {
        return loadMemberPort.listByThreadId(threadId).stream()
            .filter(CommunityThreadMember::isActive)
            .map(CommunityThreadMember::getMemberId)
            .toList();
    }
}
