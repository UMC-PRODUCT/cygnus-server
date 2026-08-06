package com.umc.product.form.application.port.in.command.dto;

import lombok.Builder;

/**
 * 폼을 PUBLISHED 상태로 전환하는 Command. 이 이후부터 응답 수집 가능.
 */
@Builder
public record PublishFormCommand(
    Long formId,
    Long requesterMemberId
) {
}
