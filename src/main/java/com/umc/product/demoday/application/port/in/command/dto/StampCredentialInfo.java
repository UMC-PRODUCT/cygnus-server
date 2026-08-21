package com.umc.product.demoday.application.port.in.command.dto;

import java.time.Instant;

public record StampCredentialInfo(
    String qrValue,
    Instant generatedAt
) {
}
