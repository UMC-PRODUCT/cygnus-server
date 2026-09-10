package com.umc.product.organization.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductMemberInfo;
import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductMemberSearchCondition;
import com.umc.product.organization.domain.enums.UmcProductLeadershipRole;
import com.umc.product.organization.domain.enums.UmcProductPosition;
import com.umc.product.support.ControllerTestSupport;

class UmcProductMemberQueryControllerTest extends ControllerTestSupport {

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
                .param("squadId", "70")
                .param("activeOn", "2026-07-13")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.result.content.length()").value(1))
            .andExpect(jsonPath("$.result.content[0].umcProductMemberId").value("30"))
            .andExpect(jsonPath("$.result.content[0].memberName").value("홍길동"))
            .andExpect(jsonPath("$.result.page").value("0"))
            .andExpect(jsonPath("$.result.size").value("10"))
            .andExpect(jsonPath("$.result.totalElements").value("1"))
            .andExpect(jsonPath("$.result.totalPages").value("1"))
            .andExpect(jsonPath("$.result.hasNext").value(false))
            .andExpect(jsonPath("$.result.hasPrevious").value(false));

        verify(getUmcProductMemberUseCase).search(
            UmcProductMemberSearchCondition.of(
                10L, UmcProductLeadershipRole.UMC_PRODUCT_LEAD, UmcProductPosition.SERVER_DEVELOPER,
                70L, LocalDate.of(2026, 7, 13)
            ),
            PageRequest.of(0, 10)
        );
    }

    @Test
    @DisplayName("UMC PRODUCT 멤버 상세와 전체 활동 이력을 조회한다")
    void UMC_PRODUCT_멤버_상세를_조회한다() throws Exception {
        // given
        given(getUmcProductMemberUseCase.getById(30L)).willReturn(memberInfo());

        // when & then
        mockMvc.perform(get("/api/v1/umc-product/members/{memberId}", 30L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.umcProductMemberId").value("30"))
            .andExpect(jsonPath("$.result.memberId").value("100"))
            .andExpect(jsonPath("$.result.memberName").value("홍길동"))
            .andExpect(jsonPath("$.result.memberNickname").value("길동"))
            .andExpect(jsonPath("$.result.memberSchoolName").value("한국대학교"))
            .andExpect(jsonPath("$.result.memberProfileImageId").value("member-profile-id"))
            .andExpect(jsonPath("$.result.memberProfileImageUrl").value("https://example.com/member-profile.png"))
            .andExpect(jsonPath("$.result.introduction").value("UMC PRODUCT 서버 개발자"))
            .andExpect(jsonPath("$.result.umcProductProfileImageId").value("umc-product-profile-id"))
            .andExpect(jsonPath("$.result.umcProductProfileImageUrl").value("https://example.com/umc-product-profile.png"))
            .andExpect(jsonPath("$.result.activityPeriods").isArray())
            .andExpect(jsonPath("$.result.activityPeriods").isEmpty())
            .andExpect(jsonPath("$.result.chapterMemberships").isArray())
            .andExpect(jsonPath("$.result.chapterMemberships").isEmpty())
            .andExpect(jsonPath("$.result.productLeaderships").isArray())
            .andExpect(jsonPath("$.result.productLeaderships").isEmpty())
            .andExpect(jsonPath("$.result.squadParticipations").isArray())
            .andExpect(jsonPath("$.result.squadParticipations").isEmpty());

        verify(getUmcProductMemberUseCase).getById(30L);
    }

    @Test
    @DisplayName("활동 기준일에 시각이나 offset이 포함되면 요청을 거부한다")
    void 시각이_포함된_활동_기준일을_거부한다() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/umc-product/members")
                .param("activeOn", "2026-07-13T00:00:00Z"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(getUmcProductMemberUseCase);
    }

    private UmcProductMemberInfo memberInfo() {
        return new UmcProductMemberInfo(
            30L,
            100L,
            "홍길동",
            "길동",
            "한국대학교",
            "member-profile-id",
            "https://example.com/member-profile.png",
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
