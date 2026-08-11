package com.umc.product.chat.domain;

/**
 * 채팅방 메시지 조회의 공개 범위.
 * <p>
 * 방을 만든 소비 도메인이 생성 시점에 정하며, Chat engine은 소비 도메인을 알지 않고 이 값만 본다.
 * 조회에만 적용되고 메시지 생성/수정/삭제, 리액션, 읽음 갱신 같은 모든 변경은 범위와 무관하게
 * 방 멤버만 수행할 수 있다.
 * <ul>
 *     <li>{@code MEMBER_ONLY} - 방 멤버만 메시지를 조회할 수 있다. 기본값이며 fail-closed 기준선이다.</li>
 *     <li>{@code PUBLIC} - 소비 도메인이 resource 접근을 허용한 사용자라면 비멤버도 조회할 수 있다.
 *     resource 단위 공개 여부는 소비 도메인이 판단하므로, 이 값을 쓰는 소비 도메인은 자신의 조회
 *     권한 검증을 반드시 유지해야 한다.</li>
 * </ul>
 */
public enum ChatRoomReadScope {
    MEMBER_ONLY,
    PUBLIC
}
