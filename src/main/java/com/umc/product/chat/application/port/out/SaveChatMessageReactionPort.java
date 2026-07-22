package com.umc.product.chat.application.port.out;

public interface SaveChatMessageReactionPort {

    boolean addIfAbsent(Long messageId, Long memberId, String emoji);

    boolean remove(Long messageId, Long memberId, String emoji);

    void deleteByMessageId(Long messageId);
}
