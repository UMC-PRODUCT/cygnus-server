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
import com.umc.product.organization.adapter.in.web.dto.request.CreateUmcProductChapterRequest;
import com.umc.product.organization.adapter.in.web.dto.request.UpdateUmcProductChapterRequest;
import com.umc.product.organization.application.port.in.command.ManageUmcProductChapterUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/umc-product/chapters")
@RequiredArgsConstructor
@Tag(name = "Organization | UMC PRODUCT Chapter Command", description = "UMC PRODUCT Chapter를 관리합니다.")
public class UmcProductChapterCommandController {

    private final ManageUmcProductChapterUseCase manageUmcProductChapterUseCase;

    @PostMapping
    @Operation(operationId = "UMC-PRODUCT-CHAPTER-001", summary = "UMC PRODUCT Chapter 생성")
    public Long create(
        @CurrentMember MemberPrincipal currentMember,
        @RequestBody @Valid CreateUmcProductChapterRequest request
    ) {
        return manageUmcProductChapterUseCase.create(request.toCommand(currentMemberId(currentMember)));
    }

    @PatchMapping("/{chapterId}")
    @Operation(operationId = "UMC-PRODUCT-CHAPTER-002", summary = "UMC PRODUCT Chapter 수정")
    public void update(
        @PathVariable Long chapterId,
        @CurrentMember MemberPrincipal currentMember,
        @RequestBody @Valid UpdateUmcProductChapterRequest request
    ) {
        manageUmcProductChapterUseCase.update(request.toCommand(chapterId, currentMemberId(currentMember)));
    }

    @DeleteMapping("/{chapterId}")
    @Operation(
        operationId = "UMC-PRODUCT-CHAPTER-003",
        summary = "UMC PRODUCT Chapter 삭제",
        description = "하위 Part가 있는 Chapter는 삭제할 수 없으며 비활성화해야 합니다."
    )
    public void delete(@PathVariable Long chapterId, @CurrentMember MemberPrincipal currentMember) {
        manageUmcProductChapterUseCase.delete(chapterId, currentMemberId(currentMember));
    }

    private Long currentMemberId(MemberPrincipal currentMember) {
        if (currentMember == null) {
            throw new AccessDeniedException("인증이 필요합니다.");
        }
        return currentMember.getMemberId();
    }
}
