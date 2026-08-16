package com.umc.product.curriculum.adapter.in.web.v2.dto.response;

import java.time.Instant;
import java.util.List;

import com.umc.product.curriculum.application.port.in.query.dto.OriginalWorkbookInfo;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookStatus;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookType;

/**
 * OriginalWorkbook에 대한 정보를 조회합니다.
 *
 * @param originalWorkbookId PK
 * @param title              원본 워크북 제목
 * @param description        (nullable) 원본 워크북 설명
 * @param content            (nullable) 원본 워크북 내용
 * @param url                (nullable) 원본 워크북 URL
 * @param type               워크북 유형 (메인/부록 여부)
 * @param status             워크북 상태 (임시저장, 배포 준비됨,배포됨 등)
 */
public record OriginalWorkbookResponse(
    Long originalWorkbookId,
    String title,
    String description,
    String url,
    String content,
    OriginalWorkbookType type,
    OriginalWorkbookStatus status,
    Instant releasedAt,
    Long releasedMemberId,
    OriginalWorkbookMissionResponse missions,
    List<OriginalWorkbookMissionResponse> missionList
) {
    public static OriginalWorkbookResponse from(OriginalWorkbookInfo info) {
        List<OriginalWorkbookMissionResponse> missionList = info.missions().stream()
            .map(mission -> OriginalWorkbookMissionResponse.from(info.originalWorkbookId(), mission))
            .toList();
        return new OriginalWorkbookResponse(
            info.originalWorkbookId(), info.title(), info.description(), info.url(), info.content(),
            info.type(), info.status(), info.releasedAt(), info.releasedMemberId(),
            missionList.isEmpty() ? null : missionList.get(0),
            missionList
        );
    }
}
