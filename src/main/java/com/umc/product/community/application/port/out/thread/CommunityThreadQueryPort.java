package com.umc.product.community.application.port.out.thread;

import java.util.List;
import java.util.Optional;

import com.umc.product.community.application.port.out.thread.dto.CommunityThreadListCondition;
import com.umc.product.community.application.port.out.thread.dto.CommunityThreadListRows;
import com.umc.product.community.application.port.out.thread.dto.CommunityThreadMemberRow;
import com.umc.product.community.application.port.out.thread.dto.CommunityThreadQueryRow;

public interface CommunityThreadQueryPort {

    /**
     * @deprecated 스레드 목록은 {@link #browseThreads}를 사용한다. requester가 ACTIVE 멤버인 스레드만 반환하는 이 조회는 "내 참여 스레드만 모아보기" 재사용을 위해
     * 보류한 상태다.
     */
    @Deprecated
    CommunityThreadListRows searchThreads(CommunityThreadListCondition condition);

    CommunityThreadListRows browseThreads(CommunityThreadListCondition condition);

    Optional<CommunityThreadQueryRow> findThread(Long threadId, Long requesterMemberId);

    List<CommunityThreadMemberRow> listActiveThreadMembers(Long threadId);

    List<Long> listActiveMemberIdsByThreadId(Long threadId, int limit);

    List<Long> listInvitationBlockedMemberIds(Long threadId);
}
