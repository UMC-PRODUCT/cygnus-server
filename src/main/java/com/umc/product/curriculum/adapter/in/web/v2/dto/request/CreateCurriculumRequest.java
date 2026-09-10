package com.umc.product.curriculum.adapter.in.web.v2.dto.request;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.curriculum.application.port.in.command.dto.curriculum.CreateCurriculumCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCurriculumRequest(
    @NotNull(message = "기수 ID는 필수입니다") Long gisuId,

    ChallengerPart part,

    ChallengerTrack track,

    @NotBlank(message = "제목은 필수입니다") String title
) {

    public CreateCurriculumRequest(Long gisuId, ChallengerPart part, String title) {
        this(gisuId, part, null, title);
    }

    public CreateCurriculumCommand toCommand() {
        return CreateCurriculumCommand.builder()
            .gisuId(gisuId)
            .part(part)
            .track(track)
            .title(title)
            .build();
    }
}
