package com.umc.product.organization.adapter.in.web.swagger;

import com.umc.product.global.response.CursorResponse;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.organization.adapter.in.web.dto.response.studygroup.StudyGroupMemberResponse;
import com.umc.product.organization.adapter.in.web.dto.response.studygroup.StudyGroupNameResponse;
import com.umc.product.organization.adapter.in.web.dto.response.studygroup.StudyGroupResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 스터디 그룹 조회 API
 *
 * <p>목록 조회는 gisuId로 준비 기수를 선택할 수 있으며, 생략하면 활성 기수를 사용합니다.</p>
 *
 * <h2>API 흐름</h2>
 * <pre>
 * GET /
 *   → 내 학교/파트 기반 스터디 그룹 목록 (무한스크롤)
 *   → 그룹 선택
 *
 * GET /{groupId}
 *   → 스터디 그룹 상세 + 멤버 목록
 * </pre>
 */
@Tag(name = "Organization | 스터디 그룹 Query", description = "")
public interface StudyGroupQueryControllerApi {

    @Operation(operationId = "STUDY-GROUP-101", summary = "내가 관리하는 스터디 그룹 목록 조회", description = "로그인한 유저의 학교/파트 기반으로 스터디 그룹 목록을 조회합니다. cursor 기반 무한스크롤.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    CursorResponse<StudyGroupResponse> getStudyGroups(
        @Parameter(hidden = true) MemberPrincipal memberPrincipal,
        @Parameter(description = "페이지 커서 (첫 페이지는 null)") Long cursor,
        @Parameter(description = "페이지 크기 (기본 20, 최대 100)") int size,
        @Parameter(description = "기수 ID (생략하면 활성 기수)") Long gisuId);

    @Operation(operationId = "STUDY-GROUP-103", summary = "내가 관리하는 스터디 그룹 이름 목록 조회",
        description = """
            로그인한 유저가 관리할 수 있는 스터디 그룹의 (ID, 이름) 만 조회합니다.
            파트장은 본인이 맡은 그룹을, 학교 회장/부회장은 해당 학교 멤버가 속한 그룹을 볼 수 있습니다.

            그룹 선택 드롭다운처럼 목록 전체가 한 번에 필요한 화면을 위한 API 이며,
            스터디원/파트장 정보 없이 이름만 내려주고 페이지네이션이 없습니다.
            상세 정보가 필요하면 `STUDY-GROUP-101` 을 사용하세요.
            """)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    StudyGroupNameResponse getStudyGroupNames(
        @Parameter(hidden = true) MemberPrincipal memberPrincipal,
        @Parameter(description = "기수 ID (생략하면 활성 기수)") Long gisuId);

    @Operation(operationId = "STUDY-GROUP-102", summary = "스터디 그룹 정보 조회",
        description = "`studyGroupId` 에 해당하는 스터디 그룹의 정보를 조회합니다. (그룹명, 파트 또는 트랙, 기수, 스터디원, 파트장 정보)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = StudyGroupMemberResponse.class)))),
        @ApiResponse(responseCode = "404", description = "스터디 그룹을 찾을 수 없음")
    })
    StudyGroupResponse getStudyGroupInfo(
        @Parameter(description = "스터디 그룹 ID", required = true) Long groupId);
}
