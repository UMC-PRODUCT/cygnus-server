package com.umc.product.organization.adapter.in.web.swagger;

import com.umc.product.organization.adapter.in.web.dto.request.CreateGisuRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Organization | 기수 Command", description = "")
public interface AdminGisuControllerApi {

    @Operation(operationId = "GISU-001", summary = "기수 생성", description = "새로운 기수를 TRACK 학습 방식으로 생성합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "생성 성공"),
        @ApiResponse(responseCode = "409", description = "이미 존재하는 기수")
    })
    Long createGisu(CreateGisuRequest request);

    @Operation(operationId = "GISU-002", summary = "기수 삭제", description = "기수를 삭제합니다. 연결된 지부 또는 학교가 있으면 삭제할 수 없습니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "삭제 성공"),
        @ApiResponse(responseCode = "404", description = "기수를 찾을 수 없음"),
        @ApiResponse(responseCode = "409", description = "연결된 지부 또는 학교가 존재하여 삭제 불가")
    })
    void deleteGisu(@Parameter(description = "기수 ID", required = true) Long gisuId);

    @Operation(operationId = "GISU-003", summary = "활성 기수 변경", description = "해당 기수를 활성 상태로 변경합니다. 기존 활성 기수는 비활성화됩니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "활성화 성공"),
        @ApiResponse(responseCode = "404", description = "기수를 찾을 수 없음")
    })
    void updateActiveGisu(@Parameter(description = "기수 ID", required = true) Long gisuId);
}
