package com.umc.product.test.application.port.out.dto;

public record SeedMemberRow(
    long id,
    String name,
    String nickname,
    String email,
    Long schoolId
) {
}
