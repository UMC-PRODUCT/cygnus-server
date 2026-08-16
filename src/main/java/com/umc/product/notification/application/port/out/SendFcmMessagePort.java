package com.umc.product.notification.application.port.out;

import com.umc.product.notification.application.port.out.dto.FcmSendRequest;
import com.umc.product.notification.application.port.out.dto.FcmSendResult;

public interface SendFcmMessagePort {

    FcmSendResult send(FcmSendRequest request);
}
