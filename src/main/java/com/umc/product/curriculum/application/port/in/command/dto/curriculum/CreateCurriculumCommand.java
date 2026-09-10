package com.umc.product.curriculum.application.port.in.command.dto.curriculum;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

/**
 * 커리큘럼 생성 커맨드
 *
 * @param gisuId 기수 ID
 * @param part   파트
 * @param title  커리큘럼 제목
 */
@Builder
public record CreateCurriculumCommand(
    @NotNull(message = "기수 ID는 필수입니다") Long gisuId,

    ChallengerPart part,

    ChallengerTrack track,

    @NotBlank(message = "제목은 필수입니다") String title
) {
    public CreateCurriculumCommand(Long gisuId, ChallengerPart part, String title) {
        this(gisuId, part, null, title);
    }
}
