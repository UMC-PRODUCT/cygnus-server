package com.umc.product.challenger.application.port.in.command.dto;

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
    ChallengerRoleType challengerRoleType
) {
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
            + ", track=" + track
            + '}';
    }

    private boolean isAdminRecord() {
        return challengerRoleType != null;
    }

    public ChallengerRecord toEntity() {
        if (isAdminRecord()) {
            if (track != null) {
                throw new ChallengerDomainException(ChallengerErrorCode.INVALID_CHALLENGER_RECORD_CREATE_REQUEST,
                    "운영진 코드에는 수강 트랙을 지정할 수 없습니다.");
            }
            Long adminOrganizationId = switch (challengerRoleType.organizationType()) {
                case CENTRAL -> null; // 중앙운영사무국 소속은 organizationId가 필요없음
                case CHAPTER -> chapterId; // 챕터 관리자: organizationId는 chapterId
                case SCHOOL -> schoolId; // 학교 관리자: organizationId는 schoolId
            };

            return ChallengerRecord.createAdmin(
                creatorMemberId, gisuId, chapterId, schoolId, part, memberName,
                challengerRoleType, adminOrganizationId
            );
        } else {
            return ChallengerRecord.create(
                creatorMemberId, gisuId, chapterId, schoolId, part, track, memberName
            );
        }
    }
}
