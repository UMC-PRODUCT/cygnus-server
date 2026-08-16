package com.umc.product.notification.application.port.out.dto;

import java.util.List;
import java.util.Map;

public record FcmSendRequest(
    List<FcmSendTarget> targets,
    String title,
    String body,
    Map<String, String> data,
    String imageUrl,
    String deepLink
) {

    public FcmSendRequest {
        targets = targets == null ? List.of() : List.copyOf(targets);
        data = data == null ? Map.of() : Map.copyOf(data);
    }

    public static FcmSendRequest of(
        List<FcmSendTarget> targets,
        String title,
        String body,
        Map<String, String> data,
        String imageUrl,
        String deepLink
    ) {
        return new FcmSendRequest(targets, title, body, data, imageUrl, deepLink);
    }
}
