package com.umc.product.demoday.application.port.in.command.dto;

public record CreateStampCredentialCommand(
    Long pollId,
    Long boothId
) {
}
