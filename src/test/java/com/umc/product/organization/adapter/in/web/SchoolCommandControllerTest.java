package com.umc.product.organization.adapter.in.web;

import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.ResultActions;

import com.umc.product.organization.adapter.in.web.dto.request.CreateSchoolRequest;
import com.umc.product.organization.adapter.in.web.dto.request.DeleteSchoolsRequest;
import com.umc.product.organization.adapter.in.web.dto.request.SchoolLinkRequest;
import com.umc.product.organization.domain.enums.SchoolLinkType;
import com.umc.product.support.DocumentationTest;

public class SchoolCommandControllerTest extends DocumentationTest {


    @Test
    void 총괄_신규학교를_추가한다() throws Exception {
        // given when
        CreateSchoolRequest request = CreateSchoolRequest.builder().schoolName("중앙대학교")
            .shortName("중앙대")
            .remark("중앙대는 멋집니다.").logoImageId("file-123")
            .links(List.of(
                new SchoolLinkRequest("카카오톡 오픈채팅", SchoolLinkType.KAKAO, "https://open.kakao.com/o/example"),
                new SchoolLinkRequest("인스타그램", SchoolLinkType.INSTAGRAM, "https://instagram.com/example"),
                new SchoolLinkRequest("유튜브 채널", SchoolLinkType.YOUTUBE, "https://youtube.com/@example")
            )).build();

        // then
        ResultActions result = mockMvc.perform(
            post("/api/v1/schools").content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk()).andDo(restDocsHandler.document(
            requestFields(fieldWithPath("schoolName").type(JsonFieldType.STRING).description("학교 이름"),
                fieldWithPath("shortName").optional().type(JsonFieldType.STRING).description("학교 약칭 (20자 이내)"),
                fieldWithPath("remark").type(JsonFieldType.STRING).description("비고"),
                fieldWithPath("logoImageId").optional().type(JsonFieldType.STRING).description("로고 이미지 파일 ID"),
                fieldWithPath("links").optional().type(JsonFieldType.ARRAY).description("학교 링크 목록"),
                fieldWithPath("links[].title").type(JsonFieldType.STRING).description("링크 제목"),
                fieldWithPath("links[].type").type(JsonFieldType.STRING)
                    .description("링크 타입 (KAKAO, INSTAGRAM, YOUTUBE)"),
                fieldWithPath("links[].url").type(JsonFieldType.STRING).description("링크 URL"))));

    }

    @Test
    void 총괄_학교정보를_수정한다() throws Exception {
        // given // when
        Long schoolId = 1L;

        CreateSchoolRequest request = CreateSchoolRequest.builder().schoolName("동국대학교")
            .shortName("동국대")
            .remark("신승호 라면이 맛있습니다.").logoImageId("file-456").build();

        ResultActions result = mockMvc.perform(
            patch("/api/v1/schools/{schoolId}", schoolId).content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isOk())
            .andDo(restDocsHandler.document(pathParameters(parameterWithName("schoolId").description("학교 ID")),
                requestFields(
                    fieldWithPath("schoolName").optional().type(JsonFieldType.STRING).description("학교 이름"),
                    fieldWithPath("shortName").optional().type(JsonFieldType.STRING).description("학교 약칭 (20자 이내)"),
                    fieldWithPath("remark").optional().type(JsonFieldType.STRING).description("비고"),
                    fieldWithPath("logoImageId").optional().type(JsonFieldType.STRING).description("로고 이미지 파일 ID"),
                    fieldWithPath("links").optional().type(JsonFieldType.ARRAY).description("학교 링크 목록 (전달 시 전체 교체)"),
                    fieldWithPath("links[].title").optional().type(JsonFieldType.STRING).description("링크 제목"),
                    fieldWithPath("links[].type").optional().type(JsonFieldType.STRING)
                        .description("링크 타입 (KAKAO, INSTAGRAM, YOUTUBE)"),
                    fieldWithPath("links[].url").optional().type(JsonFieldType.STRING).description("링크 URL"))));

    }

    @Test
    void 총괄_신규학교_shortName이_20자를_초과하면_400을_반환한다() throws Exception {
        // given
        String tooLongShortName = "가".repeat(21);
        CreateSchoolRequest request = CreateSchoolRequest.builder()
            .schoolName("중앙대학교")
            .shortName(tooLongShortName)
            .remark("비고")
            .build();

        // when
        ResultActions result = mockMvc.perform(
            post("/api/v1/schools").content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    void 총괄_학교를_일괄_삭제한다() throws Exception {
        // given
        DeleteSchoolsRequest request = new DeleteSchoolsRequest(List.of(1L, 2L, 3L));

        // when
        ResultActions result = mockMvc.perform(
            delete("/api/v1/schools").content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isOk()).andDo(restDocsHandler.document(
            requestFields(fieldWithPath("schoolIds").type(JsonFieldType.ARRAY).description("삭제할 학교 ID 목록"))));
    }

}
