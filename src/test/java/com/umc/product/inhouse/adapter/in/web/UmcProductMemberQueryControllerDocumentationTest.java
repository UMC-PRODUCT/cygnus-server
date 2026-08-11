package com.umc.product.inhouse.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.umc.product.inhouse.application.port.in.query.dto.UmcProductMemberInfo;
import com.umc.product.support.DocumentationTest;

class UmcProductMemberQueryControllerDocumentationTest extends DocumentationTest {

    @Test
    @DisplayName("UMC PRODUCT 멤버를 활동 기준일과 조직 조건으로 검색한다")
    void UMC_PRODUCT_멤버를_검색한다() throws Exception {
        // given
        UmcProductMemberInfo member = memberInfo();
        given(getUmcProductMemberUseCase.search(any(), any())).willReturn(
            new PageImpl<>(List.of(member), PageRequest.of(0, 10), 1)
        );

        // when & then
        mockMvc.perform(get("/api/v1/umc-product/members")
                .param("chapterId", "10")
                .param("leadershipRole", "UMC_PRODUCT_LEAD")
                .param("position", "SERVER_DEVELOPER")
                .param("departmentId", "70")
                .param("includeDescendants", "true")
                .param("activeOn", "2026-07-13")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andDo(restDocsHandler.document(queryParameters(
                parameterWithName("chapterId").description("Chapter ID 필터").optional(),
                parameterWithName("leadershipRole")
                    .description("Product Leadership 역할 필터").optional(),
                parameterWithName("position").description("직군 필터").optional(),
                parameterWithName("departmentId").description("Department ID 필터").optional(),
                parameterWithName("includeDescendants").description("하위 Department 소속 포함 여부").optional(),
                parameterWithName("activeOn").description("활동 기준일 (yyyy-MM-dd)").optional(),
                parameterWithName("page").description("페이지 번호 (0부터 시작)").optional(),
                parameterWithName("size").description("페이지 크기").optional()
            )));
    }

    @Test
    @DisplayName("UMC PRODUCT 멤버 상세와 전체 활동 이력을 조회한다")
    void UMC_PRODUCT_멤버_상세를_조회한다() throws Exception {
        // given
        given(getUmcProductMemberUseCase.getById(30L)).willReturn(memberInfo());

        // when & then
        mockMvc.perform(get("/api/v1/umc-product/members/{memberId}", 30L))
            .andExpect(status().isOk())
            .andDo(restDocsHandler.document(pathParameters(
                parameterWithName("memberId").description("UMC PRODUCT 멤버 ID")
            )));
    }

    @Test
    @DisplayName("로그인 계정에 연동된 내 UMC PRODUCT 프로필을 조회한다")
    void 내_프로필을_조회한다() throws Exception {
        given(getUmcProductMemberUseCase.findByAccountMemberId(TEST_MEMBER_ID))
            .willReturn(Optional.of(memberInfo()));
        given(umcProductAccessPolicy.canManageUmcProduct(TEST_MEMBER_ID)).willReturn(true);

        mockMvc.perform(get("/api/v1/umc-product/members/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.profile.umcProductMemberId").value(30L))
            .andExpect(jsonPath("$.result.canManage").value(true))
            .andDo(restDocsHandler);
    }

    @Test
    @DisplayName("연동된 인원이 없어도 관리 가능 여부와 null 프로필을 반환한다")
    void 연동된_인원이_없어도_권한을_반환한다() throws Exception {
        given(getUmcProductMemberUseCase.findByAccountMemberId(TEST_MEMBER_ID)).willReturn(Optional.empty());
        given(umcProductAccessPolicy.canManageUmcProduct(TEST_MEMBER_ID)).willReturn(true);

        mockMvc.perform(get("/api/v1/umc-product/members/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.profile").isEmpty())
            .andExpect(jsonPath("$.result.canManage").value(true));
    }

    @Test
    @DisplayName("활동 기준일에 시각이나 offset이 포함되면 요청을 거부한다")
    void 시각이_포함된_활동_기준일을_거부한다() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/umc-product/members")
                .param("activeOn", "2026-07-13T00:00:00Z"))
            .andExpect(status().isBadRequest());
    }

    private UmcProductMemberInfo memberInfo() {
        return new UmcProductMemberInfo(
            30L,
            "홍길동",
            "길동",
            20L,
            "한국대학교",
            "UMC PRODUCT 서버 개발자",
            "umc-product-profile-id",
            "https://example.com/umc-product-profile.png",
            List.of(),
            List.of(),
            List.of(),
            List.of()
        );
    }
}
