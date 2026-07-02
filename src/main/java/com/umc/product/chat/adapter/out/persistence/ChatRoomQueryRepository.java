package com.umc.product.chat.adapter.out.persistence;

import static com.umc.product.chat.domain.QChatMember.chatMember;
import static com.umc.product.chat.domain.QChatRoom.chatRoom;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.product.chat.domain.ChatRoom;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ChatRoomQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<ChatRoom> findAllByMemberId(Long memberId) {
        return queryFactory.selectFrom(chatRoom)
            .join(chatMember).on(chatMember.roomId.eq(chatRoom.id))
            .where(chatMember.memberId.eq(memberId))
            .fetch();
    }
}
