package com.umc.product.chat.application.port.out;

import java.util.List;
import java.util.Map;

public interface LoadChatMessageMentionPort {

    List<Long> listMemberIdsByMessageId(Long messageId);

    Map<Long, List<Long>> listMemberIdsByMessageIds(List<Long> messageIds);
}
