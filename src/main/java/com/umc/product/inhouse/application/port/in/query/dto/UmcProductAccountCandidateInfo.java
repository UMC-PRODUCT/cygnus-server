package com.umc.product.inhouse.application.port.in.query.dto;

public record UmcProductAccountCandidateInfo(
    Long memberId,
    String name,
    String nickname,
    String email,
    String profileImageLink,
    boolean alreadyLinked
) {
}
