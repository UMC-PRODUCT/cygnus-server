package com.umc.product.test.application.port.in.command.dto;

import java.util.List;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;

/**
 * 챌린저 분포 시딩 Command. ADR-017 참조.
 *
 * @param gisuId                대상 기수 (null 이면 활성 기수)
 * @param countPerPartPerSchool 한 (Chapter, School, Part) 셀당 생성할 챌린저 수
 * @param parts                 시딩할 파트 목록 (null/empty 면 ADMIN 제외 전 파트)
 * @param chapterIds            대상 Chapter 목록 (null/empty 면 해당 기수 전체)
 * @param countPerTrackPerSchool 한 (Chapter, School, Track) 셀당 생성할 챌린저 수
 * @param tracks                 시딩할 트랙 목록 (null/empty 면 기본 트랙 전체)
 */
public record SeedChallengersCommand(
    Long gisuId,
    Integer countPerPartPerSchool,
    List<ChallengerPart> parts,
    List<Long> chapterIds,
    Integer countPerTrackPerSchool,
    List<ChallengerTrack> tracks
) {
    public SeedChallengersCommand(
        Long gisuId, int countPerPartPerSchool, List<ChallengerPart> parts, List<Long> chapterIds
    ) {
        this(gisuId, countPerPartPerSchool, parts, chapterIds, null, null);
    }
}
