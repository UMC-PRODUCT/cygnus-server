package com.umc.product.global.config.notification;

public enum NotificationTransport {
    LOCAL,
    SHADOW,
    EXTERNAL;

    public boolean sendsLocally() {
        return this != EXTERNAL;
    }

    public boolean sendsExternally() {
        return this != LOCAL;
    }
}
