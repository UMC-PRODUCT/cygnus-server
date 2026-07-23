package com.umc.product.community.application.port.out.thread;

import java.util.Optional;

import com.umc.product.community.domain.CommunityThread;

public interface LoadCommunityThreadPort {

    Optional<CommunityThread> findById(Long threadId);

    Optional<CommunityThread> findByIdForUpdate(Long threadId);

    Optional<CommunityThread> findByChatRoomId(Long chatRoomId);
}
