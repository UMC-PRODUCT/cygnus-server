package com.umc.product.organization.adapter.in.web;

import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.umc.product.organization.application.port.in.query.dto.school.SchoolDetailInfo;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolNameInfo;
import com.umc.product.organization.domain.enums.SchoolLinkType;
import com.umc.product.storage.application.port.in.query.dto.FileInfo;
import com.umc.product.storage.domain.enums.FileCategory;
import com.umc.product.support.DocumentationTest;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.ResultActions;

class AdminSchoolQueryControllerTest extends DocumentationTest {


    @Test
    void 학교_상세정보를_조회합니다() throws Exception {
        // give
        Long schoolId = 1L;
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-04T00:00:00Z");

        List<SchoolDetailInfo.SchoolLinkItem> links = List.of(
            new SchoolDetailInfo.SchoolLinkItem("카카오톡 오픈채팅", SchoolLinkType.KAKAO, "https://open.kakao.com/o/example"),
            new SchoolDetailInfo.SchoolLinkItem("인스타그램", SchoolLinkType.INSTAGRAM, "https://instagram.com/example"),
            new SchoolDetailInfo.SchoolLinkItem("유튜브 채널", SchoolLinkType.YOUTUBE, "https://youtube.com/@example")
        );

        SchoolDetailInfo schoolDetailInfo = new SchoolDetailInfo(3L, "Ain 지부", "중앙대학교", null, 1L, "비고", "logo-file-123",
            links, true, createdAt, updatedAt);
        FileInfo fileInfo = new FileInfo("logo-file-123", "동국대학교 로고", FileCategory.SCHOOL_LOGO, null, null,
            "https://storage.example.com/school-logo/logo.png", null, null, null);
        given(getSchoolUseCase.getSchoolDetail(schoolId)).willReturn(schoolDetailInfo);
        given(getFileUseCase.getById("logo-file-123")).willReturn(fileInfo);
        // when
        ResultActions result = mockMvc.perform(get("/api/v1/schools/{schoolId}", schoolId));
        // then
        result.andExpect((status().isOk()))
            .andDo(restDocsHandler.document(pathParameters(parameterWithName("schoolId").description("학교 ID")),
                responseFields(fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                    fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                    fieldWithPath("result.chapterId").type(JsonFieldType.STRING).description("지부 ID"),
                    fieldWithPath("result.chapterName").type(JsonFieldType.STRING).description("지부 이름"),
                    fieldWithPath("result.schoolName").type(JsonFieldType.STRING).description("학교 이름"),
                    fieldWithPath("result.schoolId").type(JsonFieldType.STRING).description("학교 ID"),
                    fieldWithPath("result.isActive").type(JsonFieldType.BOOLEAN).description("학교 활성상태"),
                    fieldWithPath("result.remark").type(JsonFieldType.STRING).description("비고"),
                    fieldWithPath("result.logoImageUrl").type(JsonFieldType.STRING).description("로고 이미지 URL")
                        .optional(),
                    fieldWithPath("result.links").type(JsonFieldType.ARRAY).description("학교 링크 목록"),
                    fieldWithPath("result.links[].title").type(JsonFieldType.STRING).description("링크 제목"),
                    fieldWithPath("result.links[].type").type(JsonFieldType.STRING)
                        .description("링크 타입 (KAKAO, INSTAGRAM, YOUTUBE)"),
                    fieldWithPath("result.links[].url").type(JsonFieldType.STRING).description("링크 URL"),
                    fieldWithPath("result.createdAt").type(JsonFieldType.STRING).description("생성일자"),
                    fieldWithPath("result.updatedAt").type(JsonFieldType.STRING).description("수정일자"))));
    }

    @Test
    void 학교_전체_목록을_조회합니다() throws Exception {
        // given
        List<SchoolNameInfo> schoolNames = List.of(
            new SchoolNameInfo(1L, "동국대학교"),
            new SchoolNameInfo(2L, "서울대학교"),
            new SchoolNameInfo(3L, "중앙대학교")
        );

        given(getSchoolUseCase.getAllSchoolNames()).willReturn(schoolNames);

        // when
        ResultActions result = mockMvc.perform(get("/api/v1/schools/all"));

        // then
        result.andExpect(status().isOk())
            .andDo(restDocsHandler.document(
                responseFields(
                    fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                    fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                    fieldWithPath("result.schools").type(JsonFieldType.ARRAY).description("학교 목록"),
                    fieldWithPath("result.schools[].schoolId").type(JsonFieldType.STRING).description("학교 ID"),
                    fieldWithPath("result.schools[].schoolName").type(JsonFieldType.STRING).description("학교 이름")
                )
            ));
    }

    private Instant toInstant(int year, int month, int day) {
        return Instant.parse(String.format("%04d-%02d-%02dT00:00:00Z", year, month, day));
    }

}
