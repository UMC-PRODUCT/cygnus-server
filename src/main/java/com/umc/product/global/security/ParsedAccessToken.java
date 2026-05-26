package com.umc.product.global.security;

import com.umc.product.common.domain.enums.ClientType;
import java.util.List;

public record ParsedAccessToken(Long memberId, List<String> roles, ClientType clientType) {

}
