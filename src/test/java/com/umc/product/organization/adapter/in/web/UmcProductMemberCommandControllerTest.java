package com.umc.product.organization.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.umc.product.organization.adapter.in.web.dto.request.CreateUmcProductChapterMembershipRequest;
import com.umc.product.organization.adapter.in.web.dto.request.CreateUmcProductLeadershipRequest;
import com.umc.product.organization.adapter.in.web.dto.request.CreateUmcProductMemberActivityPeriodRequest;
import com.umc.product.organization.adapter.in.web.dto.request.CreateUmcProductMemberRequest;
import com.umc.product.organization.adapter.in.web.dto.request.UmcProductActivityPeriodRequest;
import com.umc.product.organization.adapter.in.web.dto.request.UpdateUmcProductChapterMembershipRequest;
import com.umc.product.organization.adapter.in.web.dto.request.UpdateUmcProductLeadershipRequest;
import com.umc.product.organization.adapter.in.web.dto.request.UpdateUmcProductMemberActivityPeriodRequest;
import com.umc.product.organization.adapter.in.web.dto.request.UpdateUmcProductMemberProfileRequest;
import com.umc.product.organization.application.port.in.command.dto.CreateUmcProductChapterMembershipCommand;
import com.umc.product.organization.application.port.in.command.dto.CreateUmcProductLeadershipCommand;
import com.umc.product.organization.application.port.in.command.dto.CreateUmcProductMemberActivityPeriodCommand;
import com.umc.product.organization.application.port.in.command.dto.CreateUmcProductMemberCommand;
import com.umc.product.organization.application.port.in.command.dto.UmcProductActivityPeriodCommand;
import com.umc.product.organization.application.port.in.command.dto.UpdateUmcProductChapterMembershipCommand;
import com.umc.product.organization.application.port.in.command.dto.UpdateUmcProductLeadershipCommand;
import com.umc.product.organization.application.port.in.command.dto.UpdateUmcProductMemberActivityPeriodCommand;
import com.umc.product.organization.application.port.in.command.dto.UpdateUmcProductMemberProfileCommand;
import com.umc.product.organization.domain.enums.UmcProductLeadershipRole;
import com.umc.product.organization.domain.enums.UmcProductPosition;
import com.umc.product.support.ControllerTestSupport;

class UmcProductMemberCommandControllerTest extends ControllerTestSupport {

    private static final LocalDate START_DATE = LocalDate.of(2026, 7, 13);
    private static final LocalDate END_DATE = LocalDate.of(2026, 12, 31);

