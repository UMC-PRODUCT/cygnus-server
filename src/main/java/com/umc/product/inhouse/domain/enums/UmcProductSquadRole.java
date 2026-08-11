package com.umc.product.inhouse.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UmcProductSquadRole {
    MEMBER("Member", 0),
    SQUAD_LEAD("Squad Lead", 1);

    private final String displayName;
    private final int sortOrder;
}
