package com.umc.product.community.application.port.out.thread;

import java.util.List;
import java.util.Optional;

import com.umc.product.community.application.port.out.thread.dto.CommunityThreadListCondition;
import com.umc.product.community.application.port.out.thread.dto.CommunityThreadListRows;
import com.umc.product.community.application.port.out.thread.dto.CommunityThreadMemberRow;
import com.umc.product.community.application.port.out.thread.dto.CommunityThreadQueryRow;

public interface CommunityThreadQueryPort {

    CommunityThreadListRows searchThreads(CommunityThreadListCondition condition);

    Optional<CommunityThreadQueryRow> findThread(Long threadId, Long requesterMemberId);

    List<CommunityThreadMemberRow> listActiveThreadMembers(Long threadId);

    List<Long> listActiveMemberIdsByThreadId(Long threadId, int limit);

    List<Long> listInvitationBlockedMemberIds(Long threadId);
}
