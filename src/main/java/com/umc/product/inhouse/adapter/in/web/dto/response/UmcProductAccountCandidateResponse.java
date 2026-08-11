package com.umc.product.inhouse.adapter.in.web.dto.response;

import com.umc.product.inhouse.application.port.in.query.dto.UmcProductAccountCandidateInfo;

public record UmcProductAccountCandidateResponse(
    Long memberId,
    String name,
    String nickname,
    String email,
    String profileImageLink,
    boolean alreadyLinked
) {
    public static UmcProductAccountCandidateResponse from(UmcProductAccountCandidateInfo info) {
        return new UmcProductAccountCandidateResponse(
            info.memberId(),
            info.name(),
            info.nickname(),
            info.email(),
            info.profileImageLink(),
            info.alreadyLinked()
        );
    }
}
