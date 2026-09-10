package com.umc.product.challenger.adapter.in.web;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.authorization.adapter.in.aspect.CheckAccess;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.challenger.adapter.in.web.assembler.ChallengerRecordResponseAssembler;
import com.umc.product.challenger.adapter.in.web.dto.request.AddChallengerRecordToMemberRequest;
import com.umc.product.challenger.adapter.in.web.dto.request.CreateChallengerRecordRequest;
import com.umc.product.challenger.adapter.in.web.dto.response.ChallengerRecordResponse;
import com.umc.product.challenger.application.port.in.command.ManageChallengerRecordUseCase;
import com.umc.product.challenger.application.port.in.command.dto.ConsumeChallengerRecordCommand;
import com.umc.product.challenger.application.port.in.command.dto.CreateChallengerRecordCommand;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/challenger-record")
@RequiredArgsConstructor
@Validated
@Tag(name = "Challenger | 챌린저 기록", description = "챌린저 활동 기록과 등록 코드를 관리합니다.")
public class ChallengerRecordController {

    private final ChallengerRecordResponseAssembler assembler;
    private final ManageChallengerRecordUseCase manageChallengerRecordUseCase;

    // 코드를 이용해서 Member에 챌린저 기록을 추가하는 API
    @Operation(operationId = "CHALLENGER-RECORD-001", summary = "6자리 코드를 이용해서 회원(계정)에 챌린저 기록 추가",
        description = """
            각 챌린저 활동 기록에 대해서 발급된 6자리 코드를 입력하여,
            현재 로그인한 계정에 챌린저 기록 및 권한을 추가하는 기능입니다.

            각 코드는 1회만 사용 가능하며, 어떤 계정에, 언제 사용되었는지 기록됩니다.
            """)
    @PostMapping("member")
//    @WebhookAlarm(
//        title = "'챌린저 기록이 추가되었어요!'",
//        content = "'회원 ID: ' + #memberPrincipal.getMemberId() + '\n챌린저 코드: ' + #request.code"
//    )
    public void addChallengerRecordToMember(
        @CurrentMember MemberPrincipal memberPrincipal,
        @Valid @RequestBody AddChallengerRecordToMemberRequest request) {

        manageChallengerRecordUseCase.consumeCode(
            ConsumeChallengerRecordCommand.builder()
                .targetMemberId(memberPrincipal.getMemberId())
                .code(request.code())
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

    // 코드를 생성하는 API
    @CheckAccess(
        resourceType = ResourceType.CHALLENGER_RECORD,
        permission = PermissionType.WRITE
    )
    @Operation(operationId = "CHALLENGER-RECORD-002", summary = "챌린저 가입 및 기록용 코드 생성",
        description = """
            중앙운영사무국 총괄단이 기수의 학습 유형에 맞는 part 또는 기본 tracks 목록으로 6자리 코드를 발급합니다.
            TRACK 기수는 기본 트랙을 복수로 지정할 수 있고, 비수강 운영진은 역할과 tracks: []를 사용합니다.
            기존 단일 track 입력도 지원하지만 tracks와 동시에 지정할 수 없습니다.
            이름과 학교가 일치하는 회원에게 수강 트랙과 challengerRoleType 역할을 한 코드로 부여합니다.
            운영진 코드의 part는 수강이 아닌 담당 파트이며, 수강하지 않는 중앙 운영진만 chapterId를 생략할 수 있습니다.
            PART 기수의 기존 코드 발급과 사용 방식은 유지합니다.
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
                .track(request.track())
                .tracks(request.tracks())
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

            중앙운영사무국 총괄단이 단건 발급과 동일한 기수·학교·지부 및 학습 유형 검증으로 코드를 일괄 발급합니다.
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
                    .track(req.track())
                    .tracks(req.tracks())
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

}
