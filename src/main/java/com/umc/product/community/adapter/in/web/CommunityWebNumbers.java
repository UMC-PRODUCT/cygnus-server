package com.umc.product.community.adapter.in.web;

public final class CommunityWebNumbers {

    private CommunityWebNumbers() {
    }

    public static Long id(String value) {
        return Long.valueOf(value);
    }

    public static Long optionalId(String value) {
        return value == null ? null : id(value);
    }

    public static String text(Number value) {
        return value == null ? null : value.toString();
    }
}
