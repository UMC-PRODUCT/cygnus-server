package com.umc.product.organization.adapter.in.web;

import static org.hamcrest.Matchers.contains;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import com.umc.product.organization.application.port.in.query.dto.school.SchoolDetailInfo;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolNameInfo;
import com.umc.product.organization.domain.enums.SchoolLinkType;
import com.umc.product.support.ControllerTestSupport;

class AdminSchoolQueryControllerTest extends ControllerTestSupport {


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

        SchoolDetailInfo schoolDetailInfo = new SchoolDetailInfo(3L, "Ain 지부", "중앙대학교", "중앙대", 1L, "비고",
            "https://storage.example.com/school-logo/logo.png", links, true, createdAt, updatedAt);
        given(getSchoolUseCase.getSchoolDetail(schoolId)).willReturn(schoolDetailInfo);
        // when
        ResultActions result = mockMvc.perform(get("/api/v1/schools/{schoolId}", schoolId));
        // then
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").isString())
            .andExpect(jsonPath("$.message").isString())
            .andExpect(jsonPath("$.result.chapterId").isString())
            .andExpect(jsonPath("$.result.chapterId").value("3"))
            .andExpect(jsonPath("$.result.chapterName").value("Ain 지부"))
            .andExpect(jsonPath("$.result.schoolId").isString())
            .andExpect(jsonPath("$.result.schoolId").value("1"))
            .andExpect(jsonPath("$.result.schoolName").value("중앙대학교"))
            .andExpect(jsonPath("$.result.shortName").value("중앙대"))
            .andExpect(jsonPath("$.result.isActive").value(true))
            .andExpect(jsonPath("$.result.remark").value("비고"))
            .andExpect(jsonPath("$.result.logoImageUrl").value("https://storage.example.com/school-logo/logo.png"))
            .andExpect(jsonPath("$.result.links").isArray())
            .andExpect(jsonPath("$.result.links[*].title").value(contains("카카오톡 오픈채팅", "인스타그램", "유튜브 채널")))
            .andExpect(jsonPath("$.result.links[*].type").value(contains("KAKAO", "INSTAGRAM", "YOUTUBE")))
            .andExpect(jsonPath("$.result.links[*].url").value(contains(
                "https://open.kakao.com/o/example", "https://instagram.com/example", "https://youtube.com/@example")))
            .andExpect(jsonPath("$.result.createdAt").value(createdAt.toString()))
            .andExpect(jsonPath("$.result.updatedAt").value(updatedAt.toString()));
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
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").isString())
            .andExpect(jsonPath("$.message").isString())
            .andExpect(jsonPath("$.result.schools").isArray())
            .andExpect(jsonPath("$.result.schools[0].schoolId").isString())
            .andExpect(jsonPath("$.result.schools[*].schoolId").value(contains("1", "2", "3")))
            .andExpect(jsonPath("$.result.schools[*].schoolName").value(contains("동국대학교", "서울대학교", "중앙대학교")));
    }

    private Instant toInstant(int year, int month, int day) {
        return Instant.parse(String.format("%04d-%02d-%02dT00:00:00Z", year, month, day));
    }

}
