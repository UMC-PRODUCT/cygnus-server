package com.umc.product.demoday.application.port.in.query.dto;

import com.umc.product.demoday.domain.DemodayBooth;

public record DemodayBoothInfo(
        Long boothId,
        Long projectId,
        String displayName
) {

    public static DemodayBoothInfo from(DemodayBooth booth) {
        return new DemodayBoothInfo(booth.getId(), booth.getProjectId(), booth.getDisplayName());
    }
}
