package com.umc.product.community.application.port.in.command.thread;

import com.umc.product.community.application.port.in.command.thread.dto.CommunityThreadMemberLifecycleInfo;
import com.umc.product.community.application.port.in.command.thread.dto.KickCommunityThreadMemberCommand;

public interface KickCommunityThreadMemberUseCase {

    CommunityThreadMemberLifecycleInfo kick(KickCommunityThreadMemberCommand command);
}
