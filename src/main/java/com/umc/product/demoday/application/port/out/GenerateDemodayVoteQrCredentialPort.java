package com.umc.product.demoday.application.port.out;

import java.time.Instant;

public interface GenerateDemodayVoteQrCredentialPort {

    /**
     * 주어진 시간 구간(issuedAt~expiresAt)의 INFO QR credential을 결정적으로 생성한다.
     *
     * <p>같은 pollId와 같은 구간이 주어지면 항상 같은 문자열을 반환해야 한다. 호출할 때마다 새 값을
     * 만드는 명령이 아니라, 그 구간에 대응하는 값을 계산해 돌려주는 함수다.
     */
    String generate(Long pollId, Instant issuedAt, Instant expiresAt);
}
