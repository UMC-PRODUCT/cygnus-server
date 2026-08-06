package com.umc.product.global.security;

import java.util.List;

import com.umc.product.common.domain.enums.ClientType;

public record ParsedAccessToken(Long memberId, List<String> roles, ClientType clientType) {

}
