package com.umc.product.chat.domain;

/**
 * Chat Port In 호출자가 서버 인증 정보로 만든 actor/target context다.
 *
 * <p>actor는 현재 authenticated member이며, target은 membership 또는 읽음 상태처럼 다른 member를
 * 명시해야 하는 operation에서만 사용한다. 클라이언트 DTO가 이 값을 직접 구성해서는 안 된다.</p>
 */
public record ChatRoomActorContext(
    Long actorMemberId,
    Long targetMemberId
) {

    public static ChatRoomActorContext actor(Long actorMemberId) {
        return new ChatRoomActorContext(actorMemberId, null);
    }

    public static ChatRoomActorContext actorAndTarget(Long actorMemberId, Long targetMemberId) {
        return new ChatRoomActorContext(actorMemberId, targetMemberId);
    }

    public boolean hasAuthenticatedActor() {
        return actorMemberId != null && actorMemberId > 0;
    }

    public boolean isSelfTarget() {
        return hasAuthenticatedActor() && actorMemberId.equals(targetMemberId);
    }
}
