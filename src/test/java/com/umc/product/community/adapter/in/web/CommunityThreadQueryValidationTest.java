package com.umc.product.community.adapter.in.web;

import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.umc.product.community.application.port.in.query.thread.BrowseCommunityThreadsUseCase;
import com.umc.product.community.application.port.in.query.thread.GetPublicCommunityThreadDetailUseCase;
import com.umc.product.community.application.port.in.query.thread.ListCommunityThreadMembersUseCase;
import com.umc.product.community.application.port.in.query.thread.SearchCommunityThreadInvitableUseCase;
import com.umc.product.community.application.port.in.query.thread.message.GetCommunityThreadMessageHistoryUseCase;
import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;

@WebMvcTest(controllers = CommunityThreadQueryController.class)
@Import(JacksonConfig.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Community thread query 입력 검증")
class CommunityThreadQueryValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private BrowseCommunityThreadsUseCase browseThreadsUseCase;

    @MockitoBean
    private GetPublicCommunityThreadDetailUseCase getPublicThreadUseCase;

    @MockitoBean
    private ListCommunityThreadMembersUseCase listThreadMembersUseCase;

    @MockitoBean
    private SearchCommunityThreadInvitableUseCase searchThreadInvitableUseCase;

    @MockitoBean
    private GetCommunityThreadMessageHistoryUseCase getMessageHistoryUseCase;

    @BeforeEach
    void setUpCurrentMember() {
        MemberPrincipal principal = new MemberPrincipal(99L);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "+1", "01", "1x", "9223372036854775808"})
    @DisplayName("positive-decimal full match가 아닌 thread ID는 Port In 전에 거절한다")
    void getThread_rejectsInvalidPositiveDecimalId(String threadId) throws Exception {
        mockMvc.perform(get("/api/v1/community/threads/{threadId}", threadId))
            .andExpect(status().is4xxClientError());

        then(getPublicThreadUseCase).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "101"})
    @DisplayName("history limit 범위를 벗어나면 Port In 전에 거절한다")
    void getMessageHistory_rejectsInvalidLimit(String limit) throws Exception {
        mockMvc.perform(get("/api/v1/community/threads/42/messages").param("limit", limit))
            .andExpect(status().isBadRequest());

        then(getMessageHistoryUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("81 code point 검색어는 Port In 전에 거절한다")
    void listThreads_rejectsLongKeyword() throws Exception {
        mockMvc.perform(get("/api/v1/community/threads").param("q", "가".repeat(81)))
            .andExpect(status().isBadRequest());

        then(browseThreadsUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("음수 offset은 Port In 전에 거절한다")
    void listThreads_rejectsNegativeOffset() throws Exception {
        mockMvc.perform(get("/api/v1/community/threads").param("offset", "-1"))
            .andExpect(status().isBadRequest());

        then(browseThreadsUseCase).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ALL", "UNREAD", "study", "qna", "project", "free", "unknown"})
    @DisplayName("문서화된 대소문자와 다른 filter는 Port In 전에 거절한다")
    void listThreads_rejectsUndocumentedFilterValues(String filter) throws Exception {
        mockMvc.perform(get("/api/v1/community/threads").param("filter", filter))
            .andExpect(status().isBadRequest());

        then(browseThreadsUseCase).shouldHaveNoInteractions();
    }
}
