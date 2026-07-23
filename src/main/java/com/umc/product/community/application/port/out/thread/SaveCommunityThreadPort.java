package com.umc.product.community.application.port.out.thread;

import com.umc.product.community.domain.CommunityThread;

public interface SaveCommunityThreadPort {

    CommunityThread save(CommunityThread thread);
}
