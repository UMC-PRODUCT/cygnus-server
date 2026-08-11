package com.umc.product.inhouse.application.port.in.command.dto;

import java.time.LocalDate;

import com.umc.product.inhouse.domain.enums.UmcProductLeadershipRole;

public record RegisterUmcProductLeadershipCommand(
    UmcProductLeadershipRole role,
    LocalDate startDate,
    LocalDate endDate
) {
}
