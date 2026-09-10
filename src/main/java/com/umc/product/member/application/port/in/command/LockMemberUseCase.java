package com.umc.product.member.application.port.in.command;

/**
 * 호출한 트랜잭션이 끝날 때까지 회원의 변경 요청을 직렬화한다.
 */
public interface LockMemberUseCase {

    void lockById(Long memberId);
}
