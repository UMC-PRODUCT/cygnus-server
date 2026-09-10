package com.umc.product.challenger.adapter.in.web.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.ChallengerTrack;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateChallengerRecordRequest(
    @NotNull(message = "기수 ID는 필수입니다") Long gisuId,
    @NotNull(message = "지부 ID는 필수입니다") Long chapterId,
    @NotNull(message = "학교 ID는 필수입니다") Long schoolId,
    ChallengerPart part,
    ChallengerTrack track,
    @NotBlank(message = "회원 이름은 필수입니다") @Size(max = 30, message = "회원 이름은 30자 이하여야 합니다") String memberName,
    ChallengerRoleType challengerRoleType
) {
    public CreateChallengerRecordRequest(
        Long gisuId, Long chapterId, Long schoolId, ChallengerPart part,
        String memberName, ChallengerRoleType challengerRoleType
    ) {
        this(gisuId, chapterId, schoolId, part, null, memberName, challengerRoleType);
    }

    @AssertTrue(message = "파트 또는 기본 트랙 하나를 선택하고, 운영진 코드는 파트를 사용해주세요") @JsonIgnore
    public boolean isLearningSelectionValid() {
        if (challengerRoleType != null) {
            return part != null && track == null;
        }
        return (part != null) != (track != null) && (track == null || track.isBasic());
    }
}
