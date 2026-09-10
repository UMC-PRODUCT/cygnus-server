package com.umc.product.organization.adapter.in.web.dto.request;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.organization.application.port.in.command.dto.UpdateStudyGroupCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "스터디 그룹 수정 요청")
public record UpdateStudyGroupRequest(

    @Schema(description = "그룹명", example = "React A팀", minLength = 1, maxLength = 50)
    @Size(min = 1, max = 50, message = "그룹명은 1~50자") String name,

    @Schema(description = "파트 (미전달 시 변경 없음)", example = "WEB")
    ChallengerPart part,
    ChallengerTrack track
) {
    public UpdateStudyGroupRequest(String name, ChallengerPart part) {
        this(name, part, null);
    }

    public UpdateStudyGroupCommand toCommand(Long groupId) {
        return new UpdateStudyGroupCommand(groupId, name, part, track);
    }

}
