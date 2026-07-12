package com.umc.product.chat.application.port.out;

import com.umc.product.chat.domain.ChatMember;

public interface SaveChatMemberPort {

    ChatMember save(ChatMember chatMember);

    void delete(Long roomId, Long memberId);

    /**
     * 멤버의 읽음 위치를 candidate 로 단조 증가시킨다(원자적).
     * <p>
     * {@code last_read_message_id = GREATEST(COALESCE(last_read_message_id, 0), candidate)} 를 DB 에서
     * 한 번에 수행하므로, 여러 트랜잭션이 동시에 갱신해도 뒤로 되돌아가지 않는다(엔티티 로드-비교-저장의 lost update 방지).
     */
    void bumpLastReadMessageId(Long roomId, Long memberId, long candidateMessageId);
}
