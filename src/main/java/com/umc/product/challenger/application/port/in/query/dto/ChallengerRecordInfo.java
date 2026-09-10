package com.umc.product.challenger.application.port.in.query.dto;

import java.time.Instant;

import com.umc.product.challenger.domain.ChallengerRecord;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.ChallengerTrack;

import lombok.Builder;

@Builder
public record ChallengerRecordInfo(
    Long id,
    String code,
    String memberName,
    ChallengerRoleType challengerRoleType,
    Long organizationId,
    Long createdMemberId,
    Long gisuId,
    Long chapterId,
    Long schoolId,
    ChallengerPart part,
    ChallengerTrack track,
    boolean isUsed,
    Long usedMemberId,
    Instant usedAt
) {
    public ChallengerRecordInfo(
        Long id, String code, String memberName, ChallengerRoleType challengerRoleType,
        Long organizationId, Long createdMemberId, Long gisuId, Long chapterId, Long schoolId,
        ChallengerPart part, boolean isUsed, Long usedMemberId, Instant usedAt
    ) {
        this(id, code, memberName, challengerRoleType, organizationId, createdMemberId, gisuId,
            chapterId, schoolId, part, null, isUsed, usedMemberId, usedAt);
    }

    public static ChallengerRecordInfo from(ChallengerRecord entity) {
        return ChallengerRecordInfo.builder()
            .id(entity.getId())
            .code(entity.getCode())
            .memberName(entity.getMemberName())
            .challengerRoleType(entity.getChallengerRoleType())
            .organizationId(entity.getOrganizationId())
            .createdMemberId(entity.getCreatedMemberId())
            .gisuId(entity.getGisuId())
            .chapterId(entity.getChapterId())
            .schoolId(entity.getSchoolId())
            .part(entity.getPart())
            .track(entity.getTrack())
            .isUsed(entity.isUsed())
            .usedMemberId(entity.getUsedMemberId())
            .usedAt(entity.getUsedAt())
            .build();
    }
}
