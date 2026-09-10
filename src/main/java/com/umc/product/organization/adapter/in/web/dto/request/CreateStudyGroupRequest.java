package com.umc.product.organization.adapter.in.web.dto.request;

import java.util.Set;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.organization.application.port.in.command.dto.CreateStudyGroupCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "스터디 그룹 생성 요청")
public record CreateStudyGroupRequest(
        @Schema(description = "그룹명", example = "React A팀", minLength = 1, maxLength = 50)
        @NotBlank(message = "그룹명은 필수입니다") @Size(min = 1, max = 50, message = "그룹명은 1~50자") String name,

        @Schema(description = "기수 ID", example = "9")
        @NotNull(message = "기수 ID는 필수입니다") Long gisuId,

        @Schema(description = "파트", example = "WEB")
        ChallengerPart part,

        @Schema(description = "파트장 ID 목록", example = "[101, 102]")
        @NotEmpty Set<Long> mentorIds,

        @Schema(description = "스터디원 회원 ID 목록", example = "[102, 103, 104]")
        @NotEmpty Set<Long> memberIds,

        @Schema(description = "Track 기수의 기본 트랙")
        ChallengerTrack track
) {
    public CreateStudyGroupRequest(
        String name, Long gisuId, ChallengerPart part, Set<Long> mentorIds, Set<Long> memberIds
    ) {
        this(name, gisuId, part, mentorIds, memberIds, null);
    }

    public CreateStudyGroupCommand toCommand() {
        return new CreateStudyGroupCommand(name, gisuId, part, mentorIds,
            memberIds, track);
    }
}
