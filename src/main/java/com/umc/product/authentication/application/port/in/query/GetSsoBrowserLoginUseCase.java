package com.umc.product.authentication.application.port.in.query;

import com.umc.product.authentication.application.port.in.dto.SsoBrowserLoginInfo;

public interface GetSsoBrowserLoginUseCase {

    SsoBrowserLoginInfo getLogin(String rawLoginToken);
}
