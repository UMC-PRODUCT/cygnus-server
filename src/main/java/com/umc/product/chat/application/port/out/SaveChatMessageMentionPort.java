package com.umc.product.chat.application.port.out;

import java.util.List;

public interface SaveChatMessageMentionPort {

    void saveAll(Long messageId, List<Long> memberIds);

    void deleteByMessageId(Long messageId);
}
