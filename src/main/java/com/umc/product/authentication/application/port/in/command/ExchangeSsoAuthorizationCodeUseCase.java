package com.umc.product.authentication.application.port.in.command;

import com.umc.product.authentication.application.port.in.command.dto.ExchangeSsoAuthorizationCodeCommand;
import com.umc.product.authentication.application.port.in.command.dto.SsoTokenInfo;

public interface ExchangeSsoAuthorizationCodeUseCase {

    SsoTokenInfo exchange(ExchangeSsoAuthorizationCodeCommand command);
}
