package com.umc.product.analytics.application.port.in.query.dto;

public enum Granularity {
    WEEKLY,
    MONTHLY;

    public String dbValue() {
        return this == WEEKLY ? "week" : "month";
    }
}
