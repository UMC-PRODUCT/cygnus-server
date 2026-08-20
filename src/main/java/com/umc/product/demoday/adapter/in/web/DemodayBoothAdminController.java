package com.umc.product.demoday.adapter.in.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.demoday.adapter.in.web.dto.request.RegisterDemodayBoothBatchRequest;
import com.umc.product.demoday.adapter.in.web.dto.request.RegisterDemodayBoothRequest;
import com.umc.product.demoday.adapter.in.web.dto.response.CreateDemodayStampQrResponse;
import com.umc.product.demoday.adapter.in.web.dto.response.DemodayAdminBoothListResponse;
import com.umc.product.demoday.adapter.in.web.dto.response.RegisterDemodayBoothBatchResponse;
import com.umc.product.demoday.adapter.in.web.dto.response.RegisterDemodayBoothResponse;
import com.umc.product.demoday.application.port.in.command.CreateDemodayStampUseCase;
import com.umc.product.demoday.application.port.in.command.RegisterDemodayBoothUseCase;
import com.umc.product.demoday.application.port.in.command.dto.CreateStampCredentialCommand;
import com.umc.product.demoday.application.port.in.command.dto.StampCredentialInfo;
import com.umc.product.demoday.application.port.in.query.ListDemodayAdminBoothUseCase;
import com.umc.product.demoday.application.port.in.query.dto.DemodayAdminBoothListInfo;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(
    name = "데모데이 부스 관리자",
    description = "해당 기수 총괄단 또는 SUPER_ADMIN이 데모데이 투표 대상 부스를 관리하는 API"
)
@RestController
@RequestMapping("/api/v1/demoday/admin/polls/{pollId}/booths")
@RequiredArgsConstructor
public class DemodayBoothAdminController {

    private final CreateDemodayStampUseCase createDemodayStampUseCase;
    private final RegisterDemodayBoothUseCase registerDemodayBoothUseCase;
    private final ListDemodayAdminBoothUseCase listDemodayAdminBoothUseCase;

