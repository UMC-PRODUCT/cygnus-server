package com.umc.product.notification.application.port.in;

import com.umc.product.notification.application.port.in.dto.FcmNotificationRequestInfo;
import com.umc.product.notification.application.port.in.dto.RequestFcmNotificationCommand;

public interface RequestFcmNotificationUseCase {

    FcmNotificationRequestInfo request(RequestFcmNotificationCommand command);
}
