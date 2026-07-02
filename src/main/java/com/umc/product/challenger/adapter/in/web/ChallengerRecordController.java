package com.umc.product.challenger.adapter.in.web;

import com.umc.product.authentication.domain.EmailVerificationPurpose;
import com.umc.product.authorization.adapter.in.aspect.CheckAccess;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.challenger.adapter.in.web.assembler.ChallengerRecordResponseAssembler;
import com.umc.product.challenger.adapter.in.web.dto.request.AddChallengerRecordToMemberRequest;
import com.umc.product.challenger.adapter.in.web.dto.request.CreateChallengerRecordRequest;
import com.umc.product.challenger.adapter.in.web.dto.request.SearchChallengerRecordRequest;
import com.umc.product.challenger.adapter.in.web.dto.response.ChallengerRecordResponse;
import com.umc.product.challenger.adapter.in.web.dto.response.ChallengerRecordSummaryResponse;
import com.umc.product.challenger.adapter.in.web.dto.response.UnusedChallengerRecordStatisticsResponse;
import com.umc.product.challenger.application.port.in.command.ManageChallengerRecordUseCase;
import com.umc.product.challenger.application.port.in.command.dto.ConsumeChallengerRecordCommand;
import com.umc.product.challenger.application.port.in.command.dto.CreateChallengerRecordCommand;
import com.umc.product.global.response.PageResponse;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/challenger-record")
@RequiredArgsConstructor
@Validated
@Tag(name = "Challenger | 챌린저 기록", description = "챌린저 활동 기록과 등록 코드를 관리합니다.")
public class ChallengerRecordController {

    private final ChallengerRecordResponseAssembler assembler;
    private final ManageChallengerRecordUseCase manageChallengerRecordUseCase;
    private final JwtTokenProvider jwtTokenProvider;

    // 코드를 이용해서 Member에 챌린저 기록을 추가하는 API
    @Operation(operationId = "CHALLENGER-RECORD-001", summary = "6자리 코드를 이용해서 회원(계정)에 챌린저 기록 추가",
        description = """
            각 챌린저 활동 기록에 대해서 발급된 6자리 코드를 입력하여,
            현재 로그인한 계정에 챌린저 기록 및 권한을 추가하는 기능입니다.

            CHALLENGER_REGISTER 용도로 발급된 emailVerificationToken 으로 이메일 소유를 검증하며,
            검증된 이메일이 로그인한 계정의 이메일과 일치해야 합니다. (운영진 코드는 이메일 검증을 생략합니다.)

            각 코드는 1회만 생성 가능하며, 어떤 계정에, 언제 사용되었는지 기록됩니다.
            """)
    @PostMapping("member")
//    @WebhookAlarm(
//        title = "'챌린저 기록이 추가되었어요!'",
//        content = "'회원 ID: ' + #memberPrincipal.getMemberId() + '\n챌린저 코드: ' + #request.code"
//    )
    public void addChallengerRecordToMember(
        @CurrentMember MemberPrincipal memberPrincipal,
        @Valid @RequestBody AddChallengerRecordToMemberRequest request) {

        String verifiedEmail = jwtTokenProvider.parseEmailVerificationToken(
            request.emailVerificationToken(),
            EmailVerificationPurpose.CHALLENGER_REGISTER
        );

        manageChallengerRecordUseCase.consumeCode(
            ConsumeChallengerRecordCommand.builder()
                .targetMemberId(memberPrincipal.getMemberId())
                .code(request.code())
                .verifiedEmail(verifiedEmail)
                .build()
        );
    }

    @CheckAccess(
        resourceType = ResourceType.CHALLENGER_RECORD,
        permission = PermissionType.READ
    )
    @GetMapping("code/{code}")
    @Operation(operationId = "CHALLENGER-RECORD-101", summary = "코드로 ChallengerRecord 조회")
    public ChallengerRecordResponse getChallengerRecordByCode(
        @PathVariable String code
    ) {
        return assembler.from(code);
    }

    @CheckAccess(
        resourceType = ResourceType.CHALLENGER_RECORD,
        permission = PermissionType.READ
    )
    @GetMapping("id/{id}")
    @Operation(operationId = "CHALLENGER-RECORD-102", summary = "ID로 ChallengerRecord 조회")
    public ChallengerRecordResponse getChallengerRecordById(
        @PathVariable Long id
    ) {
        return assembler.from(id);
    }

