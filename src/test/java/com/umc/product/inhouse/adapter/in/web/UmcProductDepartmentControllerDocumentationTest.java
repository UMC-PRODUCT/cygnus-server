package com.umc.product.inhouse.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import com.umc.product.inhouse.adapter.in.web.dto.request.CreateUmcProductDepartmentParticipantRequest;
import com.umc.product.inhouse.adapter.in.web.dto.request.CreateUmcProductDepartmentRequest;
import com.umc.product.inhouse.adapter.in.web.dto.request.UpdateUmcProductDepartmentParticipantRequest;
import com.umc.product.inhouse.adapter.in.web.dto.request.UpdateUmcProductDepartmentRequest;
import com.umc.product.inhouse.application.port.in.query.dto.UmcProductDepartmentInfo;
import com.umc.product.inhouse.domain.enums.UmcProductDepartmentRole;
import com.umc.product.inhouse.domain.enums.UmcProductPosition;
import com.umc.product.support.DocumentationTest;

class UmcProductDepartmentControllerDocumentationTest extends DocumentationTest {

    private static final LocalDate START_DATE = LocalDate.of(2026, 7, 13);
    private static final LocalDate END_DATE = LocalDate.of(2026, 12, 31);

    @Test
    @DisplayName("UMC PRODUCT Department를 생성한다")
    void UMC_PRODUCT_Department를_생성한다() throws Exception {
        // given
        CreateUmcProductDepartmentRequest request = new CreateUmcProductDepartmentRequest(
            "SPRINT", "Sprint Department", "제품 개선 Department", START_DATE, END_DATE, 1, true
        );
        given(manageUmcProductDepartmentUseCase.create(any())).willReturn(70L);

        // when & then
        mockMvc.perform(post("/api/v1/umc-product/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andDo(restDocsHandler.document(requestFields(departmentFields())));
    }

    @Test
    @DisplayName("UMC PRODUCT Department 목록을 활동 기준일로 조회한다")
    void UMC_PRODUCT_Department_목록을_조회한다() throws Exception {
        // given
        given(getUmcProductDepartmentUseCase.list(true, START_DATE)).willReturn(List.of(
            new UmcProductDepartmentInfo(
                70L, "SPRINT", "Sprint Department", "제품 개선 Department", START_DATE, END_DATE, 1, true
            )
        ));

        // when & then
        mockMvc.perform(get("/api/v1/umc-product/departments")
                .param("active", "true")
                .param("activeOn", "2026-07-13"))
            .andExpect(status().isOk())
            .andDo(restDocsHandler.document(queryParameters(
                parameterWithName("active").description("활성 여부 필터").optional(),
                parameterWithName("activeOn").description("활동 기준일 (yyyy-MM-dd)").optional()
            )));
    }

    @Test
    @DisplayName("UMC PRODUCT Department를 수정한다")
    void UMC_PRODUCT_Department를_수정한다() throws Exception {
        // given
        UpdateUmcProductDepartmentRequest request = new UpdateUmcProductDepartmentRequest(
            "SPRINT-2", "Sprint Department 2", "제품 개선 Department", START_DATE, END_DATE, 2, true
        );

        // when & then
        mockMvc.perform(patch("/api/v1/umc-product/departments/{departmentId}", 70L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andDo(restDocsHandler.document(
                departmentPathParameters(),
                requestFields(departmentFields())
            ));
    }

    @Test
    @DisplayName("UMC PRODUCT Department를 삭제한다")
    void UMC_PRODUCT_Department를_삭제한다() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/v1/umc-product/departments/{departmentId}", 70L))
            .andExpect(status().isOk())
            .andDo(restDocsHandler.document(departmentPathParameters()));
    }

    @Test
    @DisplayName("UMC PRODUCT Department 참여를 생성한다")
    void UMC_PRODUCT_Department_참여를_생성한다() throws Exception {
        // given
        CreateUmcProductDepartmentParticipantRequest request = new CreateUmcProductDepartmentParticipantRequest(
            30L,
            UmcProductDepartmentRole.DEPARTMENT_LEAD,
            UmcProductPosition.PRODUCT_OWNER,
            "Department Lead",
            "제품 목표 관리",
            START_DATE,
            END_DATE
        );
        given(manageUmcProductDepartmentUseCase.createParticipant(any())).willReturn(80L);

        // when & then
        mockMvc.perform(post("/api/v1/umc-product/departments/{departmentId}/participants", 70L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andDo(restDocsHandler.document(
                departmentPathParameters(),
                requestFields(createParticipantFields())
            ));
    }

    @Test
    @DisplayName("제거된 Department 참여자 전체 교체 API는 404를 반환한다")
    void 제거된_Department_참여자_전체_교체_API는_404를_반환한다() throws Exception {
        mockMvc.perform(put("/api/v1/umc-product/departments/{departmentId}/participants", 70L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("[]"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("UMC PRODUCT Department 참여를 수정한다")
    void UMC_PRODUCT_Department_참여를_수정한다() throws Exception {
        // given
        UpdateUmcProductDepartmentParticipantRequest request = new UpdateUmcProductDepartmentParticipantRequest(
            UmcProductDepartmentRole.MEMBER,
            UmcProductPosition.SERVER_DEVELOPER,
            "Server Developer",
            "API 개발",
            START_DATE,
            END_DATE
        );

        // when & then
        mockMvc.perform(patch(
                "/api/v1/umc-product/departments/{departmentId}/participants/{participantId}", 70L, 80L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andDo(restDocsHandler.document(
                departmentAndParticipantPathParameters(),
                requestFields(updateParticipantFields())
            ));
    }

    @Test
    @DisplayName("UMC PRODUCT Department 참여를 삭제한다")
    void UMC_PRODUCT_Department_참여를_삭제한다() throws Exception {
        // when & then
        mockMvc.perform(delete(
                "/api/v1/umc-product/departments/{departmentId}/participants/{participantId}", 70L, 80L))
            .andExpect(status().isOk())
            .andDo(restDocsHandler.document(departmentAndParticipantPathParameters()));
    }

    private org.springframework.restdocs.snippet.Snippet departmentPathParameters() {
        return pathParameters(parameterWithName("departmentId").description("Department ID"));
    }

    private org.springframework.restdocs.snippet.Snippet departmentAndParticipantPathParameters() {
        return pathParameters(
            parameterWithName("departmentId").description("Department ID"),
            parameterWithName("participantId").description("Department 참여 ID")
        );
    }

    private org.springframework.restdocs.payload.FieldDescriptor[] departmentFields() {
        return new org.springframework.restdocs.payload.FieldDescriptor[] {
            fieldWithPath("code").type(JsonFieldType.STRING).description("Department 코드").optional(),
            fieldWithPath("name").type(JsonFieldType.STRING).description("Department 이름").optional(),
            fieldWithPath("description").type(JsonFieldType.STRING).description("Department 설명").optional(),
            fieldWithPath("startDate").type(JsonFieldType.STRING)
                .description("운영 시작일 (yyyy-MM-dd)"),
            fieldWithPath("endDate").type(JsonFieldType.STRING)
                .description("운영 종료일, 종료일 포함 (yyyy-MM-dd)").optional(),
            fieldWithPath("sortOrder").type(JsonFieldType.STRING).description("정렬 순서").optional(),
            fieldWithPath("active").type(JsonFieldType.BOOLEAN).description("활성 여부").optional()
        };
    }

    private org.springframework.restdocs.payload.FieldDescriptor[] createParticipantFields() {
        return new org.springframework.restdocs.payload.FieldDescriptor[] {
            fieldWithPath("umcProductMemberId").type(JsonFieldType.STRING)
                .description("UMC PRODUCT 멤버 ID"),
            updateParticipantFields()[0],
            updateParticipantFields()[1],
            updateParticipantFields()[2],
            updateParticipantFields()[3],
            updateParticipantFields()[4],
            updateParticipantFields()[5]
        };
    }

    private org.springframework.restdocs.payload.FieldDescriptor[] updateParticipantFields() {
        return new org.springframework.restdocs.payload.FieldDescriptor[] {
            fieldWithPath("role").type(JsonFieldType.STRING).description("Department 역할: MEMBER, DEPARTMENT_LEAD"),
            fieldWithPath("position").type(JsonFieldType.STRING).description("직군"),
            fieldWithPath("responsibilityTitle").type(JsonFieldType.STRING)
                .description("책임명").optional(),
            fieldWithPath("responsibilityDescription").type(JsonFieldType.STRING)
                .description("책임 설명").optional(),
            fieldWithPath("startDate").type(JsonFieldType.STRING)
                .description("참여 시작일 (yyyy-MM-dd)"),
            fieldWithPath("endDate").type(JsonFieldType.STRING)
                .description("참여 종료일, 종료일 포함 (yyyy-MM-dd)").optional()
        };
    }
}
