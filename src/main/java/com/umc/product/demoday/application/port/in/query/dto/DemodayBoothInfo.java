package com.umc.product.demoday.application.port.in.query.dto;

public record DemodayBoothInfo(
        Long boothId,
        Long projectId,
        String displayName
) {
}
