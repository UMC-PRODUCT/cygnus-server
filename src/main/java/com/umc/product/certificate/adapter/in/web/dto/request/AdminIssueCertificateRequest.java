package com.umc.product.certificate.adapter.in.web.dto.request;

import com.umc.product.certificate.application.port.in.command.dto.AdminIssueCertificateCommand;
import com.umc.product.certificate.domain.CertificateTemplate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "운영진 인증서 발급 요청")
public record AdminIssueCertificateRequest(
    @Schema(
        description = "PDF 배경과 문구 규칙을 선택하는 템플릿입니다. 수료증/공로증/상장은 이 값을 사용합니다.",
        example = "UMC_DEMO_DAY_FIRST_PRIZE",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "template은 필수입니다.") CertificateTemplate template,

    @Schema(description = "인증서를 받을 회원 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "수신자 회원 ID는 필수입니다.") Long recipientMemberId,

    @Schema(description = "인증서에 표시하고 자격을 판정할 기수 ID", example = "7", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "기수 ID는 필수입니다.") Long gisuId,

    @Schema(
        description = "공로증/상장 제목 override입니다. 비우면 template의 기본 상명이 사용됩니다.",
        example = "커스텀 공로상"
    )
    @Size(max = 100, message = "공로증 제목은 100자 이하로 입력해주세요.") String meritTitle,

    @Schema(
        description = "공로증/상장 본문 override입니다. 비우면 template과 기수 정보로 기본 문구를 생성합니다.",
        example = "탁월한 기여를 인정하여 이 상장을 수여합니다."
    )
    @Size(max = 500, message = "공로증 설명은 500자 이하로 입력해주세요.") String meritDescription,

    @Schema(description = "동일 범위 유효 인증서가 있을 때 기존 인증서를 폐기하고 재발급할지 여부", example = "false")
    boolean reissue
) {

    public AdminIssueCertificateCommand toCommand(Long requesterMemberId) {
        return AdminIssueCertificateCommand.builder()
            .template(template)
            .requesterMemberId(requesterMemberId)
            .recipientMemberId(recipientMemberId)
            .gisuId(gisuId)
            .meritTitle(meritTitle)
            .meritDescription(meritDescription)
            .reissue(reissue)
            .build();
    }

}
