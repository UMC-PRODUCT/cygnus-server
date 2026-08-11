package com.umc.product.inhouse.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UmcProductLeadershipRole {
    UMC_PRODUCT_VICE_LEAD("UMC PRODUCT 부총괄", 0),
    UMC_PRODUCT_LEAD("UMC PRODUCT 총괄", 1);

    private final String displayName;
    private final int sortOrder;
}
