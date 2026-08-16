package com.umc.product.organization.adapter.in.web.swagger;

import org.springframework.web.bind.annotation.PathVariable;

import com.umc.product.organization.adapter.in.web.dto.request.CreateStudyGroupRequest;
import com.umc.product.organization.adapter.in.web.dto.request.UpdateStudyGroupRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Organization | 스터디 그룹 Command", description = "")
public interface StudyGroupCommandControllerApi {

    @Operation(operationId = "STUDY-GROUP-001", summary = "스터디 그룹 생성", description = """
        스터디 그룹을 생성합니다. 스터디 그룹은 특정 기수에 속해야 하며, 파트를 명시해야 합니다.

        스터디원 및 담당 파트장은 모두 `memberId` 로 명시해주시면 됩니다.

        스터디원의 경우, 같은 기수에 동일한 파트의 다른 스터디에 속해있지 않아야 합니다. (e.g. 9기에 SpringBoot 스터디 2개에 들어가는 것은 불가능)
        파트장은 관계 없습니다.
        """)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "404", description = "기수를 찾을 수 없음")
    })
    void create(CreateStudyGroupRequest request);

    @Operation(operationId = "STUDY-GROUP-002", summary = "스터디 그룹 수정", description = """
        스터디 그룹의 이름과 파트를 수정합니다. 전달하지 않은 필드는 변경되지 않습니다.

        파트 변경 시, 현재 스터디원 중 같은 기수의 변경할 파트 스터디에 이미 속한 회원이 있으면 400 에러가 발생합니다.
        스터디원 및 파트장 수정은 별도의 API 사용 바랍니다.
        """)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "404", description = "스터디 그룹을 찾을 수 없음")
    })
    void update(
        @Parameter(description = "스터디 그룹 ID", required = true) Long studyGroupId,
        UpdateStudyGroupRequest request);

    @Operation(operationId = "STUDY-GROUP-003", summary = "스터디 그룹에 스터디원 추가", description = "스터디 그룹에 스터디원을 추가합니다.")
    void addMember(
        @PathVariable Long studyGroupId,
        @PathVariable Long memberId
    );

    @Operation(operationId = "STUDY-GROUP-004", summary = "스터디 그룹에 담당 파트장 추가", description = "스터디 그룹에 파트장을 추가합니다.")
    void addMentor(
        @PathVariable Long studyGroupId,
        @PathVariable Long mentorId
    );

    @Operation(operationId = "STUDY-GROUP-005", summary = "스터디 그룹에 스터디원 제거", description = "스터디 그룹에서 스터디원을 제거합니다.")
    void deleteMember(
        @PathVariable Long studyGroupId,
        @PathVariable Long memberId
    );

    @Operation(operationId = "STUDY-GROUP-006", summary = "스터디 그룹에 담당 파트장 제거", description = "스터디 그룹에서 파트장을 제거합니다.")
    void deleteMentor(
        @PathVariable Long studyGroupId,
        @PathVariable Long mentorId
    );

    @Operation(operationId = "STUDY-GROUP-007", summary = "스터디 그룹 삭제", description = "스터디 그룹을 삭제합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "삭제 성공"),
        @ApiResponse(responseCode = "404", description = "스터디 그룹을 찾을 수 없음")
    })
    void delete(@Parameter(description = "스터디 그룹 ID", required = true) Long studyGroupId);
}
