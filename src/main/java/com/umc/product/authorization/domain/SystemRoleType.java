package com.umc.product.authorization.domain;

import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.authorization.domain.exception.AuthorizationErrorCode;

/**
 * 회원 단위로 부여되는 시스템 권한입니다.
 * <p>
 * 저장 위치는 member 도메인이 될 수 있지만, 권한 정책의 의미는 authorization 도메인에서 해석합니다.
 */
public enum SystemRoleType {
    SUPER_ADMIN;

    public static SystemRoleType from(String roleType) {
        try {
            return SystemRoleType.valueOf(roleType);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new AuthorizationDomainException(
                AuthorizationErrorCode.INVALID_INPUT_VALUE,
                "지원하지 않는 시스템 권한입니다: " + roleType
            );
        }
    }
}
