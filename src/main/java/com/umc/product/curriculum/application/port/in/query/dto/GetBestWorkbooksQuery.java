package com.umc.product.curriculum.application.port.in.query.dto;

import java.util.List;
import java.util.Set;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;

/**
 * 베스트 워크북 목록 조회 쿼리
 * <p>
 * 다중 선택 필터를 지원하며, 제공된 값들의 카르테시안 곱으로 결과를 반환합니다.
 *
 * @param gisuId       기수 ID (nullable: 미제공 시 전체)
 * @param schoolIds    학교 ID 목록 (nullable: 미제공 시 전체)
 * @param parts        파트 목록 (nullable: 미제공 시 전체)
 * @param weekNos      주차 번호 목록 (nullable: 미제공 시 전체)
 * @param studyGroupIds 스터디 그룹 ID 목록 (nullable: 미제공 시 전체)
 * @param page         0부터 시작하는 페이지 번호 (기본값: 0)
 * @param size         페이지 크기 (기본값: 20)
 */
public record GetBestWorkbooksQuery(
    Long gisuId,
    Set<Long> schoolIds,
    Set<Long> memberIds,
    Set<ChallengerPart> parts,
    List<Long> weekNos,
    List<Long> studyGroupIds,
    int page,
    int size,
    Set<ChallengerTrack> tracks
) {
    private static final int DEFAULT_SIZE = 20;

    public GetBestWorkbooksQuery(
        Long gisuId, Set<Long> schoolIds, Set<Long> memberIds, Set<ChallengerPart> parts,
        List<Long> weekNos, List<Long> studyGroupIds, int page, int size
    ) {
        this(gisuId, schoolIds, memberIds, parts, weekNos, studyGroupIds, page, size, null);
    }

    public GetBestWorkbooksQuery {
        if (page < 0) {
            page = 0;
        }
        if (size <= 0) {
            size = DEFAULT_SIZE;
        }
    }

    public static GetBestWorkbooksQuery of(
        Long gisuId,
        Set<Long> schoolIds,
        Set<ChallengerPart> parts,
        List<Long> weekNos,
        List<Long> studyGroupIds,
        Integer page,
        Integer size
    ) {
        return new GetBestWorkbooksQuery(
            gisuId, schoolIds, null, parts, weekNos, studyGroupIds,
            page == null ? 0 : page,
            size == null ? DEFAULT_SIZE : size
        );
    }

    public GetBestWorkbooksQuery withMemberIds(Set<Long> memberIds) {
        return new GetBestWorkbooksQuery(
            gisuId, schoolIds, memberIds, parts, weekNos, studyGroupIds, page, size, tracks
        );
    }

    public GetBestWorkbooksQuery withTracks(Set<ChallengerTrack> tracks) {
        return new GetBestWorkbooksQuery(
            gisuId, schoolIds, memberIds, parts, weekNos, studyGroupIds, page, size, tracks
        );
    }
}
