package com.umc.product.community.adapter.in.web;

import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.umc.product.community.application.port.in.query.thread.BrowseCommunityThreadsUseCase;
import com.umc.product.community.application.port.in.query.thread.GetPublicCommunityThreadDetailUseCase;
import com.umc.product.community.application.port.in.query.thread.ListCommunityThreadMembersUseCase;
import com.umc.product.community.application.port.in.query.thread.SearchCommunityThreadInvitableUseCase;
import com.umc.product.community.application.port.in.query.thread.message.GetCommunityThreadMessageHistoryUseCase;
import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;

@WebMvcTest(controllers = CommunityThreadQueryController.class)
@Import({JacksonConfig.class, CommunityCanonicalPathFilter.class})
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Community canonical raw path 경계")
class CommunityCanonicalPathFilterIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private CommunityCanonicalPathFilter canonicalPathFilter;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private BrowseCommunityThreadsUseCase browseThreadsUseCase;

    @MockitoBean
    private GetPublicCommunityThreadDetailUseCase getPublicThreadDetailUseCase;

    @MockitoBean
    private ListCommunityThreadMembersUseCase listThreadMembersUseCase;

    @MockitoBean
    private SearchCommunityThreadInvitableUseCase searchThreadInvitableUseCase;

    @MockitoBean
    private GetCommunityThreadMessageHistoryUseCase getMessageHistoryUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUpCanonicalPathFilter() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .addFilters(canonicalPathFilter)
            .build();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/api/v1/community/threads/%31",
        "/api/v1/community/threads/%30%31",
        "/api/v1/community/threads/%2B1",
        "/api/v1/community/threads/%2D1"
    })
    @DisplayName("percent-encoded 숫자 path는 400으로 거절하고 Community Port In을 호출하지 않는다")
    void rejectsPercentEncodedNumericPathBeforePortIn(String rawPath) throws Exception {
        mockMvc.perform(get(URI.create(rawPath)))
            .andExpect(status().isBadRequest());

        then(browseThreadsUseCase).shouldHaveNoInteractions();
        then(getPublicThreadDetailUseCase).shouldHaveNoInteractions();
        then(listThreadMembersUseCase).shouldHaveNoInteractions();
        then(searchThreadInvitableUseCase).shouldHaveNoInteractions();
        then(getMessageHistoryUseCase).shouldHaveNoInteractions();
    }
}
