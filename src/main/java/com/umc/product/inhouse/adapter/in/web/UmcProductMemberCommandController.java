package com.umc.product.inhouse.adapter.in.web;

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
import com.umc.product.inhouse.adapter.in.web.dto.request.CreateUmcProductChapterMembershipRequest;
import com.umc.product.inhouse.adapter.in.web.dto.request.CreateUmcProductLeadershipRequest;
import com.umc.product.inhouse.adapter.in.web.dto.request.CreateUmcProductMemberActivityPeriodRequest;
import com.umc.product.inhouse.adapter.in.web.dto.request.CreateUmcProductMemberRequest;
import com.umc.product.inhouse.adapter.in.web.dto.request.RegisterUmcProductMemberRequest;
import com.umc.product.inhouse.adapter.in.web.dto.request.UpdateUmcProductChapterMembershipRequest;
import com.umc.product.inhouse.adapter.in.web.dto.request.UpdateUmcProductLeadershipRequest;
import com.umc.product.inhouse.adapter.in.web.dto.request.UpdateUmcProductMemberActivityPeriodRequest;
import com.umc.product.inhouse.adapter.in.web.dto.request.UpdateUmcProductMemberProfileRequest;
import com.umc.product.inhouse.adapter.in.web.dto.response.RegisterUmcProductMemberResponse;
import com.umc.product.inhouse.application.port.in.command.ManageUmcProductMemberUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/umc-product/members")
@RequiredArgsConstructor
@Tag(name = "Organization | UMC PRODUCT 멤버 Command", description = "UMC PRODUCT 멤버와 활동 이력을 관리합니다.")
public class UmcProductMemberCommandController {

    private final ManageUmcProductMemberUseCase manageUmcProductMemberUseCase;

    @PostMapping
    @Operation(
        operationId = "UMC-PRODUCT-MEMBER-001",
        summary = "UMC PRODUCT 멤버 생성",
        description = "기본 프로필과 한 개 이상의 활동 기간으로 UMC PRODUCT 멤버를 등록합니다."
    )
    public Long create(
        @CurrentMember MemberPrincipal currentMember,
        @RequestBody @Valid CreateUmcProductMemberRequest request
    ) {
        return manageUmcProductMemberUseCase.create(request.toCommand(currentMemberId(currentMember)));
    }

    @PostMapping("/register")
    @Operation(
        operationId = "UMC-PRODUCT-MEMBER-013",
        summary = "UMC PRODUCT 인원 등록과 계정 자동 발급",
        description = "인원과 초기 조직 소속을 등록하고 영어 닉네임 기반 계정을 발급합니다. 임시 비밀번호는 응답에서 한 번만 노출됩니다."
    )
    public RegisterUmcProductMemberResponse register(
        @CurrentMember MemberPrincipal currentMember,
        @RequestBody @Valid RegisterUmcProductMemberRequest request
    ) {
        return RegisterUmcProductMemberResponse.from(
            manageUmcProductMemberUseCase.register(request.toCommand(currentMemberId(currentMember)))
        );
    }

    @PatchMapping("/{memberId}/profile")
    @Operation(
        operationId = "UMC-PRODUCT-MEMBER-002",
        summary = "UMC PRODUCT 멤버 프로필 수정",
        description = "UMC PRODUCT 멤버의 소개와 UMC PRODUCT 전용 프로필 이미지를 수정합니다. 본인 또는 멤버 프로필 관리 권한을 가진 요청자만 호출할 수 있습니다."
    )
    public void updateProfile(
        @PathVariable Long memberId,
        @CurrentMember MemberPrincipal currentMember,
        @RequestBody @Valid UpdateUmcProductMemberProfileRequest request
    ) {
        manageUmcProductMemberUseCase.updateProfile(
            request.toCommand(memberId, currentMemberId(currentMember))
        );
    }

    @PostMapping("/{memberId}/activity-periods")
    @Operation(
        operationId = "UMC-PRODUCT-MEMBER-003",
        summary = "UMC PRODUCT 멤버 활동 기간 생성"
    )
    public Long createActivityPeriod(
        @PathVariable Long memberId,
        @CurrentMember MemberPrincipal currentMember,
        @RequestBody @Valid CreateUmcProductMemberActivityPeriodRequest request
    ) {
        return manageUmcProductMemberUseCase.createActivityPeriod(
            request.toCommand(memberId, currentMemberId(currentMember))
        );
    }

    @PatchMapping("/{memberId}/activity-periods/{periodId}")
    @Operation(
        operationId = "UMC-PRODUCT-MEMBER-004",
        summary = "UMC PRODUCT 멤버 활동 기간 수정"
    )
    public void updateActivityPeriod(
        @PathVariable Long memberId,
        @PathVariable Long periodId,
        @CurrentMember MemberPrincipal currentMember,
        @RequestBody @Valid UpdateUmcProductMemberActivityPeriodRequest request
    ) {
        manageUmcProductMemberUseCase.updateActivityPeriod(
            request.toCommand(memberId, periodId, currentMemberId(currentMember))
        );
    }

