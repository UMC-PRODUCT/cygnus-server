package com.umc.product.inhouse.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import com.umc.product.inhouse.adapter.in.web.dto.request.CreateUmcProductChapterMembershipRequest;
import com.umc.product.inhouse.adapter.in.web.dto.request.CreateUmcProductLeadershipRequest;
import com.umc.product.inhouse.adapter.in.web.dto.request.CreateUmcProductMemberActivityPeriodRequest;
import com.umc.product.inhouse.adapter.in.web.dto.request.CreateUmcProductMemberRequest;
import com.umc.product.inhouse.adapter.in.web.dto.request.UmcProductActivityPeriodRequest;
import com.umc.product.inhouse.adapter.in.web.dto.request.UpdateUmcProductChapterMembershipRequest;
import com.umc.product.inhouse.adapter.in.web.dto.request.UpdateUmcProductLeadershipRequest;
import com.umc.product.inhouse.adapter.in.web.dto.request.UpdateUmcProductMemberActivityPeriodRequest;
import com.umc.product.inhouse.adapter.in.web.dto.request.UpdateUmcProductMemberProfileRequest;
import com.umc.product.inhouse.domain.enums.UmcProductLeadershipRole;
import com.umc.product.inhouse.domain.enums.UmcProductPosition;
import com.umc.product.support.DocumentationTest;

class UmcProductMemberCommandControllerDocumentationTest extends DocumentationTest {

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
            .andDo(restDocsHandler.document(requestFields(
                fieldWithPath("memberId").type(JsonFieldType.STRING).description("전역 멤버 ID"),
                fieldWithPath("introduction").type(JsonFieldType.STRING).description("소개").optional(),
                fieldWithPath("profileImageId").type(JsonFieldType.STRING)
                    .description("UMC PRODUCT 프로필 이미지 파일 ID").optional(),
                fieldWithPath("activityPeriods").type(JsonFieldType.ARRAY)
                    .description("한 개 이상의 활동 기간"),
                fieldWithPath("activityPeriods[].startDate").type(JsonFieldType.STRING)
                    .description("활동 시작일 (yyyy-MM-dd)"),
                fieldWithPath("activityPeriods[].endDate").type(JsonFieldType.STRING)
                    .description("활동 종료일, 종료일 포함 (yyyy-MM-dd)").optional()
            )));
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
            .andExpect(status().isOk())
            .andDo(restDocsHandler.document(
                memberPathParameters(),
                requestFields(
                    fieldWithPath("introduction").type(JsonFieldType.STRING).description("소개").optional(),
                    fieldWithPath("profileImageId").type(JsonFieldType.STRING)
                        .description("UMC PRODUCT 프로필 이미지 파일 ID").optional()
                )
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
            .andDo(restDocsHandler.document(memberPathParameters(), requestFields(dateFields())));
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
            .andExpect(status().isOk())
            .andDo(restDocsHandler.document(
                memberAndChildPathParameters("periodId", "활동 기간 ID"),
                requestFields(dateFields())
            ));
    }

    @Test
    @DisplayName("UMC PRODUCT 멤버 활동 기간을 삭제한다")
    void UMC_PRODUCT_멤버_활동_기간을_삭제한다() throws Exception {
        // when & then
        mockMvc.perform(delete(
                "/api/v1/umc-product/members/{memberId}/activity-periods/{periodId}", 30L, 40L))
            .andExpect(status().isOk())
            .andDo(restDocsHandler.document(
                memberAndChildPathParameters("periodId", "활동 기간 ID")
            ));
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
            .andDo(restDocsHandler.document(
                memberPathParameters(),
                requestFields(chapterMembershipFields())
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
            .andExpect(status().isOk())
            .andDo(restDocsHandler.document(
                memberAndChildPathParameters("chapterMembershipId", "Chapter 소속 ID"),
                requestFields(chapterMembershipFields())
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
            .andExpect(status().isOk())
            .andDo(restDocsHandler.document(
                memberAndChildPathParameters("chapterMembershipId", "Chapter 소속 ID")
            ));
    }

    @Test
    @DisplayName("제거된 UMC PRODUCT Part 소속 API는 404를 반환한다")
    void 제거된_UMC_PRODUCT_Part_소속_API는_404를_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/umc-product/members/{memberId}/part-memberships", 30L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isNotFound());
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
            .andDo(restDocsHandler.document(
                memberPathParameters(),
                requestFields(leadershipFields())
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
            .andExpect(status().isOk())
            .andDo(restDocsHandler.document(
                memberAndChildPathParameters("leadershipId", "Leadership ID"),
                requestFields(leadershipFields())
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
            .andExpect(status().isOk())
            .andDo(restDocsHandler.document(
                memberAndChildPathParameters("leadershipId", "Leadership ID")
            ));
    }

    @Test
    @DisplayName("UMC PRODUCT 멤버를 삭제한다")
    void UMC_PRODUCT_멤버를_삭제한다() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/v1/umc-product/members/{memberId}", 30L))
            .andExpect(status().isOk())
            .andDo(restDocsHandler.document(memberPathParameters()));
    }

    private org.springframework.restdocs.snippet.Snippet memberPathParameters() {
        return pathParameters(parameterWithName("memberId").description("UMC PRODUCT 멤버 ID"));
    }

    private org.springframework.restdocs.snippet.Snippet memberAndChildPathParameters(
        String childName,
        String childDescription
    ) {
        return pathParameters(
            parameterWithName("memberId").description("UMC PRODUCT 멤버 ID"),
            parameterWithName(childName).description(childDescription)
        );
    }

    private org.springframework.restdocs.payload.FieldDescriptor[] dateFields() {
        return new org.springframework.restdocs.payload.FieldDescriptor[] {
            fieldWithPath("startDate").type(JsonFieldType.STRING)
                .description("시작일 (yyyy-MM-dd)"),
            fieldWithPath("endDate").type(JsonFieldType.STRING)
                .description("종료일, 종료일 포함 (yyyy-MM-dd)").optional()
        };
    }

    private org.springframework.restdocs.payload.FieldDescriptor[] chapterMembershipFields() {
        return new org.springframework.restdocs.payload.FieldDescriptor[] {
            fieldWithPath("chapterId").type(JsonFieldType.STRING).description("Chapter ID"),
            fieldWithPath("position").type(JsonFieldType.STRING).description("직군"),
            fieldWithPath("responsibilityTitle").type(JsonFieldType.STRING)
                .description("책임명").optional(),
            fieldWithPath("responsibilityDescription").type(JsonFieldType.STRING)
                .description("책임 설명").optional(),
            dateFields()[0],
            dateFields()[1]
        };
    }

    private org.springframework.restdocs.payload.FieldDescriptor[] leadershipFields() {
        return new org.springframework.restdocs.payload.FieldDescriptor[] {
            fieldWithPath("role").type(JsonFieldType.STRING)
                .description("Leadership 역할: UMC_PRODUCT_LEAD, UMC_PRODUCT_VICE_LEAD"),
            dateFields()[0],
            dateFields()[1]
        };
    }
}
