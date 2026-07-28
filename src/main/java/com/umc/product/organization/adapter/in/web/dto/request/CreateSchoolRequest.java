package com.umc.product.organization.adapter.in.web.dto.request;

import com.umc.product.organization.application.port.in.command.dto.CreateSchoolCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Builder;

@Builder
@Schema(description = "학교 생성 요청")
public record CreateSchoolRequest(
        @Schema(description = "학교명", example = "서울대학교", minLength = 2, maxLength = 50)
        @NotBlank @Size(min = 2, max = 50, message = "학교명은 2~50자")
        String schoolName,

        @Schema(description = "학교 약칭", example = "서울대", maxLength = 20)
        @Size(max = 20, message = "학교 약칭은 20자 이내")
        String shortName,

        @Schema(description = "비고", example = "관악캠퍼스", maxLength = 200)
        @Size(max = 200, message = "비고는 200자 이내")
        String remark,

        @Schema(description = "로고 이미지 파일 ID (presigned URL 업로드 후 전달)", example = "abc123-def456")
        String logoImageId,

        @Schema(description = "학교 링크 목록")
        @Valid
        List<SchoolLinkRequest> links
) {
    public CreateSchoolCommand toCommand() {
        return new CreateSchoolCommand(
                schoolName,
                shortName,
                remark,
                logoImageId,
                links != null ? links.stream().map(SchoolLinkRequest::toCommand).toList() : List.of()
        );
    }
}
