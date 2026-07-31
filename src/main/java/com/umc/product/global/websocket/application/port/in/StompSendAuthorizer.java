package com.umc.product.global.websocket.application.port.in;

/**
 * 소비 도메인이 소유한 STOMP application destination SEND 인가 계약.
 *
 * <p>각 소비 도메인은 자신이 소유한 destination만 {@link #supports(String)}에서 선택하고,
 * resource 식별자 해석과 접근 권한 검증을 수행한다.
 */
public interface StompSendAuthorizer {

    boolean supports(String destination);

    boolean isAuthorized(Long memberId, String destination);
}
