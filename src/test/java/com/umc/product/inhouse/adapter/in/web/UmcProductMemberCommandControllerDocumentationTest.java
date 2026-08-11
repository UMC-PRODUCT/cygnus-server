package com.umc.product.inhouse.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
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
import com.umc.product.inhouse.adapter.in.web.dto.request.LinkUmcProductMemberAccountRequest;
import com.umc.product.inhouse.adapter.in.web.dto.request.RegisterUmcProductMemberRequest;
import com.umc.product.inhouse.adapter.in.web.dto.request.UmcProductActivityPeriodRequest;
import com.umc.product.inhouse.adapter.in.web.dto.request.UpdateUmcProductChapterMembershipRequest;
import com.umc.product.inhouse.adapter.in.web.dto.request.UpdateUmcProductLeadershipRequest;
import com.umc.product.inhouse.adapter.in.web.dto.request.UpdateUmcProductMemberActivityPeriodRequest;
import com.umc.product.inhouse.adapter.in.web.dto.request.UpdateUmcProductMemberProfileRequest;
import com.umc.product.inhouse.application.port.in.command.dto.RegisterUmcProductMemberResult;
import com.umc.product.inhouse.application.port.in.command.dto.ResetUmcProductAccountPasswordResult;
import com.umc.product.inhouse.domain.enums.UmcProductDepartmentRole;
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
            "홍길동",
            "길동",
            20L,
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
                fieldWithPath("name").type(JsonFieldType.STRING).description("이름"),
                fieldWithPath("nickname").type(JsonFieldType.STRING).description("닉네임"),
                fieldWithPath("schoolId").type(JsonFieldType.STRING).description("학교 ID").optional(),
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
    @DisplayName("UMC PRODUCT 인원을 등록하고 계정을 자동 발급한다")
    void UMC_PRODUCT_인원을_등록하고_계정을_발급한다() throws Exception {
        RegisterUmcProductMemberRequest request = new RegisterUmcProductMemberRequest(
            "홍길동",
            "길동",
            "gildong",
            20L,
            "UMC PRODUCT 서버 개발자",
            "profile-file-id",
            List.of(new UmcProductActivityPeriodRequest(START_DATE, END_DATE)),
            List.of(new RegisterUmcProductMemberRequest.ChapterMembership(
                10L, UmcProductPosition.SERVER_DEVELOPER, "Server Developer", "API 개발", START_DATE, END_DATE
            )),
            List.of(new RegisterUmcProductMemberRequest.DepartmentParticipation(
                70L,
                UmcProductDepartmentRole.DEPARTMENT_LEAD,
                UmcProductPosition.SERVER_DEVELOPER,
                "Department Lead",
                "제품 개발 리드",
                START_DATE,
                END_DATE
            )),
            List.of(new RegisterUmcProductMemberRequest.ProductLeadership(
                UmcProductLeadershipRole.UMC_PRODUCT_LEAD, START_DATE, END_DATE
            ))
        );
        given(manageUmcProductMemberUseCase.register(any())).willReturn(
            new RegisterUmcProductMemberResult(
                30L, 500L, "gildong@university.neordinary.com", "TempPass1!aaaaaa"
            )
        );

        mockMvc.perform(post("/api/v1/umc-product/members/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andDo(restDocsHandler.document(
                requestFields(registerFields()),
                responseFields(
                    fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                    fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                    fieldWithPath("result.umcProductMemberId").type(JsonFieldType.STRING)
                        .description("생성된 UMC PRODUCT 인원 ID"),
                    fieldWithPath("result.memberId").type(JsonFieldType.STRING)
                        .description("자동 발급된 로그인 계정 ID"),
                    fieldWithPath("result.email").type(JsonFieldType.STRING)
                        .description("자동 발급된 로그인 이메일"),
                    fieldWithPath("result.temporaryPassword").type(JsonFieldType.STRING)
                        .description("응답에서 한 번만 노출되는 임시 비밀번호")
                )
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
    void UMC_PRODUCT_인원에_로그인_계정을_연동한다() throws Exception {
        LinkUmcProductMemberAccountRequest request = new LinkUmcProductMemberAccountRequest(200L);
        given(manageUmcProductMemberUseCase.linkAccount(any())).willReturn(7L);

        mockMvc.perform(post("/api/v1/umc-product/members/{umcProductMemberId}/accounts", 30L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andDo(restDocsHandler.document(
                pathParameters(parameterWithName("umcProductMemberId").description("UMC PRODUCT 인원 ID")),
                requestFields(fieldWithPath("memberId").type(JsonFieldType.STRING).description("연동할 로그인 계정 ID"))
            ));
    }

    @Test
    void UMC_PRODUCT_인원의_로그인_계정_연동을_해제한다() throws Exception {
        mockMvc.perform(delete(
                "/api/v1/umc-product/members/{umcProductMemberId}/accounts/{accountMemberId}",
                30L,
                200L
            ))
            .andExpect(status().isOk())
            .andDo(restDocsHandler.document(pathParameters(
                parameterWithName("umcProductMemberId").description("UMC PRODUCT 인원 ID"),
                parameterWithName("accountMemberId").description("연동 해제할 로그인 계정 ID")
            )));
    }

    @Test
    void 자동_발급_계정의_임시_비밀번호를_재발급한다() throws Exception {
        given(manageUmcProductMemberUseCase.resetAccountPassword(30L, 500L, TEST_MEMBER_ID))
            .willReturn(new ResetUmcProductAccountPasswordResult(
                "jeong@university.neordinary.com",
                "NewTemp1!bbbbbbb"
            ));

        mockMvc.perform(post(
                "/api/v1/umc-product/members/{umcProductMemberId}/accounts/{accountMemberId}/reset-password",
                30L,
                500L
            ))
            .andExpect(status().isOk())
            .andDo(restDocsHandler.document(
                pathParameters(
                    parameterWithName("umcProductMemberId").description("UMC PRODUCT 인원 ID"),
                    parameterWithName("accountMemberId").description("비밀번호를 재발급할 로그인 계정 ID")
                ),
                responseFields(
                    fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                    fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                    fieldWithPath("result.email").type(JsonFieldType.STRING).description("발급 계정 이메일"),
                    fieldWithPath("result.temporaryPassword").type(JsonFieldType.STRING)
                        .description("응답에서 한 번만 노출되는 임시 비밀번호")
                )
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

    private org.springframework.restdocs.payload.FieldDescriptor[] registerFields() {
        return new org.springframework.restdocs.payload.FieldDescriptor[] {
            fieldWithPath("name").type(JsonFieldType.STRING).description("이름"),
            fieldWithPath("nickname").type(JsonFieldType.STRING).description("닉네임"),
            fieldWithPath("englishNickname").type(JsonFieldType.STRING).description("이메일용 영어 닉네임"),
            fieldWithPath("schoolId").type(JsonFieldType.STRING).description("학교 ID").optional(),
            fieldWithPath("introduction").type(JsonFieldType.STRING).description("소개").optional(),
            fieldWithPath("profileImageId").type(JsonFieldType.STRING).description("프로필 이미지 ID").optional(),
            fieldWithPath("activityPeriods").type(JsonFieldType.ARRAY).description("한 개 이상의 활동 기간"),
            fieldWithPath("activityPeriods[].startDate").type(JsonFieldType.STRING).description("활동 시작일"),
            fieldWithPath("activityPeriods[].endDate").type(JsonFieldType.STRING).description("활동 종료일").optional(),
            fieldWithPath("chapterMemberships").type(JsonFieldType.ARRAY).description("초기 Chapter 소속"),
            fieldWithPath("chapterMemberships[].chapterId").type(JsonFieldType.STRING).description("Chapter ID"),
            fieldWithPath("chapterMemberships[].position").type(JsonFieldType.STRING).description("직군"),
            fieldWithPath("chapterMemberships[].responsibilityTitle").type(JsonFieldType.STRING)
                .description("책임명").optional(),
            fieldWithPath("chapterMemberships[].responsibilityDescription").type(JsonFieldType.STRING)
                .description("책임 설명").optional(),
            fieldWithPath("chapterMemberships[].startDate").type(JsonFieldType.STRING).description("시작일"),
            fieldWithPath("chapterMemberships[].endDate").type(JsonFieldType.STRING).description("종료일").optional(),
            fieldWithPath("departmentParticipations").type(JsonFieldType.ARRAY).description("초기 Department 참여"),
            fieldWithPath("departmentParticipations[].departmentId").type(JsonFieldType.STRING)
                .description("Department ID"),
            fieldWithPath("departmentParticipations[].role").type(JsonFieldType.STRING).description("역할"),
            fieldWithPath("departmentParticipations[].position").type(JsonFieldType.STRING).description("직군"),
            fieldWithPath("departmentParticipations[].responsibilityTitle").type(JsonFieldType.STRING)
                .description("책임명").optional(),
            fieldWithPath("departmentParticipations[].responsibilityDescription").type(JsonFieldType.STRING)
                .description("책임 설명").optional(),
            fieldWithPath("departmentParticipations[].startDate").type(JsonFieldType.STRING).description("시작일"),
            fieldWithPath("departmentParticipations[].endDate").type(JsonFieldType.STRING)
                .description("종료일").optional(),
            fieldWithPath("productLeaderships").type(JsonFieldType.ARRAY).description("초기 Product Leadership"),
            fieldWithPath("productLeaderships[].role").type(JsonFieldType.STRING).description("Leadership 역할"),
            fieldWithPath("productLeaderships[].startDate").type(JsonFieldType.STRING).description("시작일"),
            fieldWithPath("productLeaderships[].endDate").type(JsonFieldType.STRING).description("종료일").optional()
        };
    }
}
