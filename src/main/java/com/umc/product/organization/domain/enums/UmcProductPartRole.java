package com.umc.product.organization.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UmcProductPartRole {
    MEMBER("멤버", 0),
    PART_LEAD("파트 리드", 1);

    private final String displayName;
    private final int sortOrder;
}
