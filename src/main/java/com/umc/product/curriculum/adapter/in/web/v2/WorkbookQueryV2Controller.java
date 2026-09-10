package com.umc.product.curriculum.adapter.in.web.v2;

import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.curriculum.adapter.in.web.v2.dto.request.GetBestWorkbooksRequest;
import com.umc.product.curriculum.adapter.in.web.v2.dto.request.GetStudyMemberSubmissionsRequest;
import com.umc.product.curriculum.adapter.in.web.v2.dto.response.BestWorkbookResponse;
import com.umc.product.curriculum.adapter.in.web.v2.dto.response.ChallengerWorkbookResponse;
import com.umc.product.curriculum.adapter.in.web.v2.dto.response.OriginalWorkbookResponse;
import com.umc.product.curriculum.adapter.in.web.v2.dto.response.StudyMemberSubmissionResponse;
import com.umc.product.curriculum.application.port.in.query.GetChallengerWorkbookUseCase;
import com.umc.product.curriculum.application.port.in.query.GetOriginalWorkbookUseCase;
import com.umc.product.curriculum.application.port.in.query.GetStudyMemberSubmissionUseCase;
import com.umc.product.curriculum.application.port.in.query.GetWeeklyBestWorkbookUseCase;
import com.umc.product.curriculum.application.port.in.query.dto.StudyMemberSubmissionInfo;
import com.umc.product.curriculum.application.port.in.query.dto.WeeklyBestWorkbookPageInfo;
import com.umc.product.global.response.CursorResponse;
import com.umc.product.global.response.PageResponse;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v2/curriculums")
@RequiredArgsConstructor
@Tag(name = "Curriculum V2 | 워크북 Query", description = "원본 워크북, 챌린저 워크북, 베스트 워크북을 조회합니다.")
public class WorkbookQueryV2Controller {

    private final GetStudyMemberSubmissionUseCase getStudyMemberSubmissionUseCase;
    private final GetOriginalWorkbookUseCase getOriginalWorkbookUseCase;
    private final GetChallengerWorkbookUseCase getChallengerWorkbookUseCase;
    private final GetWeeklyBestWorkbookUseCase getWeeklyBestWorkbookUseCase;

    @Operation(
        operationId = "WORKBOOK-104",
        summary = "스터디원 제출 현황 조회",
        description = """
            요청자가 관리할 수 있는 스터디 그룹의 스터디원들과, 각자의 주차별 워크북 제출 현황을 조회합니다.
            파트장은 본인이 맡은 그룹을, 학교 회장/부회장은 해당 학교 멤버가 속한 그룹을 볼 수 있습니다.

            행 단위는 *스터디원* 이며, 주차는 각 행의 `weeks` 배열에 담깁니다.
            아직 워크북을 배포받지 않은 인원도 결과에 포함되며, 이 경우 `challengerWorkbookId` 가 null 이고
            `status` 는 `NOT_SUBMITTED` 입니다.

            Query Param 으로 아래 필터를 적용할 수 있으며 모두 선택 사항입니다.
            - 스터디 그룹 ID (생략 시 조회 가능한 전체 그룹)
            - (다중 선택 가능) 주차 (생략 시 전체 주차)

            커서 페이지네이션이며 cursor 는 직전 페이지 마지막 `studyGroupMemberId` 입니다. size 는 최대 100, 기본 20 입니다.
            """
    )
    @GetMapping("/workbook-submissions")
    public CursorResponse<StudyMemberSubmissionResponse> getStudyMemberSubmissions(
        @ParameterObject @Valid GetStudyMemberSubmissionsRequest request,
        @CurrentMember MemberPrincipal memberPrincipal
    ) {
        List<StudyMemberSubmissionInfo> content = getStudyMemberSubmissionUseCase.getStudyMemberSubmissions(
            request.toQuery(memberPrincipal.getMemberId())
        );

        return CursorResponse.of(
            content,
            request.resolvedSize(),
            StudyMemberSubmissionInfo::studyGroupMemberId,
            StudyMemberSubmissionResponse::from
        );
    }

