package com.umc.product.inhouse.adapter.in.web;

import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import com.umc.product.inhouse.adapter.in.web.dto.request.FindUmcProductAccountCandidateRequest;
import com.umc.product.inhouse.application.port.in.query.dto.UmcProductAccountCandidateInfo;
import com.umc.product.inhouse.application.port.in.query.dto.UmcProductMemberAccountInfo;
import com.umc.product.inhouse.domain.enums.UmcProductMemberAccountType;
import com.umc.product.support.DocumentationTest;

class UmcProductMemberAccountQueryControllerDocumentationTest extends DocumentationTest {

    @Test
    void UMC_PRODUCT_인원의_연동_계정을_조회한다() throws Exception {
        given(getUmcProductMemberAccountUseCase.listAccounts(TEST_MEMBER_ID, 30L)).willReturn(List.of(
            new UmcProductMemberAccountInfo(
                500L,
                "정의찬",
                "제옹",
                "jeong@university.neordinary.com",
                null,
                UmcProductMemberAccountType.PROVISIONED
            )
        ));

        mockMvc.perform(get("/api/v1/umc-product/members/{umcProductMemberId}/accounts", 30L))
            .andExpect(status().isOk())
            .andDo(restDocsHandler.document(pathParameters(
                parameterWithName("umcProductMemberId").description("UMC PRODUCT 인원 ID")
            )));
    }

    @Test
    void 이메일로_연동_후보_계정을_정확히_검색한다() throws Exception {
        FindUmcProductAccountCandidateRequest request = new FindUmcProductAccountCandidateRequest(
            "linked@example.com"
        );
        given(getUmcProductMemberAccountUseCase.findCandidateByEmail(
            TEST_MEMBER_ID, "linked@example.com"
        )).willReturn(Optional.of(new UmcProductAccountCandidateInfo(
            200L,
            "홍길동",
            "길동",
            "linked@example.com",
            null,
            false
        )));

        mockMvc.perform(post("/api/v1/umc-product/members/account-candidates/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andDo(restDocsHandler.document(requestFields(
                fieldWithPath("email").type(JsonFieldType.STRING).description("정확히 일치시킬 로그인 이메일")
            )));
    }
}
