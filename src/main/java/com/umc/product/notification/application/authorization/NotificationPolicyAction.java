package com.umc.product.notification.application.authorization;

public enum NotificationPolicyAction {
    SEND_FCM("notification:send-fcm"),
    DELETE_TOKEN("notification:delete-token");

    private final String id;

    NotificationPolicyAction(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
