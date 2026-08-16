package com.umc.product.authentication.application.port.in.command;

import com.umc.product.authentication.application.port.in.command.dto.LoginSsoBrowserByAppleAuthorizationCodeCommand;
import com.umc.product.authentication.application.port.in.command.dto.LoginSsoBrowserByEmailCommand;
import com.umc.product.authentication.application.port.in.command.dto.LoginSsoBrowserByOAuthTokenCommand;
import com.umc.product.authentication.application.port.in.dto.SsoBrowserLoginInfo;
import com.umc.product.authentication.application.port.in.dto.SsoBrowserOAuthLoginResult;

public interface ManageSsoBrowserLoginUseCase {

    SsoBrowserLoginInfo loginByEmail(LoginSsoBrowserByEmailCommand command);

    SsoBrowserOAuthLoginResult loginByOAuthToken(LoginSsoBrowserByOAuthTokenCommand command);

    SsoBrowserOAuthLoginResult loginByAppleAuthorizationCode(LoginSsoBrowserByAppleAuthorizationCodeCommand command);
}
