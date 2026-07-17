package com.umc.product.organization.adapter.in.web.swagger;

import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.organization.adapter.in.web.dto.request.AssignSchoolRequest;
import com.umc.product.organization.adapter.in.web.dto.request.CreateSchoolRequest;
import com.umc.product.organization.adapter.in.web.dto.request.DeleteSchoolsRequest;
import com.umc.product.organization.adapter.in.web.dto.request.UnassignSchoolRequest;
import com.umc.product.organization.adapter.in.web.dto.request.UpdateSchoolRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Organization | 학교 Command", description = "")
public interface AdminSchoolControllerApi {

    @Operation(operationId = "SCHOOL-001", summary = "학교 생성", description = "새로운 학교를 등록합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "생성 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "404", description = "지부를 찾을 수 없음")
    })
    void createSchool(
        CreateSchoolRequest request,
        @Parameter(hidden = true) @CurrentMember MemberPrincipal memberPrincipal
    );

    @Operation(operationId = "SCHOOL-002", summary = "학교 수정", description = "학교 정보를 수정합니다. 입력된 필드만 수정됩니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "수정 성공"),
        @ApiResponse(responseCode = "404", description = "학교를 찾을 수 없음")
    })
    void updateSchool(
        @Parameter(description = "학교 ID", required = true) Long schoolId,
        UpdateSchoolRequest request,
        @Parameter(hidden = true) @CurrentMember MemberPrincipal memberPrincipal
    );

    @Operation(operationId = "SCHOOL-003", summary = "학교 삭제", description = "여러 학교를 일괄 삭제합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "삭제 성공"),
        @ApiResponse(responseCode = "400", description = "삭제할 학교 ID가 비어있음")
    })
    void deleteSchools(DeleteSchoolsRequest request);

    @Operation(operationId = "SCHOOL-004", summary = "학교 지부 배정", description = "학교를 특정 지부에 배정합니다. 다른 지부에 있던 학교면 이동 처리됩니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "배정 성공"),
        @ApiResponse(responseCode = "404", description = "학교 또는 지부를 찾을 수 없음")
    })
    @Deprecated(since = "v2.0.0", forRemoval = true)
    void assignToChapter(
        @Parameter(description = "학교 ID", required = true) Long schoolId,
        AssignSchoolRequest request
    );

    @Operation(operationId = "SCHOOL-005", summary = "학교 지부 배정 해제", description = "학교를 지부에서 제외하여 배정 대기 상태로 변경합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "배정 해제 성공"),
        @ApiResponse(responseCode = "404", description = "학교를 찾을 수 없음")
    })
    @Deprecated(since = "v2.0.0", forRemoval = true)
    void unassignFromChapter(
        @Parameter(description = "학교 ID", required = true) Long schoolId,
        UnassignSchoolRequest request
    );
}