    @Test
    @DisplayName("UMC PRODUCT 멤버를 활동 기간과 함께 생성한다")
    void UMC_PRODUCT_멤버를_생성한다() throws Exception {
        // given
        CreateUmcProductMemberRequest request = new CreateUmcProductMemberRequest(
            100L,
            "UMC PRODUCT 서버 개발자",
            "profile-file-id",
            List.of(new UmcProductActivityPeriodRequest(START_DATE, END_DATE))
        );
        given(manageUmcProductMemberUseCase.create(any())).willReturn(30L);

        // when & then
        mockMvc.perform(post("/api/v1/umc-product/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("30"));

        verify(manageUmcProductMemberUseCase).create(CreateUmcProductMemberCommand.of(
            TEST_MEMBER_ID, 100L, "UMC PRODUCT 서버 개발자", "profile-file-id",
            List.of(UmcProductActivityPeriodCommand.of(START_DATE, END_DATE))
        ));
    }

    @Test
    @DisplayName("UMC PRODUCT 멤버 프로필을 수정한다")
    void UMC_PRODUCT_멤버_프로필을_수정한다() throws Exception {
        // given
        UpdateUmcProductMemberProfileRequest request = new UpdateUmcProductMemberProfileRequest(
            "프로필 소개 수정", "new-profile-file-id"
        );

        // when & then
        mockMvc.perform(patch("/api/v1/umc-product/members/{memberId}/profile", 30L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        verify(manageUmcProductMemberUseCase).updateProfile(UpdateUmcProductMemberProfileCommand.of(
            30L, TEST_MEMBER_ID, "프로필 소개 수정", "new-profile-file-id"
        ));
    }

    @Test
    @DisplayName("UMC PRODUCT 멤버 활동 기간을 생성한다")
    void UMC_PRODUCT_멤버_활동_기간을_생성한다() throws Exception {
        // given
        CreateUmcProductMemberActivityPeriodRequest request =
            new CreateUmcProductMemberActivityPeriodRequest(START_DATE, END_DATE);
        given(manageUmcProductMemberUseCase.createActivityPeriod(any())).willReturn(40L);

        // when & then
        mockMvc.perform(post("/api/v1/umc-product/members/{memberId}/activity-periods", 30L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("40"));

        verify(manageUmcProductMemberUseCase).createActivityPeriod(CreateUmcProductMemberActivityPeriodCommand.of(
            30L, TEST_MEMBER_ID, START_DATE, END_DATE
        ));
    }

    @Test
    @DisplayName("UMC PRODUCT 멤버 활동 기간을 수정한다")
    void UMC_PRODUCT_멤버_활동_기간을_수정한다() throws Exception {
        // given
        UpdateUmcProductMemberActivityPeriodRequest request =
            new UpdateUmcProductMemberActivityPeriodRequest(START_DATE, END_DATE);

        // when & then
        mockMvc.perform(patch(
                "/api/v1/umc-product/members/{memberId}/activity-periods/{periodId}", 30L, 40L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        verify(manageUmcProductMemberUseCase).updateActivityPeriod(UpdateUmcProductMemberActivityPeriodCommand.of(
            30L, 40L, TEST_MEMBER_ID, START_DATE, END_DATE
        ));
    }

    @Test
    @DisplayName("UMC PRODUCT 멤버 활동 기간을 삭제한다")
    void UMC_PRODUCT_멤버_활동_기간을_삭제한다() throws Exception {
        // when & then
        mockMvc.perform(delete(
                "/api/v1/umc-product/members/{memberId}/activity-periods/{periodId}", 30L, 40L))
            .andExpect(status().isOk());

        verify(manageUmcProductMemberUseCase).deleteActivityPeriod(30L, 40L, TEST_MEMBER_ID);
    }

    @Test
    @DisplayName("UMC PRODUCT 멤버 Chapter 소속을 생성한다")
    void UMC_PRODUCT_멤버_Chapter_소속을_생성한다() throws Exception {
        // given
        CreateUmcProductChapterMembershipRequest request = new CreateUmcProductChapterMembershipRequest(
            20L,
            UmcProductPosition.SERVER_DEVELOPER,
            "Server Developer",
            "서버 개발",
            START_DATE,
            END_DATE
        );
        given(manageUmcProductMemberUseCase.createChapterMembership(any())).willReturn(50L);

        // when & then
        mockMvc.perform(post("/api/v1/umc-product/members/{memberId}/chapter-memberships", 30L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("50"));

        verify(manageUmcProductMemberUseCase).createChapterMembership(CreateUmcProductChapterMembershipCommand.of(
            30L, TEST_MEMBER_ID, 20L, UmcProductPosition.SERVER_DEVELOPER,
            "Server Developer", "서버 개발", START_DATE, END_DATE
        ));
    }

    @Test
    @DisplayName("UMC PRODUCT 멤버 Chapter 소속을 수정한다")
    void UMC_PRODUCT_멤버_Chapter_소속을_수정한다() throws Exception {
        // given
        UpdateUmcProductChapterMembershipRequest request = new UpdateUmcProductChapterMembershipRequest(
            20L,
            UmcProductPosition.SERVER_DEVELOPER,
            "Server Developer",
            "API 개발",
            START_DATE,
            END_DATE
        );

        // when & then
        mockMvc.perform(patch(
                "/api/v1/umc-product/members/{memberId}/chapter-memberships/{chapterMembershipId}",
                30L,
                50L
            )
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        verify(manageUmcProductMemberUseCase).updateChapterMembership(UpdateUmcProductChapterMembershipCommand.of(
            30L, 50L, TEST_MEMBER_ID, 20L, UmcProductPosition.SERVER_DEVELOPER,
            "Server Developer", "API 개발", START_DATE, END_DATE
        ));
    }

    @Test
    @DisplayName("UMC PRODUCT 멤버 Chapter 소속을 삭제한다")
    void UMC_PRODUCT_멤버_Chapter_소속을_삭제한다() throws Exception {
        // when & then
        mockMvc.perform(delete(
                "/api/v1/umc-product/members/{memberId}/chapter-memberships/{chapterMembershipId}",
                30L,
                50L
            ))
            .andExpect(status().isOk());

        verify(manageUmcProductMemberUseCase).deleteChapterMembership(30L, 50L, TEST_MEMBER_ID);
    }

    @Test
    @DisplayName("제거된 UMC PRODUCT Part 소속 API는 404를 반환한다")
    void 제거된_UMC_PRODUCT_Part_소속_API는_404를_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/umc-product/members/{memberId}/part-memberships", 30L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isNotFound());

        verifyNoInteractions(manageUmcProductMemberUseCase);
    }

    @Test
    @DisplayName("UMC PRODUCT Leadership을 생성한다")
    void UMC_PRODUCT_Leadership을_생성한다() throws Exception {
        // given
        CreateUmcProductLeadershipRequest request = new CreateUmcProductLeadershipRequest(
            UmcProductLeadershipRole.UMC_PRODUCT_LEAD, START_DATE, END_DATE
        );
        given(manageUmcProductMemberUseCase.createLeadership(any())).willReturn(60L);

        // when & then
        mockMvc.perform(post("/api/v1/umc-product/members/{memberId}/product-leaderships", 30L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("60"));

        verify(manageUmcProductMemberUseCase).createLeadership(CreateUmcProductLeadershipCommand.of(
            30L, TEST_MEMBER_ID, UmcProductLeadershipRole.UMC_PRODUCT_LEAD, START_DATE, END_DATE
        ));
    }

    @Test
    @DisplayName("UMC PRODUCT Leadership을 수정한다")
    void UMC_PRODUCT_Leadership을_수정한다() throws Exception {
        // given
        UpdateUmcProductLeadershipRequest request = new UpdateUmcProductLeadershipRequest(
            UmcProductLeadershipRole.UMC_PRODUCT_VICE_LEAD, START_DATE, END_DATE
        );

        // when & then
        mockMvc.perform(patch(
                "/api/v1/umc-product/members/{memberId}/product-leaderships/{leadershipId}",
                30L,
                60L
            )
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        verify(manageUmcProductMemberUseCase).updateLeadership(UpdateUmcProductLeadershipCommand.of(
            30L, 60L, TEST_MEMBER_ID, UmcProductLeadershipRole.UMC_PRODUCT_VICE_LEAD, START_DATE, END_DATE
        ));
    }

    @Test
    @DisplayName("UMC PRODUCT Leadership을 삭제한다")
    void UMC_PRODUCT_Leadership을_삭제한다() throws Exception {
        // when & then
        mockMvc.perform(delete(
                "/api/v1/umc-product/members/{memberId}/product-leaderships/{leadershipId}",
                30L,
                60L
            ))
            .andExpect(status().isOk());

        verify(manageUmcProductMemberUseCase).deleteLeadership(30L, 60L, TEST_MEMBER_ID);
    }

    @Test
    @DisplayName("UMC PRODUCT 멤버를 삭제한다")
    void UMC_PRODUCT_멤버를_삭제한다() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/v1/umc-product/members/{memberId}", 30L))
            .andExpect(status().isOk());

        verify(manageUmcProductMemberUseCase).delete(30L, TEST_MEMBER_ID);
    }
}
