package com.umc.product.analytics.domain;

import org.springframework.http.HttpStatus;

import com.umc.product.global.response.code.BaseCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AnalyticsErrorCode implements BaseCode {
    RESOURCE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "ANALYTICS-0001",
        "운영진 대시보드는 권한이 있는 운영진만 볼 수 있어요. 필요한 권한이 있다면 운영진에게 문의해주세요."),
    INVALID_SORT(HttpStatus.BAD_REQUEST, "ANALYTICS-0002", "지원하지 않는 정렬 조건이에요. 정렬 값을 확인해주세요."),
    INVALID_PERIOD(HttpStatus.BAD_REQUEST, "ANALYTICS-0003", "조회 시작 시각은 종료 시각보다 빨라야 해요. 기간을 다시 선택해주세요."),
    SCHOOL_ID_REQUIRED(HttpStatus.BAD_REQUEST, "ANALYTICS-0004", "학교 ID가 필요해요. schoolId 파라미터를 전달해주세요.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
