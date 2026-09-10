package com.umc.product.challenger.application.port.in.command.dto;

import java.util.List;

import com.umc.product.challenger.domain.ChallengerRecord;
import com.umc.product.challenger.domain.exception.ChallengerDomainException;
import com.umc.product.challenger.domain.exception.ChallengerErrorCode;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.ChallengerTrack;

import lombok.Builder;

@Builder
public record CreateChallengerRecordCommand(
    Long creatorMemberId,
    Long gisuId,
    Long chapterId,
    Long schoolId,
    ChallengerPart part,
    ChallengerTrack track,
    String memberName,
    ChallengerRoleType challengerRoleType,
    List<ChallengerTrack> tracks
) {
    public CreateChallengerRecordCommand {
        if (track != null && tracks != null) {
            throw new ChallengerDomainException(ChallengerErrorCode.INVALID_CHALLENGER_RECORD_CREATE_REQUEST,
                "track과 tracks를 동시에 지정할 수 없습니다.");
        }
        tracks = tracks == null ? (track == null ? List.of() : List.of(track)) : tracks;
        if (tracks.stream().anyMatch(value -> value == null || !value.isBasic())) {
            throw new ChallengerDomainException(ChallengerErrorCode.INVALID_CHALLENGER_RECORD_CREATE_REQUEST);
        }
        tracks = List.copyOf(tracks);
        track = tracks.size() == 1 ? tracks.getFirst() : null;
    }

    public CreateChallengerRecordCommand(
        Long creatorMemberId, Long gisuId, Long chapterId, Long schoolId, ChallengerPart part,
        ChallengerTrack track, String memberName, ChallengerRoleType challengerRoleType
    ) {
        this(creatorMemberId, gisuId, chapterId, schoolId, part, track, memberName, challengerRoleType, null);
    }

    public CreateChallengerRecordCommand(
        Long creatorMemberId, Long gisuId, Long chapterId, Long schoolId, ChallengerPart part,
        String memberName, ChallengerRoleType challengerRoleType
    ) {
        this(creatorMemberId, gisuId, chapterId, schoolId, part, null, memberName, challengerRoleType);
    }

    @Override
    public String toString() {
        return "CreateChallengerRecordCommand{"
            + "creatorMemberId=" + creatorMemberId
            + ", gisuId=" + gisuId
            + ", chapterId=" + chapterId
            + ", schoolId=" + schoolId
            + ", part=" + part
            + ", tracks=" + tracks
            + '}';
    }

    private boolean isAdminRecord() {
        return challengerRoleType != null;
    }

    public ChallengerRecord toEntity() {
        if (isAdminRecord()) {
            Long adminOrganizationId = switch (challengerRoleType.organizationType()) {
                case CENTRAL -> null; // 중앙운영사무국 소속은 organizationId가 필요없음
                case CHAPTER -> chapterId; // 챕터 관리자: organizationId는 chapterId
                case SCHOOL -> schoolId; // 학교 관리자: organizationId는 schoolId
            };

            return ChallengerRecord.createAdminWithTracks(
                creatorMemberId, gisuId, chapterId, schoolId, part, tracks, memberName,
                challengerRoleType, adminOrganizationId
            );
        } else {
            return ChallengerRecord.createWithTracks(
                creatorMemberId, gisuId, chapterId, schoolId, part, tracks, memberName
            );
        }
    }
}
