package com.umc.product.demoday.application.port.in.command.dto;

public record CreateDemodayEntryCodeCommand(
    Long pollId,
    int count
) {
}