    @Operation(
        operationId = "WORKBOOK-105",
        summary = "제출 현황 조회 가능 주차 목록",
        description = """
            제출 현황(WORKBOOK-104) 화면의 주차 필터에 띄울 수 있는 주차 번호 목록을 조회합니다.

            활성 기수의 파트별 커리큘럼에 정의된 주차(weekNo)의 union 이며, distinct 오름차순으로 반환합니다.
            부록(extra) 주차와 아직 배포되지 않은 주차도 포함됩니다 — WORKBOOK-104 행의 `weeks` 와 기준이 같습니다.

            - `studyGroupId` 지정 시 그 그룹 파트의 주차만 반환합니다. 존재하지 않는 그룹이면 404 입니다.
            - 생략 시 활성 기수 전체 파트 기준입니다.
            - 커리큘럼이 없으면 빈 배열입니다.
            """
    )
    @GetMapping("/workbook-submissions/weeks")
    public List<Long> getSubmissionWeeks(
        @RequestParam(required = false) Long studyGroupId,
        @RequestParam(required = false) Long gisuId
    ) {
        return gisuId == null ? getStudyMemberSubmissionUseCase.getAvailableWeekNos(studyGroupId)
            : getStudyMemberSubmissionUseCase.getAvailableWeekNos(studyGroupId, gisuId);
    }

    @Operation(
        operationId = "WORKBOOK-101",
        summary = "OriginalWorkbook 상세 조회",
        description = """
            원본 워크북을 조회합니다. 원본 워크북의 파트와 기수에 해당 파트의 스터디 그룹에 속해 있어야 합니다.
            """
    )
    @GetMapping("/original-workbooks/{originalWorkbookId}")
    public OriginalWorkbookResponse getOriginalWorkbook(
        @PathVariable Long originalWorkbookId,
        @CurrentMember MemberPrincipal memberPrincipal
    ) {
        return OriginalWorkbookResponse.from(
            getOriginalWorkbookUseCase.getById(originalWorkbookId, memberPrincipal.getMemberId())
        );
    }

    @Operation(
        operationId = "WORKBOOK-102",
        summary = "ChallengerWorkbook 상세 조회",
        description = """
            챌린저 워크북과 그에 연관된 미션 제출물 및 피드백을 조회합니다.

            꼭 본인이 아니더라도, 같은 기수에 활동한 챌린저 전원은 확인할 수 있습니다.
            기획단 결정사항에 따라서 미션 제출 내역 및 피드백 내용까지 볼 수 있습니다.
            """
    )
    @GetMapping("/challenger-workbooks/{challengerWorkbookId}")
    public ChallengerWorkbookResponse getChallengerWorkbook(
        @PathVariable Long challengerWorkbookId,
        @CurrentMember MemberPrincipal memberPrincipal
    ) {
        return ChallengerWorkbookResponse.from(
            getChallengerWorkbookUseCase.getById(challengerWorkbookId, memberPrincipal.getMemberId())
        );
    }

    @Operation(
        operationId = "WORKBOOK-103",
        summary = "베스트 워크북 조회",
        description = """
            베스트 워크북을 조회합니다. 0부터 시작하는 page 기반 Pagination이 적용되었습니다.

            Query Param으로 아래와 같은 필터를 적용할 수 있으며,
            다중 선택을 지원하며, 제공된 값들에 대한 카르테시안 곱으로 결과를 제공합니다.

            - 기수 ID
            - (다중 선택 가능) 학교 ID
            - (다중 선택 가능) 파트 (제공되지 않은 경우 전체 파트)
            - (다중 선택 가능) 주차
            - (다중 선택 가능) 스터디 그룹 ID

            size는 최대 100이며, 기수 ID를 포함한 모든 필터는 선택 사항입니다.
            """
    )
    @GetMapping("/weekly-best-workbooks")
    public PageResponse<BestWorkbookResponse> getBestWorkbooks(
        @ParameterObject @Valid GetBestWorkbooksRequest request
    ) {
        WeeklyBestWorkbookPageInfo page = getWeeklyBestWorkbookUseCase.searchBestWorkbooks(request.toQuery());
        return new PageResponse<>(
            page.content().stream().map(BestWorkbookResponse::from).toList(),
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages(),
            page.hasNext(),
            page.hasPrevious()
        );
    }
}
