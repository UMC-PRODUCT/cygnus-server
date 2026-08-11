package com.umc.product.inhouse.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.inhouse.adapter.in.web.dto.request.CreateUmcProductSquadParticipantRequest;
import com.umc.product.inhouse.adapter.in.web.dto.request.CreateUmcProductSquadRequest;
import com.umc.product.inhouse.adapter.in.web.dto.request.UpdateUmcProductSquadParticipantRequest;
import com.umc.product.inhouse.adapter.in.web.dto.request.UpdateUmcProductSquadRequest;
import com.umc.product.inhouse.application.port.in.command.ManageUmcProductSquadUseCase;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/umc-product/squads")
@RequiredArgsConstructor
@Tag(name = "Organization | UMC PRODUCT 스쿼드 Command", description = "UMC PRODUCT 스쿼드와 참여자를 관리합니다.")
public class UmcProductSquadCommandController {

    private final ManageUmcProductSquadUseCase manageUmcProductSquadUseCase;

    @PostMapping
    @Operation(
        operationId = "UMC-PRODUCT-SQUAD-001",
        summary = "UMC PRODUCT 스쿼드 생성",
        description = "코드, 이름, 달력 날짜 기준 운영 기간, 정렬 순서, 활성 여부를 저장합니다."
    )
    public Long create(
        @CurrentMember MemberPrincipal currentMember,
        @RequestBody @Valid CreateUmcProductSquadRequest request
    ) {
        return manageUmcProductSquadUseCase.create(request.toCommand(currentMemberId(currentMember)));
    }

    @PatchMapping("/{squadId}")
    @Operation(
        operationId = "UMC-PRODUCT-SQUAD-002",
        summary = "UMC PRODUCT 스쿼드 수정",
        description = "기존 참여 이력이 변경 후 Squad 기간을 벗어나면 수정을 거부합니다."
    )
    public void update(
        @PathVariable Long squadId,
        @CurrentMember MemberPrincipal currentMember,
        @RequestBody @Valid UpdateUmcProductSquadRequest request
    ) {
        manageUmcProductSquadUseCase.update(request.toCommand(squadId, currentMemberId(currentMember)));
    }

    @DeleteMapping("/{squadId}")
    @Operation(
        operationId = "UMC-PRODUCT-SQUAD-003",
        summary = "UMC PRODUCT 스쿼드 삭제",
        description = "참여 이력이 있는 Squad는 삭제할 수 없습니다."
    )
    public void delete(
        @PathVariable Long squadId,
        @CurrentMember MemberPrincipal currentMember
    ) {
        manageUmcProductSquadUseCase.delete(squadId, currentMemberId(currentMember));
    }

    @PostMapping("/{squadId}/participants")
    @Operation(
        operationId = "UMC-PRODUCT-SQUAD-004",
        summary = "UMC PRODUCT Squad 참여 생성"
    )
    public Long createParticipant(
        @PathVariable Long squadId,
        @CurrentMember MemberPrincipal currentMember,
        @RequestBody @Valid CreateUmcProductSquadParticipantRequest request
    ) {
        return manageUmcProductSquadUseCase.createParticipant(
            request.toCommand(squadId, currentMemberId(currentMember))
        );
    }

    @Hidden
    @PutMapping("/{squadId}/participants")
    public void rejectRemovedParticipantReplacement() {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    @PatchMapping("/{squadId}/participants/{participantId}")
    @Operation(operationId = "UMC-PRODUCT-SQUAD-005", summary = "UMC PRODUCT Squad 참여 수정")
    public void updateParticipant(
        @PathVariable Long squadId,
        @PathVariable Long participantId,
        @CurrentMember MemberPrincipal currentMember,
        @RequestBody @Valid UpdateUmcProductSquadParticipantRequest request
    ) {
        manageUmcProductSquadUseCase.updateParticipant(
            request.toCommand(squadId, participantId, currentMemberId(currentMember))
        );
    }

    @DeleteMapping("/{squadId}/participants/{participantId}")
    @Operation(operationId = "UMC-PRODUCT-SQUAD-006", summary = "UMC PRODUCT Squad 참여 삭제")
    public void deleteParticipant(
        @PathVariable Long squadId,
        @PathVariable Long participantId,
        @CurrentMember MemberPrincipal currentMember
    ) {
        manageUmcProductSquadUseCase.deleteParticipant(
            squadId, participantId, currentMemberId(currentMember)
        );
    }

    private Long currentMemberId(MemberPrincipal currentMember) {
        if (currentMember == null) {
            throw new AccessDeniedException("인증이 필요합니다.");
        }
        return currentMember.getMemberId();
    }
}
