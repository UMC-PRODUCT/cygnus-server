package com.umc.product.organization.adapter.in.web;

import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.umc.product.organization.adapter.in.web.dto.request.CreateSchoolRequest;
import com.umc.product.organization.adapter.in.web.dto.request.DeleteSchoolsRequest;
import com.umc.product.organization.adapter.in.web.dto.request.SchoolLinkRequest;
import com.umc.product.organization.application.port.in.command.dto.UpdateSchoolCommand;
import com.umc.product.organization.domain.enums.SchoolLinkType;
import com.umc.product.support.ControllerTestSupport;

public class SchoolCommandControllerTest extends ControllerTestSupport {


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

        result.andExpect(status().isOk());
        then(manageSchoolUseCase).should().create(request.toCommand());

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
        result.andExpect(status().isOk());
        then(manageSchoolUseCase).should().updateSchool(schoolId,
            new UpdateSchoolCommand(request.schoolName(), request.shortName(), null, request.remark(),
                request.logoImageId(), null));

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
        then(manageSchoolUseCase).shouldHaveNoInteractions();
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
        result.andExpect(status().isOk());
        then(manageSchoolUseCase).should().deleteSchools(request.schoolIds());
    }

}
