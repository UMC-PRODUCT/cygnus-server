package com.umc.product.recruiting.adapter.in.web.dto.request;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.command.dto.AddRecruitingFormSectionPolicyCommand;
import com.umc.product.recruiting.domain.enums.RecruitingFormSectionType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "지원 폼 섹션 정책 추가 요청")
public record RecruitingFormSectionPolicyRequest(
    @Schema(description = "Form 섹션 ID", example = "300")
    @NotNull @Positive Long formSectionId,
    @Schema(description = "공통 또는 트랙 전용 섹션 유형", example = "TRACK")
    @NotNull RecruitingFormSectionType type,
    @Schema(description = "TRACK 유형의 모집 트랙", example = "PLAN")
    ChallengerTrack track
) {

    @AssertTrue(message = "TRACK 섹션에는 track이 필요하고 COMMON 섹션에는 track을 지정할 수 없습니다.") public boolean isTrackConfigurationValid() {
        if (type == null) {
            return true;
        }
        return type == RecruitingFormSectionType.TRACK ? track != null : track == null;
    }

    public AddRecruitingFormSectionPolicyCommand toCommand(Long applicationFormId) {
        return AddRecruitingFormSectionPolicyCommand.builder()
            .applicationFormId(applicationFormId)
            .formSectionId(formSectionId)
            .type(type)
            .track(track)
            .build();
    }
}
