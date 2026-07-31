package com.umc.product.community.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
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
import com.umc.product.community.application.port.in.command.thread.dto.CommunityThreadLifecycleInfo;
import com.umc.product.community.application.port.in.command.thread.dto.CreateCommunityThreadCommand;
import com.umc.product.community.application.port.in.command.thread.dto.UpdateCommunityThreadCommand;
import com.umc.product.community.application.port.in.query.thread.GetCommunityThreadDetailUseCase;
import com.umc.product.community.application.port.in.query.thread.GetCommunityThreadMutationDetailUseCase;
import com.umc.product.community.application.port.in.query.thread.dto.ThreadDetailInfo;
import com.umc.product.community.domain.enums.CommunityThreadCategory;
import com.umc.product.community.domain.enums.CommunityThreadMemberRole;
import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;

@WebMvcTest(controllers = CommunityThreadLifecycleController.class)
@Import(JacksonConfig.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Community thread lifecycle REST adapter")
class CommunityThreadLifecycleControllerTest {

    private static final Long REQUESTER_ID = 99L;
    private static final Instant NOW = Instant.parse("2026-07-18T00:00:00Z");

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
    private GetCommunityThreadDetailUseCase getThreadDetailUseCase;

    @MockitoBean
    private GetCommunityThreadMutationDetailUseCase getMutationDetailUseCase;

    @BeforeEach
    void setUpCurrentMember() {
        MemberPrincipal principal = new MemberPrincipal(REQUESTER_ID);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @Test
    @DisplayName("thread 생성은 정규화 가능한 요청을 command로 전달하고 full detail을 반환한다")
    void createThread_mapsRequestAndReturnsFullDetail() throws Exception {
        given(createThreadUseCase.create(any())).willReturn(new CommunityThreadLifecycleInfo(
            42L, "서버 스터디", "함께 공부해요", CommunityThreadCategory.STUDY, "📚",
            REQUESTER_ID, 3L, 100, REQUESTER_ID, CommunityThreadMemberRole.OWNER,
            false, false, null
        ));
        given(getThreadDetailUseCase.getThread(any())).willReturn(detail(null));

        mockMvc.perform(post("/api/v1/community/threads")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "  서버 스터디  ",
                      "description": "  함께 공부해요  ",
                      "category": "STUDY",
                      "icon": "👨‍👩‍👧‍👦",
                      "memberIds": [7, 8]
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.result.threadId").value("42"))
            .andExpect(jsonPath("$.result.memberCount").value("3"))
            .andExpect(jsonPath("$.result.shareUrl").value("/api/v1/community/threads/42"));

        ArgumentCaptor<CreateCommunityThreadCommand> captor =
            ArgumentCaptor.forClass(CreateCommunityThreadCommand.class);
        then(createThreadUseCase).should().create(captor.capture());
        assertThat(captor.getValue().actorMemberId()).isEqualTo(REQUESTER_ID);
        assertThat(captor.getValue().title()).isEqualTo("서버 스터디");
        assertThat(captor.getValue().description()).isEqualTo("함께 공부해요");
        assertThat(captor.getValue().inviteeMemberIds()).containsExactly(7L, 8L);
        then(getThreadDetailUseCase).should().getThread(any());
    }

    @Test
    @DisplayName("PATCH에서 omitted와 blank description clear를 구분한다")
    void updateThread_distinguishesOmittedFromBlankDescription() throws Exception {
        given(getThreadDetailUseCase.getThread(any())).willReturn(detail(null));

        mockMvc.perform(patch("/api/v1/community/threads/42")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"description":"   "}
                    """))
            .andExpect(status().isOk());

        ArgumentCaptor<UpdateCommunityThreadCommand> captor =
            ArgumentCaptor.forClass(UpdateCommunityThreadCommand.class);
        then(updateThreadUseCase).should().update(captor.capture());
        assertThat(captor.getValue().title()).isNull();
        assertThat(captor.getValue().description()).isNull();
        assertThat(captor.getValue().descriptionProvided()).isTrue();
        assertThat(captor.getValue().category()).isNull();
        assertThat(captor.getValue().icon()).isNull();
    }

    @Test
    @DisplayName("PATCH에서 description omitted는 기존 값을 유지한다")
    void updateThread_keepsOmittedDescription() throws Exception {
        given(getThreadDetailUseCase.getThread(any())).willReturn(detail(null));

        mockMvc.perform(patch("/api/v1/community/threads/42")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"새 제목\"}"))
            .andExpect(status().isOk());

        ArgumentCaptor<UpdateCommunityThreadCommand> captor =
            ArgumentCaptor.forClass(UpdateCommunityThreadCommand.class);
        then(updateThreadUseCase).should().update(captor.capture());
        assertThat(captor.getValue().descriptionProvided()).isFalse();
    }

    @Test
    @DisplayName("soft delete 직후 mutation detail Port로 deletedAt이 있는 full detail을 반환한다")
    void deleteThread_returnsMutationDetail() throws Exception {
        given(getMutationDetailUseCase.getMutationDetail(any())).willReturn(detail(NOW));

        mockMvc.perform(delete("/api/v1/community/threads/42"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.threadId").value("42"))
            .andExpect(jsonPath("$.result.deletedAt").value(NOW.toString()));

        then(deleteThreadUseCase).should().delete(any());
        then(getMutationDetailUseCase).should().getMutationDetail(any());
        then(getThreadDetailUseCase).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @EnumSource(SettingRoute.class)
    @DisplayName("pin과 mute 설정 route는 대응하는 Community Port In을 호출한다")
    void settingsRoutes_callOnlyCommunityPorts(SettingRoute route) throws Exception {
        given(getThreadDetailUseCase.getThread(any())).willReturn(detail(null));

        mockMvc.perform(request(route.method, "/api/v1/community/threads/42" + route.path))
            .andExpect(status().isOk());

        switch (route) {
            case PIN -> then(managePinUseCase).should().pin(any());
            case UNPIN -> then(managePinUseCase).should().unpin(any());
            case MUTE -> then(manageMuteUseCase).should().mute(any());
            case UNMUTE -> then(manageMuteUseCase).should().unmute(any());
        }
    }

    private ThreadDetailInfo detail(Instant deletedAt) {
        return new ThreadDetailInfo(
            42L, "서버 스터디", "함께 공부해요", CommunityThreadCategory.STUDY, "📚",
            3L, 2L, 100, true, false, true, CommunityThreadMemberRole.OWNER,
            null, REQUESTER_ID, NOW, NOW, "/api/v1/community/threads/42", deletedAt
        );
    }

    private enum SettingRoute {
        PIN(HttpMethod.POST, "/pin"),
        UNPIN(HttpMethod.DELETE, "/pin"),
        MUTE(HttpMethod.POST, "/mute"),
        UNMUTE(HttpMethod.DELETE, "/mute");

        private final HttpMethod method;
        private final String path;

        SettingRoute(HttpMethod method, String path) {
            this.method = method;
            this.path = path;
        }
    }
}
