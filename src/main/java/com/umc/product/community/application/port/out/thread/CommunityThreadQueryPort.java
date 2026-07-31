package com.umc.product.community.application.port.out.thread;

import java.util.List;
import java.util.Optional;

import com.umc.product.community.application.port.out.thread.dto.CommunityThreadListCondition;
import com.umc.product.community.application.port.out.thread.dto.CommunityThreadListRows;
import com.umc.product.community.application.port.out.thread.dto.CommunityThreadMemberRow;
import com.umc.product.community.application.port.out.thread.dto.CommunityThreadQueryRow;

public interface CommunityThreadQueryPort {

    /**
     * @deprecated 전체 공개 통합 리스트로 전환되어 {@link #browseThreads}로 대체된다.
     *     초대받은 멤버의 스레드만 반환한다.
     */
    @Deprecated
    CommunityThreadListRows searchThreads(CommunityThreadListCondition condition);

    CommunityThreadListRows browseThreads(CommunityThreadListCondition condition);

    Optional<CommunityThreadQueryRow> findThread(Long threadId, Long requesterMemberId);

    List<CommunityThreadMemberRow> listActiveThreadMembers(Long threadId);

    List<Long> listActiveMemberIdsByThreadId(Long threadId, int limit);

    List<Long> listInvitationBlockedMemberIds(Long threadId);
}