    @Operation(
        operationId = "registerDemodayBooth",
        summary = "데모데이 부스 등록",
        description = """
            투표 행사에 부스를 하나 등록합니다.

            요청자는 투표가 속한 기수의 총괄단이거나 SUPER_ADMIN이어야 합니다.
            `boothCode`는 행사에서 사용하는 1 이상의 정수이며 같은 투표 안에서 중복될 수 없습니다.
            이미 사용 중인 코드를 등록하면 409(DEMODAY-0204)를 반환합니다.
            등록 경로는 두 가지이며 `projectId`와 `displayName` 중 정확히 하나만 채워 구분합니다.
            UPMS에 등록된 프로젝트는 `projectId`로, 외부 참가팀은 `displayName`으로 등록합니다.
            둘 다 채우거나 둘 다 비우면 어느 경로인지 정할 수 없어 400(DEMODAY-0200)으로 거부합니다.

            투표 상태가 OPEN이 되면 더 이상 부스를 추가할 수 없고 409(DEMODAY-0105)를 반환합니다.
            투표가 시작된 뒤 부스가 늘어나면 먼저 투표한 사람과 나중에 투표한 사람의 선택지가 달라져
            순위를 비교할 근거가 사라지기 때문입니다. 부스 등록은 투표를 OPEN하기 전에 마쳐야 합니다.
            """
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterDemodayBoothResponse registerBooth(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal memberPrincipal,
        @Parameter(description = "부스를 등록할 데모데이 투표 ID", required = true, example = "1")
        @PathVariable("pollId") Long pollId,
        @Valid @RequestBody RegisterDemodayBoothRequest request) {

        Long boothId = registerDemodayBoothUseCase.register(
            request.toCommand(pollId, memberPrincipal.getMemberId()));

        return RegisterDemodayBoothResponse.from(boothId);
    }

    @Operation(
        operationId = "registerDemodayBoothsInBatch",
        summary = "데모데이 부스 일괄 등록 (시스템 관리자 전용)",
        description = """
            **이 API는 시스템 관리자(SUPER_ADMIN)를 위한 API입니다.**
            해당 기수의 총괄단은 호출할 수 없으며, 총괄단은 단건 등록 API를 사용하세요.

            여러 부스를 한 번의 요청으로 등록합니다. 이번 기수에는 관리자 화면이 없어 운영자가 API를 직접
            호출하므로, 행사 전 준비 단계에서 부스를 한 번에 넣기 위한 경로입니다.

            항목마다 `boothCode`를 채우고 `projectId`와 `displayName` 중 정확히 하나만 채우는 규칙은 단건 등록과 같습니다.
            **한 항목이라도 규칙을 어기면 전체가 저장되지 않습니다.** 부분 성공을 허용하면 무엇이 들어갔는지
            운영자가 목록을 다시 확인해야 하는데, 그 비용이 요청 전체를 고쳐 다시 보내는 비용보다 큽니다.
            기존 부스 또는 요청 안의 다른 항목과 코드가 중복되면 409(DEMODAY-0204)를 반환합니다.

            응답의 `boothIds`는 요청한 부스 목록과 순서가 같습니다.
            투표 상태가 OPEN이면 단건 등록과 동일하게 409(DEMODAY-0105)를 반환합니다.
            """
    )
    @PostMapping("/batch")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterDemodayBoothBatchResponse registerBoothsInBatch(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal memberPrincipal,
        @Parameter(description = "부스를 등록할 데모데이 투표 ID", required = true, example = "1")
        @PathVariable("pollId") Long pollId,
        @Valid @RequestBody RegisterDemodayBoothBatchRequest request) {

        List<Long> boothIds = registerDemodayBoothUseCase.registerAll(
            request.toCommand(pollId, memberPrincipal.getMemberId()));

        return RegisterDemodayBoothBatchResponse.from(boothIds);
    }

    @Operation(
        operationId = "createOrReissueDemodayBoothStampQr",
        summary = "데모데이 부스 스탬프 QR 생성 또는 재발급",
        description = """
            해당 부스의 스탬프 QR을 생성합니다. 이미 생성된 QR이 있으면 새 QR로 교체되고,
            이전 QR은 즉시 무효가 됩니다.

            QR 값은 스탬프 적립 화면으로 이동하는 URL입니다. URL fragment에 포함된 credential은
            이 응답에서만 확인할 수 있으므로 운영자는 안전하게 전달·보관해야 합니다.
            """
    )
    @PostMapping("/{boothId}/stamp-qr")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateDemodayStampQrResponse createOrReissueStampQr(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal memberPrincipal,
        @Parameter(description = "부스가 속한 데모데이 투표 ID", required = true, example = "1")
        @PathVariable("pollId") Long pollId,
        @Parameter(description = "스탬프 QR을 생성하거나 재발급할 부스 ID", required = true, example = "1")
        @PathVariable("boothId") Long boothId) {

        StampCredentialInfo stampCredentialInfo = createDemodayStampUseCase.create(
            memberPrincipal.getMemberId(), new CreateStampCredentialCommand(pollId, boothId));

        return CreateDemodayStampQrResponse.from(stampCredentialInfo);
    }

    @Operation(
        operationId = "listDemodayAdminBooths",
        summary = "데모데이 부스 등록 현황 조회",
        description = """
            투표에 등록된 부스 목록을 부스 코드 오름차순으로 조회합니다.

            요청자는 투표가 속한 기수의 총괄단이거나 SUPER_ADMIN이어야 합니다.
            등록 직후 운영자가 확인해야 하는 값을 함께 내려줍니다.
            `boothCount`로 등록 개수를, `boothAddable`로 아직 부스를 더 넣을 수 있는지를 확인하세요.

            참여자용 목록(`GET /api/v1/demoday/polls/{pollId}/booths`)과 달리 투표의 운영 상태를 포함합니다.
            """
    )
    @GetMapping
    public DemodayAdminBoothListResponse listBooths(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal memberPrincipal,
        @Parameter(description = "부스를 조회할 데모데이 투표 ID", required = true, example = "1")
        @PathVariable("pollId") Long pollId) {

        DemodayAdminBoothListInfo boothListInfo =
            listDemodayAdminBoothUseCase.listBooths(pollId, memberPrincipal.getMemberId());

        return DemodayAdminBoothListResponse.from(boothListInfo);
    }
}
