package com.umc.product.inhouse.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UmcProductPosition {
    UNSPECIFIED("미지정", 0),
    PRODUCT_OWNER("Product Owner", 1),
    PRODUCT_DESIGNER("Product Designer", 2),
    IOS_DEVELOPER("iOS Developer", 3),
    ANDROID_DEVELOPER("Android Developer", 4),
    WEB_DEVELOPER("Web Developer", 5),
    SERVER_DEVELOPER("Server Developer", 6),
    ETC("기타", 99);

    private final String displayName;
    private final int sortOrder;
}
