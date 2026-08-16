package com.umc.product.recruiting.application.port.in.query.dto;

import java.time.Instant;

/**
 * 차수 설정에 조회 시점 부가 정보를 얹은 값.
 *
 * <p>작성자와 지원자 유무는 차수의 설정이 아니라 목록 화면이 필요로 하는 조회 결과라
 * {@link RecruitingRoundConfigurationInfo}를 오염시키지 않고 여기서 합친다.
 */
public record RecruitingRoundDetailInfo(
    RecruitingRoundConfigurationInfo configuration,
    Instant createdAt,
    RecruitingRoundAuthorInfo author,
    boolean hasApplicants
) {

    public static RecruitingRoundDetailInfo of(
        RecruitingRoundConfigurationInfo configuration,
        Instant createdAt,
        RecruitingRoundAuthorInfo author,
        boolean hasApplicants
    ) {
        return new RecruitingRoundDetailInfo(configuration, createdAt, author, hasApplicants);
    }
}
