package com.umc.product.notification.application.port.out;

import java.util.List;

import com.umc.product.notification.domain.FcmToken;

public interface SaveFcmPort {
    void save(FcmToken newToken);

    default void saveAll(List<FcmToken> fcmTokens) {
        fcmTokens.forEach(this::save);
    }
}
