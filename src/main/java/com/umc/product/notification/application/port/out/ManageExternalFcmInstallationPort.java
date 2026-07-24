package com.umc.product.notification.application.port.out;

import com.umc.product.notification.application.port.in.dto.RegisterFcmTokenCommand;
import com.umc.product.notification.application.port.in.dto.UnregisterFcmTokenCommand;

public interface ManageExternalFcmInstallationPort {

    void register(RegisterFcmTokenCommand command);

    void unregister(UnregisterFcmTokenCommand command);
}
