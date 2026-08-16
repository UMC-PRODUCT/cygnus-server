package com.umc.product.authentication.application.port.out;

import com.umc.product.authentication.domain.SsoClient;

public interface LoadSsoClientPort {
    SsoClient getByClientId(String clientId);
}
