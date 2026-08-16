package com.umc.product.authentication.application.port.out;

import com.umc.product.authentication.domain.SsoAuthorizationCode;

public interface SaveSsoAuthorizationCodePort {

    SsoAuthorizationCode save(SsoAuthorizationCode authorizationCode);
}
