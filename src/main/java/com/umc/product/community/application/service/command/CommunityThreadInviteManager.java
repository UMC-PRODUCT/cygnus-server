package com.umc.product.community.application.service.command;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.umc.product.chat.application.port.in.command.JoinChatRoomUseCase;
import com.umc.product.chat.application.port.in.command.dto.JoinChatRoomCommand;
import com.umc.product.community.application.port.out.thread.LoadCommunityThreadMemberPort;
import com.umc.product.community.application.port.out.thread.SaveCommunityThreadMemberPort;
import com.umc.product.community.domain.CommunityThread;
import com.umc.product.community.domain.CommunityThreadMember;
import com.umc.product.community.domain.CommunityThreadProperties;
import com.umc.product.community.domain.enums.CommunityThreadMemberState;
import com.umc.product.community.domain.exception.CommunityDomainException;
import com.umc.product.community.domain.exception.CommunityErrorCode;
import com.umc.product.member.application.port.in.query.SearchActiveChallengerInvitationUseCase;
import com.umc.product.member.application.port.in.query.dto.ActiveChallengerInvitationInfo;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CommunityThreadInviteManager {

    private final SearchActiveChallengerInvitationUseCase searchInvitationUseCase;
    private final LoadCommunityThreadMemberPort loadMemberPort;
    private final SaveCommunityThreadMemberPort saveMemberPort;
    private final JoinChatRoomUseCase joinChatRoomUseCase;
    private final CommunityThreadProperties properties;

    public List<CommunityThreadMember> invite(
        CommunityThread thread,
        List<Long> memberIds,
        Instant joinedAt
    ) {
        List<Long> sortedMemberIds = memberIds.stream().sorted().toList();
        Map<Long, CommunityThreadMember> existingMembers = loadExistingMembers(
            thread.getId(),
            sortedMemberIds
        );
        long activeMemberCount = loadMemberPort.countActiveByThreadId(thread.getId());
        if (activeMemberCount > Integer.MAX_VALUE
            || !properties.allowsInvite((int) activeMemberCount, sortedMemberIds.size())) {
            throw new CommunityDomainException(CommunityErrorCode.THREAD_CAPACITY_EXCEEDED);
        }

        Set<Long> selectedMemberIds = Set.copyOf(sortedMemberIds);
        Map<Long, ActiveChallengerInvitationInfo> eligibleMembers = searchInvitationUseCase
            .batchGetEligibleActiveChallengers(thread.getActiveGisuId(), selectedMemberIds);
        if (!eligibleMembers.keySet().containsAll(selectedMemberIds)) {
            throw new CommunityDomainException(CommunityErrorCode.THREAD_INVITEE_NOT_ELIGIBLE);
        }

        Long initialLastReadMessageId = thread.getLastMessageId();
        List<CommunityThreadMember> invitedMembers = sortedMemberIds.stream()
            .map(memberId -> {
                joinChatRoomUseCase.joinChatRoom(new JoinChatRoomCommand(
                    thread.getChatRoomId(),
                    memberId,
                    initialLastReadMessageId
                ));
                CommunityThreadMember existingMember = existingMembers.get(memberId);
                if (existingMember != null) {
                    existingMember.rejoin(joinedAt);
                    return existingMember;
                }
                return CommunityThreadMember.createMember(thread.getId(), memberId, joinedAt);
            })
            .toList();
        saveMemberPort.saveAll(invitedMembers);
        return invitedMembers;
    }

    private Map<Long, CommunityThreadMember> loadExistingMembers(
        Long threadId,
        List<Long> memberIds
    ) {
        Map<Long, CommunityThreadMember> existingMembers = new HashMap<>();
        List<CommunityThreadMember> selectedMembers = loadMemberPort.listByThreadIdAndMemberIds(
            threadId,
            Set.copyOf(memberIds)
        );
        for (CommunityThreadMember member : selectedMembers) {
            if (member.getState() == CommunityThreadMemberState.ACTIVE) {
                throw new CommunityDomainException(CommunityErrorCode.THREAD_MEMBER_ALREADY_ACTIVE);
            }
            if (member.getState() == CommunityThreadMemberState.KICKED) {
                throw new CommunityDomainException(CommunityErrorCode.THREAD_MEMBER_KICKED);
            }
            existingMembers.put(member.getMemberId(), member);
        }
        return existingMembers;
    }
}
