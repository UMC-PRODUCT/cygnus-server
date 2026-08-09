package com.umc.product.test.application.port.out.dto;

/** 벌크 적재 시 명시적 id 를 부여하기 위한 테이블별 현재 max(id). */
public record BulkSeedBaseIds(
    long memberMaxId,
    long challengerMaxId,
    long scheduleMaxId,
    long noticeMaxId
) {
}
