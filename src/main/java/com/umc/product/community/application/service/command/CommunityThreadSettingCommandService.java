package com.umc.product.community.application.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.community.application.port.in.command.thread.ManageCommunityThreadMuteUseCase;
import com.umc.product.community.application.port.in.command.thread.ManageCommunityThreadPinUseCase;
import com.umc.product.community.application.port.in.command.thread.dto.CommunityThreadLifecycleInfo;
import com.umc.product.community.application.port.in.command.thread.dto.ThreadActorCommand;
import com.umc.product.community.application.port.out.thread.LoadCommunityThreadMemberPort;
import com.umc.product.community.application.port.out.thread.LoadCommunityThreadPort;
import com.umc.product.community.application.port.out.thread.SaveCommunityThreadMemberPort;
import com.umc.product.community.domain.CommunityThread;
import com.umc.product.community.domain.CommunityThreadMember;
import com.umc.product.community.domain.CommunityThreadProperties;
import com.umc.product.community.domain.exception.CommunityDomainException;
import com.umc.product.community.domain.exception.CommunityErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CommunityThreadSettingCommandService implements
    ManageCommunityThreadPinUseCase,
    ManageCommunityThreadMuteUseCase {

    private final LoadCommunityThreadPort loadThreadPort;
    private final LoadCommunityThreadMemberPort loadMemberPort;
    private final SaveCommunityThreadMemberPort saveMemberPort;
    private final CommunityThreadProperties properties;

    @Override
    public CommunityThreadLifecycleInfo pin(ThreadActorCommand command) {
        CommunityThread thread = loadLockedThread(command.threadId());
        CommunityThreadMember actor = loadActiveActor(thread.getId(), command.actorMemberId());
        if (actor.pin()) {
            saveMemberPort.save(actor);
        }
        return info(thread, actor);
    }

    @Override
    public CommunityThreadLifecycleInfo unpin(ThreadActorCommand command) {
        CommunityThread thread = loadLockedThread(command.threadId());
        CommunityThreadMember actor = loadActiveActor(thread.getId(), command.actorMemberId());
        if (actor.unpin()) {
            saveMemberPort.save(actor);
        }
        return info(thread, actor);
    }

    @Override
    public CommunityThreadLifecycleInfo mute(ThreadActorCommand command) {
        CommunityThread thread = loadLockedThread(command.threadId());
        CommunityThreadMember actor = loadActiveActor(thread.getId(), command.actorMemberId());
        if (actor.mute()) {
            saveMemberPort.save(actor);
        }
        return info(thread, actor);
    }

    @Override
    public CommunityThreadLifecycleInfo unmute(ThreadActorCommand command) {
        CommunityThread thread = loadLockedThread(command.threadId());
        CommunityThreadMember actor = loadActiveActor(thread.getId(), command.actorMemberId());
        if (actor.unmute()) {
            saveMemberPort.save(actor);
        }
        return info(thread, actor);
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

    private CommunityThreadLifecycleInfo info(
        CommunityThread thread,
        CommunityThreadMember actor
    ) {
        long memberCount = loadMemberPort.countActiveByThreadId(thread.getId());
        return CommunityThreadLifecycleInfo.from(
            thread,
            actor,
            memberCount,
            properties.maxMembers()
        );
    }
}
