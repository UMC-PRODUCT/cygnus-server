package com.umc.product.community.adapter.in.web;

import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.umc.product.community.application.port.in.command.thread.CreateCommunityThreadUseCase;
import com.umc.product.community.application.port.in.command.thread.DeleteCommunityThreadUseCase;
import com.umc.product.community.application.port.in.command.thread.ManageCommunityThreadMuteUseCase;
import com.umc.product.community.application.port.in.command.thread.ManageCommunityThreadPinUseCase;
import com.umc.product.community.application.port.in.command.thread.UpdateCommunityThreadUseCase;
import com.umc.product.community.application.port.in.query.thread.GetCommunityThreadMutationDetailUseCase;
import com.umc.product.community.application.port.in.query.thread.GetJoinedCommunityThreadDetailUseCase;
import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;

@WebMvcTest(controllers = CommunityThreadLifecycleController.class)
@Import(JacksonConfig.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Community thread lifecycle 입력 검증")
class CommunityThreadLifecycleValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CreateCommunityThreadUseCase createThreadUseCase;

    @MockitoBean
    private UpdateCommunityThreadUseCase updateThreadUseCase;

    @MockitoBean
    private DeleteCommunityThreadUseCase deleteThreadUseCase;

    @MockitoBean
    private ManageCommunityThreadPinUseCase managePinUseCase;

    @MockitoBean
    private ManageCommunityThreadMuteUseCase manageMuteUseCase;

    @MockitoBean
    private GetJoinedCommunityThreadDetailUseCase getJoinedThreadDetailUseCase;

    @MockitoBean
    private GetCommunityThreadMutationDetailUseCase getMutationDetailUseCase;

    @BeforeEach
    void setUpCurrentMember() {
        MemberPrincipal principal = new MemberPrincipal(99L);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{}",
        "{\"title\":null}",
        "{\"description\":null}",
        "{\"category\":null}",
        "{\"icon\":null}"
    })
    @DisplayName("PATCH의 empty body와 explicit null은 Port In 전에 거절한다")
    void updateThread_rejectsEmptyAndExplicitNull(String body) throws Exception {
        mockMvc.perform(patch("/api/v1/community/threads/42")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());

        then(updateThreadUseCase).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"title\":\"   \",\"category\":\"STUDY\",\"icon\":\"📚\"}",
        "{\"title\":\"스터디\",\"category\":\"STUDY\",\"icon\":\"📚📚\"}",
        "{\"title\":\"스터디\",\"category\":\"STUDY\",\"icon\":\"📚\",\"memberIds\":[7,7]}",
        "{\"title\":\"스터디\",\"category\":\"STUDY\",\"icon\":\"📚\",\"memberIds\":[0]}"
    })
    @DisplayName("생성 입력은 title, icon grapheme, unique positive member ID를 검증한다")
    void createThread_rejectsInvalidBoundaryInput(String body) throws Exception {
        mockMvc.perform(post("/api/v1/community/threads")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());

        then(createThreadUseCase).shouldHaveNoInteractions();
    }
}