    @CheckAccess(
        resourceType = ResourceType.CHALLENGER_RECORD,
        permission = PermissionType.READ
    )
    @GetMapping
    @Operation(operationId = "CHALLENGER-RECORD-103", summary = "조건별 ChallengerRecord 코드 목록 조회",
        description = """
            기수/학교/파트/역할 조건으로 발급된 챌린저 기록 코드를 페이지 조회합니다.
            모든 조건은 선택이며 자유롭게 조합할 수 있습니다. (조건 미지정 시 전체 조회)
            """)
    public PageResponse<ChallengerRecordSummaryResponse> searchChallengerRecords(
        @ParameterObject @Valid SearchChallengerRecordRequest request,
        @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
        Pageable pageable
    ) {
        return assembler.search(request.toQuery(pageable));
    }

    @CheckAccess(
        resourceType = ResourceType.CHALLENGER_RECORD,
        permission = PermissionType.READ
    )
    @GetMapping("statistics/unused")
    @Operation(operationId = "CHALLENGER-RECORD-104", summary = "기수×학교별 미사용 ChallengerRecord 코드 개수 집계 조회",
        description = """
            아직 사용되지 않은(isUsed=false) 챌린저 기록 코드 개수를 기수×학교 단위로 그룹 집계하여 반환합니다.
            미사용 코드가 0개인 (기수, 학교) 조합은 결과에 포함되지 않으며, 기수 내림차순·학교 오름차순으로 정렬됩니다.
            전체 합계(totalUnusedCount)도 함께 제공합니다.
            """)
    public UnusedChallengerRecordStatisticsResponse getUnusedChallengerRecordStatistics() {
        return assembler.unusedStatistics();
    }

    // 코드를 생성하는 API
    @CheckAccess(
        resourceType = ResourceType.CHALLENGER_RECORD,
        permission = PermissionType.WRITE
    )
    @Operation(operationId = "CHALLENGER-RECORD-002", summary = "과거 챌린저 기록용 코드 생성",
        description = """
            중앙운영사무국 총괄단만 사용 가능한 기능입니다. 9기 이전 기수의 챌린저 기록을 업로드하고,
            각 기록을 모든 회원이 추가할 수 있도록 6자리 코드를 생성하여 발급합니다.
            """)
    @PostMapping
    public ChallengerRecordResponse createChallengerRecord(
        @CurrentMember MemberPrincipal memberPrincipal,
        @Valid @RequestBody CreateChallengerRecordRequest request
    ) {
        // TODO: SUPER_ADMIN 만 가능하도록 권한 설정

        Long id = manageChallengerRecordUseCase.create(
            CreateChallengerRecordCommand.builder()
                .part(request.part())
                .creatorMemberId(memberPrincipal.getMemberId())
                .gisuId(request.gisuId())
                .chapterId(request.chapterId())
                .schoolId(request.schoolId())
                .memberName(request.memberName())
                .challengerRoleType(request.challengerRoleType())
                .build()
        );

        return assembler.from(id);
    }

    @Operation(operationId = "CHALLENGER-RECORD-003", summary = "챌린저 기록용 코드 일괄 추가",
        description = """
            Response는 생성된 챌린저 기록의 ID 리스트입니다. (성능 상 이슈로 각각에 대해서는 id 및 code로 조회하는 API 이용)

            중앙운영사무국 총괄단만 사용 가능한 기능입니다. 9기 이전 기수의 챌린저 기록을 업로드하고,
            각 기록을 모든 회원이 추가할 수 있도록 6자리 코드를 생성하여 발급합니다.
            """)
    @PostMapping("bulk")
    @CheckAccess(
        resourceType = ResourceType.CHALLENGER_RECORD,
        permission = PermissionType.WRITE
    )
    public List<Long> createChallengerRecordBulk(
        @CurrentMember MemberPrincipal memberPrincipal,
        @Valid @RequestBody List<@Valid CreateChallengerRecordRequest> request
    ) {
        List<Long> ids = manageChallengerRecordUseCase.createBulk(
            request.stream()
                .map(req -> CreateChallengerRecordCommand.builder()
                    .part(req.part())
                    .creatorMemberId(memberPrincipal.getMemberId())
                    .gisuId(req.gisuId())
                    .chapterId(req.chapterId())
                    .schoolId(req.schoolId())
                    .memberName(req.memberName())
                    .challengerRoleType(req.challengerRoleType())
                    .build())
                .toList()
        );

        return ids;
    }

    // 발급된 코드를 단건 삭제하는 API
    @CheckAccess(
        resourceType = ResourceType.CHALLENGER_RECORD,
        permission = PermissionType.DELETE
    )
    @Operation(operationId = "CHALLENGER-RECORD-004", summary = "ChallengerRecord 코드 단건 삭제",
        description = """
            발급된 챌린저 기록 코드를 단건 물리 삭제합니다.
            중앙운영사무국 총괄단 등 삭제 권한을 가진 운영진만 사용할 수 있습니다.
            """)
    @DeleteMapping("id/{id}")
    public void deleteChallengerRecord(
        @PathVariable Long id
    ) {
        manageChallengerRecordUseCase.delete(id);
    }

}
