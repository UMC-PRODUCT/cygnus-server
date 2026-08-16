package com.umc.product.authentication.application.port.out;

import java.util.Optional;

import com.umc.product.authentication.domain.SsoAuthorizationCode;

public interface LoadSsoAuthorizationCodePort {

    Optional<SsoAuthorizationCode> findByCodeHashForUpdate(String codeHash);
}
