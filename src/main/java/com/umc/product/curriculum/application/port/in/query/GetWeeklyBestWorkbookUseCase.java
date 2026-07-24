package com.umc.product.curriculum.application.port.in.query;

import com.umc.product.curriculum.application.port.in.query.dto.GetBestWorkbooksQuery;
import com.umc.product.curriculum.application.port.in.query.dto.WeeklyBestWorkbookPageInfo;

/**
 * 베스트 워크북 조회 UseCase
 */
public interface GetWeeklyBestWorkbookUseCase {

    /**
     * 베스트 워크북 목록 조회 (커서 페이지네이션)
     * <p>
     * 기수 / 학교 / 파트 / 주차 / 스터디 그룹 등 다중 필터를 지원합니다.
     *
     * @param query 필터 및 페이지네이션 정보
     * @return 베스트 워크북 목록
     */
    WeeklyBestWorkbookPageInfo searchBestWorkbooks(GetBestWorkbooksQuery query);
}