    @DeleteMapping("/{memberId}/activity-periods/{periodId}")
    @Operation(
        operationId = "UMC-PRODUCT-MEMBER-005",
        summary = "UMC PRODUCT 멤버 활동 기간 삭제"
    )
    public void deleteActivityPeriod(
        @PathVariable Long memberId,
        @PathVariable Long periodId,
        @CurrentMember MemberPrincipal currentMember
    ) {
        manageUmcProductMemberUseCase.deleteActivityPeriod(
            memberId, periodId, currentMemberId(currentMember)
        );
    }

    @PostMapping("/{memberId}/chapter-memberships")
    @Operation(operationId = "UMC-PRODUCT-MEMBER-006", summary = "UMC PRODUCT 멤버 Chapter 소속 생성")
    public Long createChapterMembership(
        @PathVariable Long memberId,
        @CurrentMember MemberPrincipal currentMember,
        @RequestBody @Valid CreateUmcProductChapterMembershipRequest request
    ) {
        return manageUmcProductMemberUseCase.createChapterMembership(
            request.toCommand(memberId, currentMemberId(currentMember))
        );
    }

    @PatchMapping("/{memberId}/chapter-memberships/{chapterMembershipId}")
    @Operation(operationId = "UMC-PRODUCT-MEMBER-007", summary = "UMC PRODUCT 멤버 Chapter 소속 수정")
    public void updateChapterMembership(
        @PathVariable Long memberId,
        @PathVariable Long chapterMembershipId,
        @CurrentMember MemberPrincipal currentMember,
        @RequestBody @Valid UpdateUmcProductChapterMembershipRequest request
    ) {
        manageUmcProductMemberUseCase.updateChapterMembership(
            request.toCommand(memberId, chapterMembershipId, currentMemberId(currentMember))
        );
    }

    @DeleteMapping("/{memberId}/chapter-memberships/{chapterMembershipId}")
    @Operation(operationId = "UMC-PRODUCT-MEMBER-008", summary = "UMC PRODUCT 멤버 Chapter 소속 삭제")
    public void deleteChapterMembership(
        @PathVariable Long memberId,
        @PathVariable Long chapterMembershipId,
        @CurrentMember MemberPrincipal currentMember
    ) {
        manageUmcProductMemberUseCase.deleteChapterMembership(
            memberId, chapterMembershipId, currentMemberId(currentMember)
        );
    }

    @PostMapping("/{memberId}/product-leaderships")
    @Operation(operationId = "UMC-PRODUCT-MEMBER-009", summary = "UMC PRODUCT Leadership 생성")
    public Long createLeadership(
        @PathVariable Long memberId,
        @CurrentMember MemberPrincipal currentMember,
        @RequestBody @Valid CreateUmcProductLeadershipRequest request
    ) {
        return manageUmcProductMemberUseCase.createLeadership(
            request.toCommand(memberId, currentMemberId(currentMember))
        );
    }

    @PatchMapping("/{memberId}/product-leaderships/{leadershipId}")
    @Operation(operationId = "UMC-PRODUCT-MEMBER-010", summary = "UMC PRODUCT Leadership 수정")
    public void updateLeadership(
        @PathVariable Long memberId,
        @PathVariable Long leadershipId,
        @CurrentMember MemberPrincipal currentMember,
        @RequestBody @Valid UpdateUmcProductLeadershipRequest request
    ) {
        manageUmcProductMemberUseCase.updateLeadership(
            request.toCommand(memberId, leadershipId, currentMemberId(currentMember))
        );
    }

    @DeleteMapping("/{memberId}/product-leaderships/{leadershipId}")
    @Operation(operationId = "UMC-PRODUCT-MEMBER-011", summary = "UMC PRODUCT Leadership 삭제")
    public void deleteLeadership(
        @PathVariable Long memberId,
        @PathVariable Long leadershipId,
        @CurrentMember MemberPrincipal currentMember
    ) {
        manageUmcProductMemberUseCase.deleteLeadership(
            memberId, leadershipId, currentMemberId(currentMember)
        );
    }

    @DeleteMapping("/{memberId}")
    @Operation(
        operationId = "UMC-PRODUCT-MEMBER-012",
        summary = "UMC PRODUCT 멤버 삭제",
        description = "연결된 Department 참여, Chapter 소속, Leadership, 활동 기간을 순서대로 삭제한 뒤 멤버를 삭제합니다."
    )
    public void delete(
        @PathVariable Long memberId,
        @CurrentMember MemberPrincipal currentMember
    ) {
        manageUmcProductMemberUseCase.delete(memberId, currentMemberId(currentMember));
    }

    private Long currentMemberId(MemberPrincipal currentMember) {
        if (currentMember == null) {
            throw new AccessDeniedException("인증이 필요합니다.");
        }
        return currentMember.getMemberId();
    }
}
