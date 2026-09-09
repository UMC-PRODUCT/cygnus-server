package com.umc.product.organization.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.umc.product.organization.adapter.in.web.dto.request.CreateUmcProductSquadParticipantRequest;
import com.umc.product.organization.adapter.in.web.dto.request.CreateUmcProductSquadRequest;
import com.umc.product.organization.adapter.in.web.dto.request.UpdateUmcProductSquadParticipantRequest;
import com.umc.product.organization.adapter.in.web.dto.request.UpdateUmcProductSquadRequest;
import com.umc.product.organization.application.port.in.command.dto.CreateUmcProductSquadCommand;
import com.umc.product.organization.application.port.in.command.dto.CreateUmcProductSquadParticipantCommand;
import com.umc.product.organization.application.port.in.command.dto.UpdateUmcProductSquadCommand;
import com.umc.product.organization.application.port.in.command.dto.UpdateUmcProductSquadParticipantCommand;
import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductSquadInfo;
import com.umc.product.organization.domain.enums.UmcProductPosition;
import com.umc.product.organization.domain.enums.UmcProductSquadRole;
import com.umc.product.support.ControllerTestSupport;

class UmcProductSquadControllerTest extends ControllerTestSupport {

    private static final LocalDate START_DATE = LocalDate.of(2026, 7, 13);
    private static final LocalDate END_DATE = LocalDate.of(2026, 12, 31);

    @Test
    @DisplayName("UMC PRODUCT Squad를 생성한다")
    void UMC_PRODUCT_Squad를_생성한다() throws Exception {
        // given
        CreateUmcProductSquadRequest request = new CreateUmcProductSquadRequest(
            "SPRINT", "Sprint Squad", "제품 개선 Squad", START_DATE, END_DATE, 1, true
        );
        given(manageUmcProductSquadUseCase.create(any())).willReturn(70L);

        // when & then
        mockMvc.perform(post("/api/v1/umc-product/squads")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("70"));

        verify(manageUmcProductSquadUseCase).create(CreateUmcProductSquadCommand.of(
            TEST_MEMBER_ID, "SPRINT", "Sprint Squad", "제품 개선 Squad", START_DATE, END_DATE, 1, true
        ));
    }

