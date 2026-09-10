package com.umc.product.common.domain.enums;

import com.umc.product.challenger.domain.exception.ChallengerDomainException;
import com.umc.product.challenger.domain.exception.ChallengerErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChallengerTrack {
    PLAN("기획", 0),
    DESIGN("디자인", 1),
    WEB_PRODUCT_ENGINEER("웹 프로덕트 엔지니어", 2),
    MOBILE_PRODUCT_ENGINEER("모바일 프로덕트 엔지니어", 3),
    INFRA_PLUS("인프라 플러스", 4),
    ;

    private final String displayName;
    private final int sortOrder;

    public boolean isBasic() {
        return this != INFRA_PLUS;
    }

    public static ChallengerTrack from(ChallengerPart part) {
        if (part == null || part == ChallengerPart.ADMIN) {
            throw new ChallengerDomainException(ChallengerErrorCode.CHALLENGER_PART_NOT_FOUND);
        }

        return switch (part) {
            case PLAN -> PLAN;
            case DESIGN -> DESIGN;
            case WEB, NODEJS, SPRINGBOOT -> WEB_PRODUCT_ENGINEER;
            case ANDROID, IOS -> MOBILE_PRODUCT_ENGINEER;
            case ADMIN -> throw new ChallengerDomainException(ChallengerErrorCode.CHALLENGER_PART_NOT_FOUND);
        };
    }
}
