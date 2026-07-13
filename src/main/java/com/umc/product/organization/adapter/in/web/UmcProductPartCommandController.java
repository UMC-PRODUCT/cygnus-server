package com.umc.product.organization.adapter.in.web;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.organization.adapter.in.web.dto.request.CreateUmcProductPartRequest;
import com.umc.product.organization.adapter.in.web.dto.request.UpdateUmcProductPartRequest;
import com.umc.product.organization.application.port.in.command.ManageUmcProductPartUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/umc-product/parts")
@RequiredArgsConstructor
@Tag(name = "Organization | UMC PRODUCT Part Command", description = "UMC PRODUCT Part를 관리합니다.")
public class UmcProductPartCommandController {

    private final ManageUmcProductPartUseCase manageUmcProductPartUseCase;

    @PostMapping
    @Operation(operationId = "UMC-PRODUCT-PART-001", summary = "UMC PRODUCT Part 생성")
    public Long create(
        @CurrentMember MemberPrincipal currentMember,
        @RequestBody @Valid CreateUmcProductPartRequest request
    ) {
        return manageUmcProductPartUseCase.create(request.toCommand(currentMemberId(currentMember)));
    }

    @PatchMapping("/{partId}")
    @Operation(
        operationId = "UMC-PRODUCT-PART-002",
        summary = "UMC PRODUCT Part 수정",
        description = "Part의 Chapter는 변경할 수 없습니다."
    )
    public void update(
        @PathVariable Long partId,
        @CurrentMember MemberPrincipal currentMember,
        @RequestBody @Valid UpdateUmcProductPartRequest request
    ) {
        manageUmcProductPartUseCase.update(request.toCommand(partId, currentMemberId(currentMember)));
    }

    @DeleteMapping("/{partId}")
    @Operation(
        operationId = "UMC-PRODUCT-PART-003",
        summary = "UMC PRODUCT Part 삭제",
        description = "과거를 포함한 소속 이력이 있는 Part는 삭제할 수 없으며 비활성화해야 합니다."
    )
    public void delete(@PathVariable Long partId, @CurrentMember MemberPrincipal currentMember) {
        manageUmcProductPartUseCase.delete(partId, currentMemberId(currentMember));
    }

    private Long currentMemberId(MemberPrincipal currentMember) {
        if (currentMember == null) {
            throw new AccessDeniedException("인증이 필요합니다.");
        }
        return currentMember.getMemberId();
    }
}
