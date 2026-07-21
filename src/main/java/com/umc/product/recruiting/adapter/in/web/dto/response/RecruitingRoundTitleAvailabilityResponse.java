package com.umc.product.recruiting.adapter.in.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "모집 제목 사용 가능 여부")
public record RecruitingRoundTitleAvailabilityResponse(boolean available) {
}
