package com.umc.product.community.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.community.application.port.in.command.thread.ChangeCommunityThreadMemberRoleUseCase;
import com.umc.product.community.application.port.in.command.thread.InviteCommunityThreadMembersUseCase;
import com.umc.product.community.application.port.in.command.thread.KickCommunityThreadMemberUseCase;
import com.umc.product.community.application.port.in.command.thread.LeaveCommunityThreadUseCase;
import com.umc.product.community.application.port.in.command.thread.dto.ChangeCommunityThreadMemberRoleCommand;
import com.umc.product.community.application.port.in.command.thread.dto.CommunityThreadInvitationInfo;
import com.umc.product.community.application.port.in.command.thread.dto.CommunityThreadMemberLifecycleInfo;
import com.umc.product.community.application.port.in.command.thread.dto.InviteCommunityThreadMembersCommand;
import com.umc.product.community.application.port.in.query.thread.GetCommunityThreadMembersByIdsUseCase;
import com.umc.product.community.application.port.in.query.thread.dto.GetThreadMembersByIdsQuery;
import com.umc.product.community.application.port.in.query.thread.dto.ThreadMemberInfo;
import com.umc.product.community.domain.enums.CommunityThreadMemberRole;
import com.umc.product.community.domain.enums.CommunityThreadMemberState;
import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;

@WebMvcTest(controllers = CommunityThreadMembershipController.class)
@Import(JacksonConfig.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Community thread membership REST adapter")
class CommunityThreadMembershipControllerTest {

    private static final Long REQUESTER_ID = 99L;
    private static final Instant NOW = Instant.parse("2026-07-18T00:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private InviteCommunityThreadMembersUseCase inviteMembersUseCase;

    @MockitoBean
    private GetCommunityThreadMembersByIdsUseCase getMembersByIdsUseCase;

    @MockitoBean
    private KickCommunityThreadMemberUseCase kickMemberUseCase;

    @MockitoBean
    private LeaveCommunityThreadUseCase leaveThreadUseCase;

    @MockitoBean
    private ChangeCommunityThreadMemberRoleUseCase changeMemberRoleUseCase;

    @BeforeEach
    void setUpCurrentMember() {
        MemberPrincipal principal = new MemberPrincipal(REQUESTER_ID);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @Test
    @DisplayName("invite 결과 member IDs를 batch Community Port In으로 full ThreadMember에 조립한다")
    void inviteMembers_assemblesFullThreadMembersThroughBatchPort() throws Exception {
        given(inviteMembersUseCase.invite(any())).willReturn(new CommunityThreadInvitationInfo(
            42L,
            List.of(memberLifecycle(7L, CommunityThreadMemberRole.MEMBER, 4L)),
            4L
        ));
        given(getMembersByIdsUseCase.getMembersByIds(any())).willReturn(List.of(
            new ThreadMemberInfo(
                7L, "하늘", ChallengerPart.SPRINGBOOT, 8L,
                CommunityThreadMemberRole.MEMBER, NOW, CommunityThreadMemberState.ACTIVE
            )
        ));

        mockMvc.perform(post("/api/v1/community/threads/42/invite")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"memberIds":[7]}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.invitedMembers[0].memberId").value("7"))
            .andExpect(jsonPath("$.result.invitedMembers[0].name").value("하늘"))
            .andExpect(jsonPath("$.result.invitedMembers[0].generation").value("8"))
            .andExpect(jsonPath("$.result.memberCount").value("4"));

        ArgumentCaptor<InviteCommunityThreadMembersCommand> commandCaptor =
            ArgumentCaptor.forClass(InviteCommunityThreadMembersCommand.class);
        then(inviteMembersUseCase).should().invite(commandCaptor.capture());
        assertThat(commandCaptor.getValue().actorMemberId()).isEqualTo(REQUESTER_ID);
        assertThat(commandCaptor.getValue().memberIds()).containsExactly(7L);

        ArgumentCaptor<GetThreadMembersByIdsQuery> queryCaptor =
            ArgumentCaptor.forClass(GetThreadMembersByIdsQuery.class);
        then(getMembersByIdsUseCase).should().getMembersByIds(queryCaptor.capture());
        assertThat(queryCaptor.getValue().memberIds()).containsExactly(7L);
    }

    @Test
    @DisplayName("kick은 target member ID와 CurrentMember를 command에 전달한다")
    void kickMember_mapsTargetAndCurrentMember() throws Exception {
        given(kickMemberUseCase.kick(any())).willReturn(
            memberLifecycle(7L, CommunityThreadMemberRole.MEMBER, 3L)
        );

        mockMvc.perform(delete("/api/v1/community/threads/42/members/7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.threadId").value("42"))
            .andExpect(jsonPath("$.result.memberId").value("7"))
            .andExpect(jsonPath("$.result.memberCount").value("3"));

        then(kickMemberUseCase).should().kick(any());
    }

    @Test
    @DisplayName("leave는 CurrentMember 자신을 member mutation 응답으로 반환한다")
    void leaveThread_mapsCurrentMember() throws Exception {
        given(leaveThreadUseCase.leave(any())).willReturn(
            new CommunityThreadMemberLifecycleInfo(
                42L, REQUESTER_ID, CommunityThreadMemberRole.MEMBER,
                CommunityThreadMemberState.LEFT, NOW, NOW, 3L
            )
        );

        mockMvc.perform(post("/api/v1/community/threads/42/leave"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.memberId").value("99"))
            .andExpect(jsonPath("$.result.state").value("LEFT"));

        then(leaveThreadUseCase).should().leave(any());
    }

    @Test
    @DisplayName("role 변경은 OWNER를 포함한 typed role을 command에 전달한다")
    void changeRole_mapsTypedRole() throws Exception {
        given(changeMemberRoleUseCase.changeRole(any())).willReturn(
            memberLifecycle(7L, CommunityThreadMemberRole.OWNER, 4L)
        );

        mockMvc.perform(patch("/api/v1/community/threads/42/members/7/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"role":"OWNER"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.role").value("OWNER"));

        ArgumentCaptor<ChangeCommunityThreadMemberRoleCommand> captor =
            ArgumentCaptor.forClass(ChangeCommunityThreadMemberRoleCommand.class);
        then(changeMemberRoleUseCase).should().changeRole(captor.capture());
        assertThat(captor.getValue().role()).isEqualTo(CommunityThreadMemberRole.OWNER);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"memberIds\":[]}", "{\"memberIds\":[7,7]}", "{\"memberIds\":[0]}"})
    @DisplayName("invite는 non-empty unique positive member IDs만 허용한다")
    void inviteMembers_rejectsInvalidMemberIds(String body) throws Exception {
        mockMvc.perform(post("/api/v1/community/threads/42/invite")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());

        then(inviteMembersUseCase).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"role\":null}", "{\"role\":\"UNKNOWN\"}"})
    @DisplayName("role 누락, null, 미지원 값은 Port In 전에 거절한다")
    void changeRole_rejectsInvalidRole(String body) throws Exception {
        mockMvc.perform(patch("/api/v1/community/threads/42/members/7/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());

        then(changeMemberRoleUseCase).shouldHaveNoInteractions();
    }

    private CommunityThreadMemberLifecycleInfo memberLifecycle(
        Long memberId,
        CommunityThreadMemberRole role,
        long memberCount
    ) {
        return new CommunityThreadMemberLifecycleInfo(
            42L, memberId, role, CommunityThreadMemberState.ACTIVE, NOW, null, memberCount
        );
    }
}
