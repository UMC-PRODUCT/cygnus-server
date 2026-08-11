package com.umc.product.inhouse.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UmcProductDepartmentRole {
    MEMBER("Member", 0),
    DEPARTMENT_LEAD("Department Lead", 1);

    private final String displayName;
    private final int sortOrder;
}