    @Test
    @DisplayName("UMC PRODUCT Squad 목록을 활동 기준일로 조회한다")
    void UMC_PRODUCT_Squad_목록을_조회한다() throws Exception {
        // given
        given(getUmcProductSquadUseCase.list(true, START_DATE)).willReturn(List.of(
            new UmcProductSquadInfo(
                70L, "SPRINT", "Sprint Squad", "제품 개선 Squad", START_DATE, END_DATE, 1, true
            )
        ));

        // when & then
        mockMvc.perform(get("/api/v1/umc-product/squads")
                .param("active", "true")
                .param("activeOn", "2026-07-13"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.result.squads.length()").value(1))
            .andExpect(jsonPath("$.result.squads[0].squadId").value("70"))
            .andExpect(jsonPath("$.result.squads[0].code").value("SPRINT"))
            .andExpect(jsonPath("$.result.squads[0].name").value("Sprint Squad"))
            .andExpect(jsonPath("$.result.squads[0].description").value("제품 개선 Squad"))
            .andExpect(jsonPath("$.result.squads[0].startDate").value("2026-07-13"))
            .andExpect(jsonPath("$.result.squads[0].endDate").value("2026-12-31"))
            .andExpect(jsonPath("$.result.squads[0].sortOrder").value("1"))
            .andExpect(jsonPath("$.result.squads[0].active").value(true));

        verify(getUmcProductSquadUseCase).list(true, START_DATE);
    }

    @Test
    @DisplayName("UMC PRODUCT Squad를 수정한다")
    void UMC_PRODUCT_Squad를_수정한다() throws Exception {
        // given
        UpdateUmcProductSquadRequest request = new UpdateUmcProductSquadRequest(
            "SPRINT-2", "Sprint Squad 2", "제품 개선 Squad", START_DATE, END_DATE, 2, true
        );

        // when & then
        mockMvc.perform(patch("/api/v1/umc-product/squads/{squadId}", 70L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        verify(manageUmcProductSquadUseCase).update(UpdateUmcProductSquadCommand.of(
            70L, TEST_MEMBER_ID, "SPRINT-2", "Sprint Squad 2", "제품 개선 Squad", START_DATE, END_DATE, 2, true
        ));
    }

    @Test
    @DisplayName("UMC PRODUCT Squad를 삭제한다")
    void UMC_PRODUCT_Squad를_삭제한다() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/v1/umc-product/squads/{squadId}", 70L))
            .andExpect(status().isOk());

        verify(manageUmcProductSquadUseCase).delete(70L, TEST_MEMBER_ID);
    }

    @Test
    @DisplayName("UMC PRODUCT Squad 참여를 생성한다")
    void UMC_PRODUCT_Squad_참여를_생성한다() throws Exception {
        // given
        CreateUmcProductSquadParticipantRequest request = new CreateUmcProductSquadParticipantRequest(
            30L,
            UmcProductSquadRole.SQUAD_LEAD,
            UmcProductPosition.PRODUCT_OWNER,
            "Squad Lead",
            "제품 목표 관리",
            START_DATE,
            END_DATE
        );
        given(manageUmcProductSquadUseCase.createParticipant(any())).willReturn(80L);

        // when & then
        mockMvc.perform(post("/api/v1/umc-product/squads/{squadId}/participants", 70L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("80"));

        verify(manageUmcProductSquadUseCase).createParticipant(CreateUmcProductSquadParticipantCommand.of(
            70L, TEST_MEMBER_ID, 30L, UmcProductSquadRole.SQUAD_LEAD, UmcProductPosition.PRODUCT_OWNER,
            "Squad Lead", "제품 목표 관리", START_DATE, END_DATE
        ));
    }

    @Test
    @DisplayName("제거된 Squad 참여자 전체 교체 API는 404를 반환한다")
    void 제거된_Squad_참여자_전체_교체_API는_404를_반환한다() throws Exception {
        mockMvc.perform(put("/api/v1/umc-product/squads/{squadId}/participants", 70L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("[]"))
            .andExpect(status().isNotFound());

        verifyNoInteractions(manageUmcProductSquadUseCase);
    }

    @Test
    @DisplayName("UMC PRODUCT Squad 참여를 수정한다")
    void UMC_PRODUCT_Squad_참여를_수정한다() throws Exception {
        // given
        UpdateUmcProductSquadParticipantRequest request = new UpdateUmcProductSquadParticipantRequest(
            UmcProductSquadRole.MEMBER,
            UmcProductPosition.SERVER_DEVELOPER,
            "Server Developer",
            "API 개발",
            START_DATE,
            END_DATE
        );

        // when & then
        mockMvc.perform(patch(
                "/api/v1/umc-product/squads/{squadId}/participants/{participantId}", 70L, 80L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        verify(manageUmcProductSquadUseCase).updateParticipant(UpdateUmcProductSquadParticipantCommand.of(
            70L, 80L, TEST_MEMBER_ID, UmcProductSquadRole.MEMBER, UmcProductPosition.SERVER_DEVELOPER,
            "Server Developer", "API 개발", START_DATE, END_DATE
        ));
    }

    @Test
    @DisplayName("UMC PRODUCT Squad 참여를 삭제한다")
    void UMC_PRODUCT_Squad_참여를_삭제한다() throws Exception {
        // when & then
        mockMvc.perform(delete(
                "/api/v1/umc-product/squads/{squadId}/participants/{participantId}", 70L, 80L))
            .andExpect(status().isOk());

        verify(manageUmcProductSquadUseCase).deleteParticipant(70L, 80L, TEST_MEMBER_ID);
    }
}
