package com.umc.product.authentication.application.port.in.command;

import com.umc.product.authentication.application.port.in.command.dto.AuthorizeSsoCommand;
import com.umc.product.authentication.application.port.in.command.dto.SsoAuthorizationRedirectInfo;

public interface AuthorizeSsoUseCase {

    SsoAuthorizationRedirectInfo authorize(AuthorizeSsoCommand command);
}
