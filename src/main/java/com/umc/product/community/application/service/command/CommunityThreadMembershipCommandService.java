package com.umc.product.community.application.service.command;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.chat.application.port.in.command.LeaveChatRoomUseCase;
import com.umc.product.chat.application.port.in.command.dto.LeaveChatRoomCommand;
import com.umc.product.community.application.port.in.command.thread.ChangeCommunityThreadMemberRoleUseCase;
import com.umc.product.community.application.port.in.command.thread.InviteCommunityThreadMembersUseCase;
import com.umc.product.community.application.port.in.command.thread.KickCommunityThreadMemberUseCase;
import com.umc.product.community.application.port.in.command.thread.LeaveCommunityThreadUseCase;
import com.umc.product.community.application.port.in.command.thread.dto.ChangeCommunityThreadMemberRoleCommand;
import com.umc.product.community.application.port.in.command.thread.dto.CommunityThreadInvitationInfo;
import com.umc.product.community.application.port.in.command.thread.dto.CommunityThreadMemberLifecycleInfo;
import com.umc.product.community.application.port.in.command.thread.dto.InviteCommunityThreadMembersCommand;
import com.umc.product.community.application.port.in.command.thread.dto.KickCommunityThreadMemberCommand;
import com.umc.product.community.application.port.in.command.thread.dto.ThreadActorCommand;
import com.umc.product.community.application.port.out.thread.LoadCommunityThreadMemberPort;
import com.umc.product.community.application.port.out.thread.LoadCommunityThreadPort;
import com.umc.product.community.application.port.out.thread.SaveCommunityThreadMemberPort;
import com.umc.product.community.domain.CommunityThread;
import com.umc.product.community.domain.CommunityThreadMember;
import com.umc.product.community.domain.enums.CommunityThreadMemberRole;
import com.umc.product.community.domain.event.CommunityThreadInvitedEvent;
import com.umc.product.community.domain.event.CommunityThreadMemberKickedEvent;
import com.umc.product.community.domain.event.CommunityThreadMemberLeftEvent;
import com.umc.product.community.domain.exception.CommunityDomainException;
import com.umc.product.community.domain.exception.CommunityErrorCode;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CommunityThreadMembershipCommandService implements
    InviteCommunityThreadMembersUseCase,
    KickCommunityThreadMemberUseCase,
    LeaveCommunityThreadUseCase,
    ChangeCommunityThreadMemberRoleUseCase {

    private final LoadCommunityThreadPort loadThreadPort;
    private final LoadCommunityThreadMemberPort loadMemberPort;
    private final SaveCommunityThreadMemberPort saveMemberPort;
    private final CommunityThreadInviteManager inviteManager;
    private final LeaveChatRoomUseCase leaveChatRoomUseCase;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    @Override
    public CommunityThreadInvitationInfo invite(InviteCommunityThreadMembersCommand command) {
        CommunityThread thread = loadLockedThread(command.threadId());
        CommunityThreadMember actor = loadActiveActor(thread.getId(), command.actorMemberId());
        requireManager(actor);
        Instant invitedAt = clock.instant();
        List<CommunityThreadMember> invitedMembers = inviteManager.invite(
            thread,
            command.memberIds(),
            invitedAt
        );
        long memberCount = loadMemberPort.countActiveByThreadId(thread.getId());
        eventPublisher.publish(CommunityThreadInvitedEvent.of(
            thread.getId(),
            actor.getMemberId(),
            invitedMembers.stream().map(CommunityThreadMember::getMemberId).toList(),
            invitedAt
        ));
        return new CommunityThreadInvitationInfo(
            thread.getId(),
            invitedMembers.stream()
                .map(member -> CommunityThreadMemberLifecycleInfo.from(member, memberCount))
                .toList(),
            memberCount
        );
    }

    @Override
    public CommunityThreadMemberLifecycleInfo kick(KickCommunityThreadMemberCommand command) {
        CommunityThread thread = loadLockedThread(command.threadId());
        CommunityThreadMember actor = loadActiveActor(thread.getId(), command.actorMemberId());
        requireManager(actor);
        CommunityThreadMember target = loadActiveTarget(thread.getId(), command.memberId());
        if (target.getRole() == CommunityThreadMemberRole.OWNER) {
            throw new CommunityDomainException(CommunityErrorCode.THREAD_OWNER_CANNOT_BE_KICKED);
        }
        List<Long> activeMemberIds = activeMemberIds(thread.getId());
        leaveChatRoomUseCase.leaveChatRoom(LeaveChatRoomCommand.of(
            thread.getChatRoomId(),
            target.getMemberId()
        ));
        Instant kickedAt = clock.instant();
        target.kick(kickedAt);
        saveMemberPort.save(target);
        eventPublisher.publish(CommunityThreadMemberKickedEvent.of(
            thread.getId(),
            actor.getMemberId(),
            target.getMemberId(),
            activeMemberIds,
            kickedAt
        ));
        return CommunityThreadMemberLifecycleInfo.from(target, activeMemberIds.size() - 1L);
    }

    @Override
    public CommunityThreadMemberLifecycleInfo leave(ThreadActorCommand command) {
        CommunityThread thread = loadLockedThread(command.threadId());
        CommunityThreadMember actor = loadActiveActor(thread.getId(), command.actorMemberId());
        if (actor.getRole() == CommunityThreadMemberRole.OWNER) {
            throw new CommunityDomainException(CommunityErrorCode.THREAD_OWNER_CANNOT_LEAVE);
        }
        List<Long> activeMemberIds = activeMemberIds(thread.getId());
        leaveChatRoomUseCase.leaveChatRoom(LeaveChatRoomCommand.of(
            thread.getChatRoomId(),
            actor.getMemberId()
        ));
        Instant leftAt = clock.instant();
        actor.leave(leftAt);
        saveMemberPort.save(actor);
        eventPublisher.publish(CommunityThreadMemberLeftEvent.of(
            thread.getId(),
            actor.getMemberId(),
            activeMemberIds,
            leftAt
        ));
        return CommunityThreadMemberLifecycleInfo.from(actor, activeMemberIds.size() - 1L);
    }

    @Override
    public CommunityThreadMemberLifecycleInfo changeRole(
        ChangeCommunityThreadMemberRoleCommand command
    ) {
        CommunityThread thread = loadLockedThread(command.threadId());
        CommunityThreadMember actor = loadActiveActor(thread.getId(), command.actorMemberId());
        if (actor.getRole() != CommunityThreadMemberRole.OWNER) {
            throw new CommunityDomainException(CommunityErrorCode.THREAD_OWNER_REQUIRED);
        }
        CommunityThreadMember target = loadActiveTarget(thread.getId(), command.memberId());
        validateRoleChange(actor, target, command.role());
        long memberCount = loadMemberPort.countActiveByThreadId(thread.getId());
        if (command.role() == CommunityThreadMemberRole.OWNER) {
            saveMemberPort.transferOwnership(
                thread.getId(),
                actor.getMemberId(),
                target.getMemberId()
            );
            actor.changeRole(CommunityThreadMemberRole.ADMIN);
            target.changeRole(CommunityThreadMemberRole.OWNER);
        } else {
            target.changeRole(command.role());
            saveMemberPort.save(target);
        }
        return CommunityThreadMemberLifecycleInfo.from(target, memberCount);
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

    private CommunityThreadMember loadActiveTarget(Long threadId, Long memberId) {
        return loadMemberPort.findByThreadIdAndMemberId(threadId, memberId)
            .filter(CommunityThreadMember::isActive)
            .orElseThrow(() -> new CommunityDomainException(CommunityErrorCode.THREAD_MEMBER_NOT_FOUND));
    }

    private void requireManager(CommunityThreadMember actor) {
        if (actor.getRole() != CommunityThreadMemberRole.OWNER
            && actor.getRole() != CommunityThreadMemberRole.ADMIN) {
            throw new CommunityDomainException(CommunityErrorCode.THREAD_ACCESS_DENIED);
        }
    }

    private void validateRoleChange(
        CommunityThreadMember actor,
        CommunityThreadMember target,
        CommunityThreadMemberRole requestedRole
    ) {
        if (actor.getMemberId().equals(target.getMemberId())
            || target.getRole() == CommunityThreadMemberRole.OWNER
            || target.getRole() == requestedRole) {
            throw new CommunityDomainException(CommunityErrorCode.THREAD_INVALID_ROLE_CHANGE);
        }
    }

    private List<Long> activeMemberIds(Long threadId) {
        return loadMemberPort.listByThreadId(threadId).stream()
            .filter(CommunityThreadMember::isActive)
            .map(CommunityThreadMember::getMemberId)
            .toList();
    }
}
