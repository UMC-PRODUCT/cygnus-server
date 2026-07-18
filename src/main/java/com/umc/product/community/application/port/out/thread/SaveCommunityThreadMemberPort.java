package com.umc.product.community.application.port.out.thread;

import java.util.List;

import com.umc.product.community.domain.CommunityThreadMember;

public interface SaveCommunityThreadMemberPort {

    CommunityThreadMember save(CommunityThreadMember member);

    List<CommunityThreadMember> saveAll(List<CommunityThreadMember> members);

    void transferOwnership(
        Long threadId,
        Long previousOwnerMemberId,
        Long newOwnerMemberId
    );
}
