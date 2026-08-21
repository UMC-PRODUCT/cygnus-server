package com.umc.product.demoday.application.port.out;

public interface VerifyDemodayVoteAuthorizationPort {

    DemodayVoteAuthorizationTokenClaims verify(String token);
}
