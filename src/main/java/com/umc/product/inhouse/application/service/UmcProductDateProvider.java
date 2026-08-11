package com.umc.product.inhouse.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UmcProductDateProvider {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    private final Clock clock;

    public LocalDate today() {
        return clock.instant().atZone(KOREA_ZONE).toLocalDate();
    }
}
