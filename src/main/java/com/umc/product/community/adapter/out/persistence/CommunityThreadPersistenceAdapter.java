package com.umc.product.community.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.umc.product.community.application.port.out.thread.CommunityThreadQueryPort;
import com.umc.product.community.application.port.out.thread.LoadCommunityThreadMemberPort;
import com.umc.product.community.application.port.out.thread.LoadCommunityThreadPort;
import com.umc.product.community.application.port.out.thread.SaveCommunityThreadMemberPort;
import com.umc.product.community.application.port.out.thread.SaveCommunityThreadPort;
import com.umc.product.community.application.port.out.thread.dto.CommunityThreadListCondition;
import com.umc.product.community.application.port.out.thread.dto.CommunityThreadListRows;
import com.umc.product.community.application.port.out.thread.dto.CommunityThreadMemberRow;
import com.umc.product.community.application.port.out.thread.dto.CommunityThreadQueryRow;
import com.umc.product.community.domain.CommunityThread;
import com.umc.product.community.domain.CommunityThreadMember;
import com.umc.product.community.domain.enums.CommunityThreadMemberState;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CommunityThreadPersistenceAdapter implements
    LoadCommunityThreadPort,
    SaveCommunityThreadPort,
    LoadCommunityThreadMemberPort,
    SaveCommunityThreadMemberPort,
    CommunityThreadQueryPort {

    private final CommunityThreadRepository threadRepository;
    private final CommunityThreadMemberRepository memberRepository;
    private final CommunityThreadQueryRepository queryRepository;

    @Deprecated
    @Override
    public CommunityThreadListRows searchThreads(CommunityThreadListCondition condition) {
        return queryRepository.searchThreads(condition);
    }

    @Override
    public CommunityThreadListRows browseThreads(CommunityThreadListCondition condition) {
        return queryRepository.browseThreads(condition);
    }

    @Override
    public Optional<CommunityThreadQueryRow> findThread(Long threadId, Long requesterMemberId) {
        return queryRepository.findThread(threadId, requesterMemberId);
    }

    @Override
    public List<CommunityThreadMemberRow> listActiveThreadMembers(Long threadId) {
        return queryRepository.listActiveThreadMembers(threadId);
    }

    @Override
    public Optional<CommunityThread> findById(Long threadId) {
        return threadRepository.findById(threadId);
    }

    @Override
    public Optional<CommunityThread> findByIdForUpdate(Long threadId) {
        return threadRepository.findByIdForUpdate(threadId);
    }

    @Override
    public Optional<CommunityThread> findByChatRoomId(Long chatRoomId) {
        return threadRepository.findByChatRoomId(chatRoomId);
    }

    @Override
    public CommunityThread save(CommunityThread thread) {
        return threadRepository.save(thread);
    }

    @Override
    public Optional<CommunityThreadMember> findByThreadIdAndMemberId(Long threadId, Long memberId) {
        return memberRepository.findByThreadIdAndMemberId(threadId, memberId);
    }

    @Override
    public Optional<CommunityThreadMember> findByThreadIdAndMemberIdForUpdate(
        Long threadId,
        Long memberId
    ) {
        return memberRepository.findByThreadIdAndMemberIdForUpdate(threadId, memberId);
    }

    @Override
    public List<CommunityThreadMember> listByThreadIdAndMemberIds(
        Long threadId,
        Set<Long> memberIds
    ) {
        return memberRepository.findAllByThreadIdAndMemberIdIn(threadId, memberIds);
    }

    @Override
    public List<CommunityThreadMember> listByThreadId(Long threadId) {
        return memberRepository.findAllByThreadIdOrderByIdAsc(threadId);
    }

    @Override
    public long countActiveByThreadId(Long threadId) {
        return memberRepository.countByThreadIdAndState(threadId, CommunityThreadMemberState.ACTIVE);
    }

    @Override
    public CommunityThreadMember save(CommunityThreadMember member) {
        return memberRepository.save(member);
    }

    @Override
    public List<CommunityThreadMember> saveAll(List<CommunityThreadMember> members) {
        return memberRepository.saveAll(members);
    }

    @Override
    public void transferOwnership(
        Long threadId,
        Long previousOwnerMemberId,
        Long newOwnerMemberId
    ) {
        int demotedRows = memberRepository.demoteOwner(
            threadId,
            previousOwnerMemberId
        );
        if (demotedRows != 1) {
            throw new IllegalStateException("ownership transfer must demote exactly one owner");
        }
        int promotedRows = memberRepository.promoteOwner(threadId, newOwnerMemberId);
        if (promotedRows != 1) {
            throw new IllegalStateException("ownership transfer must promote exactly one owner");
        }
    }

    @Override
    public List<Long> listActiveMemberIdsByThreadId(Long threadId, int limit) {
        return queryRepository.listActiveMemberIdsByThreadId(threadId, limit);
    }

    @Override
    public List<Long> listInvitationBlockedMemberIds(Long threadId) {
        return queryRepository.listInvitationBlockedMemberIds(threadId);
    }
}
