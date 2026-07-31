package com.umc.product.notification.application.port.in;

import com.umc.product.notification.application.port.in.dto.RegisterFcmTokenCommand;
import com.umc.product.notification.application.port.in.dto.UnregisterFcmTokenCommand;

public interface ManageFcmUseCase {

    void registerFcmToken(RegisterFcmTokenCommand command);

    void unregisterFcmToken(UnregisterFcmTokenCommand command);

}
