package com.umc.product.community.adapter.in.web.dto.request;

import java.util.List;

import org.hibernate.validator.constraints.UniqueElements;

import com.umc.product.community.adapter.in.web.validation.CodePointLength;
import com.umc.product.community.adapter.in.web.validation.SingleGrapheme;
import com.umc.product.community.application.port.in.command.thread.dto.CreateCommunityThreadCommand;
import com.umc.product.community.domain.enums.CommunityThreadCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateCommunityThreadRequest(
    @NotBlank @CodePointLength(max = 80) String title,
    @CodePointLength(max = 500) String description,
    @NotNull CommunityThreadCategory category,
    @NotBlank @CodePointLength(max = 32) @SingleGrapheme String icon,
    @Size(max = 99) @UniqueElements List<@NotNull @Positive Long> memberIds
) {

    public CreateCommunityThreadRequest {
        memberIds = memberIds == null ? List.of() : List.copyOf(memberIds);
    }

    public CreateCommunityThreadCommand toCommand(Long actorMemberId) {
        return new CreateCommunityThreadCommand(
            actorMemberId,
            title,
            description,
            category,
            icon,
            memberIds
        );
    }
}
