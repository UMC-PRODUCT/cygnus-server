package com.umc.product.notification.application.port.in.dto;

import java.util.Map;

public record SendHtmlEmailCommand(
    String to,
    String subject,
    String templateName,
    Map<String, Object> variables
) {

    public SendHtmlEmailCommand {
        variables = Map.copyOf(variables);
    }
}
