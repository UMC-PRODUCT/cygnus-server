package com.umc.product.organization.adapter.in.graphql.dto;

import java.time.LocalDate;

public record UmcProductSquadFilterGraphQlRequest(
    Boolean active,
    LocalDate activeOn
) {
}
