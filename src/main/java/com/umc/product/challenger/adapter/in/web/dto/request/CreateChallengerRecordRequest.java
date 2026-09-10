package com.umc.product.challenger.adapter.in.web.dto.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.common.domain.enums.OrganizationType;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateChallengerRecordRequest(
    @NotNull(message = "기수 ID는 필수입니다") Long gisuId,
    Long chapterId,
    @NotNull(message = "학교 ID는 필수입니다") Long schoolId,
    ChallengerPart part,
    ChallengerTrack track,
    @NotBlank(message = "회원 이름은 필수입니다") @Size(max = 30, message = "회원 이름은 30자 이하여야 합니다") String memberName,
    ChallengerRoleType challengerRoleType,
    List<@NotNull ChallengerTrack> tracks
) {
    public CreateChallengerRecordRequest(
        Long gisuId, Long chapterId, Long schoolId, ChallengerPart part, ChallengerTrack track,
        String memberName, ChallengerRoleType challengerRoleType
    ) {
        this(gisuId, chapterId, schoolId, part, track, memberName, challengerRoleType, null);
    }

    public CreateChallengerRecordRequest(
        Long gisuId, Long chapterId, Long schoolId, ChallengerPart part,
        String memberName, ChallengerRoleType challengerRoleType
    ) {
        this(gisuId, chapterId, schoolId, part, null, memberName, challengerRoleType);
    }

    @AssertTrue(message = "파트 또는 기본 트랙 목록을 선택하고 track과 tracks를 동시에 지정하지 마세요") @JsonIgnore
    public boolean isLearningSelectionValid() {
        if (track != null && tracks != null) {
            return false;
        }
        List<ChallengerTrack> selectedTracks = tracks == null ? (track == null ? List.of() : List.of(track)) : tracks;
        if (selectedTracks.stream().anyMatch(value -> value == null || !value.isBasic())) {
            return false;
        }
        if (challengerRoleType != null) {
            return true;
        }
        return (part != null && selectedTracks.isEmpty()) || (part == null && !selectedTracks.isEmpty());
    }

    @AssertTrue(message = "수강하지 않는 중앙 운영진 코드만 지부를 생략할 수 있습니다") @JsonIgnore
    public boolean isChapterSelectionValid() {
        return chapterId != null || (challengerRoleType != null
            && challengerRoleType.organizationType() == OrganizationType.CENTRAL
            && track == null && (tracks == null || tracks.isEmpty()));
    }
}
